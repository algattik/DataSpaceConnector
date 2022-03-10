/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.github.javafaker.Faker;
import org.apache.commons.io.input.BoundedInputStream;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.junit.launcher.TerraformOutputsExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.SHARED_KEY;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({TerraformOutputsExtension.class, EdcExtension.class})
class AzureDataFactoryCopyIntegrationTest {

    static List<Runnable> containerCleanup = new ArrayList<>();

    String blobName = createBlobName();
    long blobSize = 100L * 1024;

    Account account1;
    Account account2;

    @AfterAll
    static void tearDownClass() {
        containerCleanup.parallelStream().forEach(Runnable::run);
    }

    @Test
    void transfer_success(AzureResourceManager azure,
                          DataPlaneManager registry) throws Exception {
        account1 = new Account(azure, "edc_test_provider_storage_resourceid");
        account2 = new Account(azure, "edc_test_consumer_storage_resourceid");

        try (var os =
                     account1.client
                             .getBlobContainerClient(account1.containerName)
                             .getBlobClient(blobName)
                             .getBlockBlobClient()
                             .getBlobOutputStream()) {
            new BoundedInputStream(
                    new FileInputStream("/dev/urandom"), blobSize).transferTo(os);
        }

        var source = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, account1.name)
                .property(CONTAINER_NAME, account1.containerName)
                .property(BLOB_NAME, blobName)
                .property(SHARED_KEY, account1.key)
                .build();
        var destination = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, account2.name)
                .property(CONTAINER_NAME, account2.containerName)
                .property(SHARED_KEY, account2.key)
                .build();
        var request = DataFlowRequest.Builder.newInstance()
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .build();

        registry.initiateTransfer(request);

        var destinationBlob = account2.client
                .getBlobContainerClient(account2.containerName)
                .getBlobClient(blobName);
        await()
                .atMost(Duration.ofMinutes(5))
                .untilAsserted(() -> assertThat(destinationBlob.exists())
                        .withFailMessage("should have copied blob between containers")
                        .isTrue());
        assertThat(destinationBlob.getProperties().getBlobSize())
                .isEqualTo(blobSize);
    }

    private static class Account {

        final static Faker FAKER = new Faker();
        final String name;
        final String key;
        final BlobServiceClient client;
        final String containerName = FAKER.lorem().characters(35, 40, false, false);

        Account(AzureResourceManager azure, String setting) {
            String accountId = Objects.requireNonNull(System.getProperty(setting), setting);
            var account = azure.storageAccounts().getById(accountId);
            name = account.name();
            key = account.getKeys().stream().findFirst().orElseThrow().value();
            client = new BlobServiceClientBuilder()
                    .credential(new StorageSharedKeyCredential(account.name(), key))
                    .endpoint(account.endPoints().primary().blob())
                    .buildClient();
            createContainer();
        }

        private void createContainer() {
            assertFalse(client.getBlobContainerClient(containerName).exists());

            BlobContainerClient blobContainerClient = client.createBlobContainer(containerName);
            assertTrue(blobContainerClient.exists());
            containerCleanup.add(() -> client.deleteBlobContainer(containerName));
        }
    }
}

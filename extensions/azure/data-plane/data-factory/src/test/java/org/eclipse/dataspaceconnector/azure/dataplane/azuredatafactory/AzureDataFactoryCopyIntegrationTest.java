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

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.input.BoundedInputStream;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.SettingResolver;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.SHARED_KEY;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class AzureDataFactoryCopyIntegrationTest {

    private static AzureResourceManager resourceManager;
    protected String account1Name;
    protected String account1Key;
    protected String account2Name;
    protected String account2Key;
    protected BlobServiceClient blobServiceClient1;
    protected BlobServiceClient blobServiceClient2;
    protected String account1ContainerName = createContainerName().replaceFirst("...", "src");
    protected List<Runnable> containerCleanup = new ArrayList<>();

    String account2ContainerName = createContainerName().replaceFirst("...", "dst");
    String blobName = createBlobName();
    long blobSize = 100L * 1024 * 1024;

    private TransferService transferService;

    @BeforeAll
    static void setUpClass() throws Exception {
        var mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> v = mapper.readValue(new File("../../../../cloud_settings.json"), Map.class);
        for (var entry : v.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue().get("value").toString());
        }

        var credential = new AzureCliCredentialBuilder().build();
        var profile = new AzureProfile(AzureEnvironment.AZURE);
        resourceManager = AzureResourceManager.authenticate(credential, profile)
                .withSubscription(Objects.requireNonNull(System.getProperty("EDC_DATAFACTORY_SUBSCRIPTIONID")));
    }

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        var account1 = resourceManager.storageAccounts()
                .getById(requiredSetting(context, "edc.provider.storage.resourceid"));
        account1Name = account1.name();
        account1Key = getStorageAccountKey(account1).value();
        blobServiceClient1 = getBlobServiceClient(account1.name(), account1Key);

        var account2 = resourceManager.storageAccounts()
                .getById(requiredSetting(context, "edc.consumer.storage.resourceid"));
        account2Name = account2.name();
        account2Key = getStorageAccountKey(account2).value();
        blobServiceClient2 = getBlobServiceClient(account2.name(), account2Key);

        createContainer(blobServiceClient1, account1ContainerName);
        createContainer(blobServiceClient2, account2ContainerName);

        TransferServiceRegistry registry = mock(TransferServiceRegistry.class);
        context.registerService(TransferServiceRegistry.class, registry);

        var extension = factory.constructInstance(DataPlaneAzureDataFactoryExtension.class);
        extension.initialize(context);

        ArgumentCaptor<TransferService> captor = ArgumentCaptor.forClass(TransferService.class);
        verify(registry).registerTransferService(captor.capture());
        transferService = captor.getValue();
    }

    public static String requiredSetting(SettingResolver context, String s) {
        return Objects.requireNonNull(context.getSetting(s, null), s);
    }

    protected void createContainer(BlobServiceClient client, String containerName) {
        assertFalse(client.getBlobContainerClient(containerName).exists());

        BlobContainerClient blobContainerClient = client.createBlobContainer(containerName);
        assertTrue(blobContainerClient.exists());
        containerCleanup.add(() -> client.deleteBlobContainer(containerName));
    }


    @NotNull
    protected BlobServiceClient getBlobServiceClient(String accountName, String key) {
        var endpoint = "https://" + accountName + ".blob.core.windows.net";
        var client = new BlobServiceClientBuilder()
                .credential(new StorageSharedKeyCredential(accountName, key))
                .endpoint(endpoint)
                .buildClient();

        client.getAccountInfo();
        return client;
    }

    @NotNull
    private StorageAccountKey getStorageAccountKey(StorageAccount account) {
        return account
                .getKeys()
                .stream().findFirst().get();
    }

    @Test
    void transfer_success() throws Exception {
        try (var os =
                     blobServiceClient1.getBlobContainerClient(account1ContainerName)
                             .getBlobClient(blobName)
                             .getBlockBlobClient()
                             .getBlobOutputStream()) {
            new BoundedInputStream(
                    new FileInputStream("/dev/urandom"), blobSize).transferTo(os);
        }

        var source = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, account1Name)
                .property(CONTAINER_NAME, account1ContainerName)
                .property(BLOB_NAME, blobName)
                .property(SHARED_KEY, account1Key)
                .build();
        var destination = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, account2Name)
                .property(CONTAINER_NAME, account2ContainerName)
                .property(SHARED_KEY, account2Key)
                .build();
        var request = DataFlowRequest.Builder.newInstance()
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .build();

        assertThat(transferService.transfer(request))
                .succeedsWithin(5, TimeUnit.MINUTES)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        var destinationBlob = blobServiceClient2
                .getBlobContainerClient(account2ContainerName)
                .getBlobClient(blobName);
        assertThat(destinationBlob.exists())
                .withFailMessage("should have copied blob between containers")
                .isTrue();
        assertThat(destinationBlob.getProperties().getBlobSize())
                .isEqualTo(blobSize);
    }
}

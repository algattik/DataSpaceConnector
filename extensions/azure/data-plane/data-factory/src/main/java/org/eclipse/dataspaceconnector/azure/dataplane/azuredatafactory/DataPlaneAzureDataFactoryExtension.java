/*
 *  Copyright (c) 2022 Microsoft Corporation
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
import com.azure.identity.AzureCliCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.datafactory.DataFactoryManager;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory.pipeline.AzureDataFactoryTransferServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.SettingResolver;

import java.util.Objects;

public class DataPlaneAzureDataFactoryExtension implements ServiceExtension {

    @Inject
    private TransferServiceRegistry registry;

    @Override
    public String name() {
        return "Data Plane Azure Data Factory";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        String keyVaultLinkedService = context.getSetting("edc.datafactory.keyvault.linkedservicename", "AzureKeyVault");
        String dataFactoryId = requiredSetting(context, "edc.datafactory.resourceid");
        String keyVaultId = requiredSetting(context, "edc.datafactory.keyvault.resourceid");
        String tenantId = requiredSetting(context, "edc.datafactory.tenantid");
        String subscriptionId = requiredSetting(context, "edc.datafactory.subscriptionid");

        AzureCliCredential credential = new AzureCliCredentialBuilder().build();
        AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);
        DataFactoryManager dataFactoryManager = DataFactoryManager.authenticate(credential, profile);
        AzureResourceManager resourceManager = AzureResourceManager.authenticate(credential, profile)
                .withSubscription(subscriptionId);
        GenericResource factory = resourceManager.genericResources().getById(dataFactoryId);
        Vault vault = resourceManager.vaults().getById(keyVaultId);

        SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(vault.vaultUri())
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        var transferService = new AzureDataFactoryTransferServiceImpl(monitor, dataFactoryManager, factory, secretClient, keyVaultLinkedService);
        registry.registerTransferService(transferService);
    }

    private static String requiredSetting(SettingResolver context, String s) {
        return Objects.requireNonNull(context.getSetting(s, null), s);
    }

}

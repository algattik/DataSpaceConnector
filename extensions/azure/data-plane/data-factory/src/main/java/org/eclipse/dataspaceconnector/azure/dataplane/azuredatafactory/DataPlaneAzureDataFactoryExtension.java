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

import org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory.pipeline.AzureDataFactoryTransferServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

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

        var transferService = new AzureDataFactoryTransferServiceImpl(monitor);
        registry.registerTransferService(transferService);
    }
}

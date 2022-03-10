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

package org.eclipse.dataspaceconnector.junit.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.Map;
import java.util.Properties;

public class TerraformOutputsExtension implements BeforeAllCallback, AfterAllCallback {
    private Properties savedProperties;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        savedProperties = (Properties) System.getProperties().clone();

        var mapper = new ObjectMapper();
        var root = GradleUtils.findRoot();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> v = mapper.readValue(new File(root, "terraform_outputs.json"), Map.class);
        for (var entry : v.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue().get("value").toString());
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        System.setProperties(savedProperties);

    }
}

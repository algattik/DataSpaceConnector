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

import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.File;
import java.io.IOException;

public class GradleUtils {
    private static final String GRADLE_WRAPPER_UNIX = "gradlew";
    private static final String GRADLE_WRAPPER_WINDOWS = "gradlew.bat";
    static final String GRADLE_WRAPPER;

    static {
        GRADLE_WRAPPER = (System.getProperty("os.name").toLowerCase().contains("win")) ? GRADLE_WRAPPER_WINDOWS : GRADLE_WRAPPER_UNIX;
    }

    private GradleUtils() {
    }

    static File findRoot() throws IOException {
        var root = findRoot(new File(".").getCanonicalFile());
        if (root == null) {
            throw new EdcException("Could not find " + GRADLE_WRAPPER + " in parent directories.");
        }
        return root;
    }

    /**
     * Utility method to locate the Gradle project root.
     *
     * @param path directory in which to start ascending search for the Gradle root.
     * @return The Gradle project root directly, or <code>null</code> if not found.
     */
    private static File findRoot(File path) {
        File gradlew = new File(path, GRADLE_WRAPPER);
        if (gradlew.exists()) {
            return path;
        }
        var parent = path.getParentFile();
        if (parent != null) {
            return findRoot(parent);
        }
        return null;
    }

}

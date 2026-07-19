/**
 * ******************************************************************************
 * Copyright (c) {2026} The original author or authors
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available
 * at http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 ********************************************************************************/

package de.iip_ecosphere.platform.tools.maven.dependencies;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests path composition in {@link BuildPluginClasspathMojo}.
 *
 * @author Holger Eichelberger, SSE
 */
public class BuildPluginClasspathMojoTest {

    /**
     * Ensures that a development plugin classpath refers to its own target JAR relative to its module directory.
     */
    @Test
    public void testRelativeTargetDirectory() {
        File module = new File("target/test-data/example-plugin");
        File target = new File(module, "target");

        String prefix = BuildPluginClasspathMojo.getRelTargetDirectory(module, target);
        Assert.assertEquals("target", prefix);
        Assert.assertEquals("target/example-plugin.jar", new File(prefix, "example-plugin.jar").getPath());
    }

}

/**
 * ******************************************************************************
 * Copyright (c) {2022} The original author or authors
 *
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License 2.0 which is available 
 * at http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 ********************************************************************************/

package test.de.iip_ecosphere.platform.configuration.easyProducer.opcua;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.iip_ecosphere.platform.configuration.easyProducer.opcua.parser.DomParser;
import de.iip_ecosphere.platform.support.FileUtils;

/**
 * Tests {@link DomParser}.
 * 
 * @author Holger Eichelberger, SSE
 */
public class DomParserTest {

    /**
     * Tests that no arguments retain the bundled default input set.
     */
    @Test
    public void testDefaultInputs() {
        File[] inputs = DomParser.selectInputFiles(new String[0]);

        Assert.assertEquals(61, inputs.length);
        Assert.assertEquals(new File("src/main/resources/NodeSets/Opc.Ua.Woodworking.NodeSet2.xml"), inputs[0]);
        Assert.assertEquals(new File("src/main/resources/NodeSets/Opc.Ua.Machinery.NodeSet2.xml"),
            inputs[inputs.length - 1]);
    }

    /**
     * Tests that one explicit input is processed.
     *
     * @throws IOException shall not occur
     */
    @Test
    public void testExplicitInput() throws IOException {
        File in = new File("src/test/resources/NodeSets/Opc.Ua.MachineTool.NodeSet2.xml");
        File out = new File("target/gen/OpcMachineTool.ivml");
        out.getParentFile().mkdirs();
        Files.deleteIfExists(out.toPath());
        DomParser.setDefaultVerbose(false);
        DomParser.setUsingIvmlFolder("target/tmp");

        DomParser.main(new String[] {in.toString()});

        Assert.assertTrue(out.isFile());
    }

    /**
     * Tests that explicit inputs are selected exactly once in caller order.
     */
    @Test
    public void testExplicitInputOrder() {
        File first = new File("src/test/resources/NodeSets/Opc.Ua.Woodworking.NodeSet2.xml");
        File second = new File("src/test/resources/NodeSets/Opc.Ua.MachineTool.NodeSet2.xml");

        File[] inputs = DomParser.selectInputFiles(new String[] {first.toString(), second.toString()});

        Assert.assertArrayEquals(new File[] {first, second}, inputs);
    }
    
    /**
     * Tests {@link DomParser} on the machine tool companion spec XML.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testDomParserMachineTool() throws IOException {
        File in = new File("src/test/resources/NodeSets/Opc.Ua.MachineTool.NodeSet2.xml");
        Assert.assertTrue(in.exists());
        File tmp = new File("target/tmp");
        tmp.mkdirs();
        File out = new File(tmp, "OpcMachineTool.ivml");
        // implicit from in to out
        DomParser.setDefaultVerbose(false); // reduce output
        DomParser.setUsingIvmlFolder("target/tmp");
        DomParser.process(in, "MachineTool", out, false);
        
        Charset charset = Charset.forName("UTF-8");
        File expected = new File("src/test/resources/OpcMachineTool.ivml");
        String exContents = normalize(FileUtils.readFileToString(expected, charset));
        String outContents = normalize(FileUtils.readFileToString(out, charset));
        Assert.assertEquals(exContents, outContents);
    }

    /**
     * Tests {@link DomParser} on the woodworking companion spec XML.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testDomParserWoodworking() throws IOException {
        File in = new File("src/test/resources/NodeSets/Opc.Ua.Woodworking.NodeSet2.xml");
        Assert.assertTrue(in.exists());
        File tmp = new File("target/tmp");
        tmp.mkdirs();
        File out = new File(tmp, "OpcWoodworking.ivml");
        // implicit from in to out
        DomParser.setDefaultVerbose(false); // reduce output
        new File("target/ivml").mkdirs();
        DomParser.setUsingIvmlFolder("target/tmp");
        DomParser.process(in, "Woodworking", out, false);
        
        Charset charset = Charset.forName("UTF-8");
        File expected = new File("src/test/resources/OpcWoodworking.ivml");
        String exContents = normalize(FileUtils.readFileToString(expected, charset));
        String outContents = normalize(FileUtils.readFileToString(out, charset));
        Assert.assertEquals(exContents, outContents);
    }

    /**
     * Tests explicit input validation.
     *
     * @throws IOException shall not occur
     */
    @Test
    public void testInvalidInputs() throws IOException {
        File tmp = new File("target/tmp");
        tmp.mkdirs();
        File missing = new File(tmp, "missing-opcua-input.xml");
        Files.deleteIfExists(missing.toPath());
        assertInvalidInput("OPC UA NodeSet input does not exist: " + missing, missing.toString());
        assertInvalidInput("OPC UA NodeSet input is not a regular file: " + tmp, tmp.toString());

        File input = new File("src/test/resources/NodeSets/Opc.Ua.MachineTool.NodeSet2.xml");
        String duplicate = new File(input.getParentFile(), "." + File.separator + input.getName()).toString();
        assertInvalidInput("Duplicate OPC UA NodeSet input: " + duplicate, input.toString(), duplicate);
    }

    /**
     * Tests rejection of an unreadable explicit input where file permissions support it.
     *
     * @throws IOException shall not occur
     */
    @Test
    public void testUnreadableInput() throws IOException {
        File tmp = new File("target/tmp");
        tmp.mkdirs();
        File unreadable = File.createTempFile("unreadable-opcua-input-", ".xml", tmp);
        boolean permissionsChanged = unreadable.setReadable(false, false);
        try {
            Assume.assumeTrue(permissionsChanged && !Files.isReadable(unreadable.toPath()));
            assertInvalidInput("OPC UA NodeSet input is not readable: " + unreadable, unreadable.toString());
        } finally {
            unreadable.setReadable(true, false);
            Files.deleteIfExists(unreadable.toPath());
        }
    }

    /**
     * Tests that all explicit inputs are validated before processing begins.
     *
     * @throws IOException shall not occur
     */
    @Test
    public void testInputPreflight() throws IOException {
        File out = new File("target/gen/OpcMachineTool.ivml");
        File missing = new File("target/tmp/missing-opcua-input.xml");
        out.getParentFile().mkdirs();
        missing.getParentFile().mkdirs();
        Files.deleteIfExists(out.toPath());
        Files.deleteIfExists(missing.toPath());

        File input = new File("src/test/resources/NodeSets/Opc.Ua.MachineTool.NodeSet2.xml");
        assertInvalidInput("OPC UA NodeSet input does not exist: " + missing,
            input.toString(), missing.toString());
        Assert.assertFalse(out.exists());
    }

    /**
     * Tests that checked XML parser errors propagate to the caller.
     *
     * @throws IOException shall not occur
     */
    @Test
    public void testParserErrorPropagation() throws IOException {
        File tmp = new File("target/tmp");
        tmp.mkdirs();
        File invalid = File.createTempFile("invalid-opcua-input-", ".xml", tmp);
        Files.write(invalid.toPath(), "<invalid>".getBytes(Charset.forName("UTF-8")));
        try {
            DomParser.main(new String[] {invalid.toString()});
            Assert.fail("Expected malformed XML input to be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains(invalid.toString()));
            Assert.assertTrue(e.getCause() instanceof SAXException);
        } finally {
            Files.deleteIfExists(invalid.toPath());
        }
    }

    /**
     * Asserts that input selection fails with the expected diagnostic.
     *
     * @param expectedMessage the expected diagnostic
     * @param args the parser arguments
     */
    private static void assertInvalidInput(String expectedMessage, String... args) {
        try {
            DomParser.main(args);
            Assert.fail("Expected invalid input to be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Helper function to indicate char differences to apply when string comparison fails.
     * 
     * @param exContents the expected contents
     * @param outContents the actual contents
     */
    static void printCharDiff(String exContents, String outContents) {
        for (int i = 0; i < Math.min(exContents.length(), outContents.length()); i++) {
            if (exContents.charAt(i) != outContents.charAt(i)) {
                System.out.println(((int) exContents.charAt(i)) + " " + ((int) outContents.charAt(i)));
            }
        }
    }

    /**
     * Normalizes unicode/UTF-8 strings for comparison (heuristics). This is just a hack. Any normalization solution 
     * solving that problem is welcome.
     * 
     * @param text the text to be normalized
     * @return the normalized text
     */
    private static String normalize(String text) {
        StringBuilder tmp = new StringBuilder(text);
        for (int i = 0; i < tmp.length(); i++) {
            int c = (int) tmp.charAt(i);
            if (c == 172) {
                tmp.setCharAt(i, (char) 45);
            } else if (c == 8211 || c == 65533) {
                tmp.setCharAt(i, '-');
            } else if (c == 8804) {
                tmp.setCharAt(i, (char) 63);
            } else if (c == 8217 || c == 8222 || c == 8220 || c == 8230) {
                tmp.setCharAt(i, (char) 45);
            }
        }
        return tmp.toString();
    }

}

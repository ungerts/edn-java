/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.printer;

import org.junit.Before;
import org.junit.Test;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parsers;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * Tests that EDN produced by the printer can be correctly parsed by Clojure.
 * This ensures cross-compatibility between the edn-java library and Clojure.
 */
public class PrinterClojureInteroperabilityTest {

    private IFn parseEdn;
    private IFn ednToStr;

    @Before
    public void setUp() {
        // Load necessary Clojure namespaces and functions
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("clojure.edn"));

        parseEdn = Clojure.var("clojure.edn", "read-string");
        ednToStr = Clojure.var("clojure.core", "pr-str");
    }

    private void assertClojureCompatible(String ednText) {
        try {
            Parser parser = Parsers.newParser(Parsers.defaultConfiguration());
            Parseable pbr = Parsers.newParseable(ednText);
            Object javaValue = parser.nextValue(pbr);

            StringWriter sw = new StringWriter();
            Printer printer = Printers.newPrinter(Printers.defaultPrinterProtocol(), sw);
            printer.printValue(javaValue);
            printer.close();

            String printedEdn = sw.toString();

            Object clojureValue = parseEdn.invoke(printedEdn);
            String clojureSerializedValue = (String) ednToStr.invoke(clojureValue);

            Parseable javaReparsed = Parsers.newParseable(printedEdn);
            Object javaReparsedValue = parser.nextValue(javaReparsed);

            Parseable clojureReparsed = Parsers.newParseable(clojureSerializedValue);
            Object clojureReparsedValue = parser.nextValue(clojureReparsed);

            assertEquals("EDN values differ when round-tripped through Clojure",
                javaReparsedValue, clojureReparsedValue);

        } catch (Exception e) {
            fail("Clojure evaluation failed: " + e.getMessage());
        }
    }

    @Test
    public void testSimpleValues() {
        assertClojureCompatible("nil");
        assertClojureCompatible("true");
        assertClojureCompatible("false");
        assertClojureCompatible("42");
        assertClojureCompatible("3.14159");
        assertClojureCompatible("42N");
        assertClojureCompatible("3.14159M");
        assertClojureCompatible(":keyword");
        assertClojureCompatible(":namespaced/keyword");
    }

    @Test
    public void testStrings() {
        assertClojureCompatible("\"Hello, world!\"");
        assertClojureCompatible("\"String with \\\"quotes\\\"\"");
        assertClojureCompatible("\"String with \\n newlines and \\t tabs\"");
    }

    @Test
    public void testCharacters() {
        assertClojureCompatible("\\a");
        assertClojureCompatible("\\space");
        assertClojureCompatible("\\newline");
    }

    @Test
    public void testSymbols() {
        assertClojureCompatible("symbol");
        assertClojureCompatible("namespaced/symbol");
        assertClojureCompatible("/");
    }

    @Test
    public void testCollections() {
        assertClojureCompatible("[]");
        assertClojureCompatible("[1 2 3]");
        assertClojureCompatible("()");
        assertClojureCompatible("(1 2 3)");
        assertClojureCompatible("{}");
        assertClojureCompatible("{:a 1 :b 2}");
        assertClojureCompatible("#{}");
        assertClojureCompatible("#{1 2 3}");
    }

    @Test
    public void testNestedCollections() {
        assertClojureCompatible("[1 [2 [3]]]");
        assertClojureCompatible("[[[][()]{}()]]");
        assertClojureCompatible("{:a {:b {:c 3}}}");
        assertClojureCompatible("{:a [1 2 #{3 4}]}");
    }

    @Test
    public void testTaggedValues() {
        assertClojureCompatible("#inst \"2023-01-01T00:00:00.000-00:00\"");
        assertClojureCompatible("#uuid \"f81d4fae-7dec-11d0-a765-00a0c91e6bf6\"");
    }

    @Test
    public void testComplexValues() {
        assertClojureCompatible("{:foo [1 2.0 19023847928034709821374012938749N 91821234112347634.128937467E-3M]" +
            " :bar/baz #{true false nil}" +
            " / (\"abc\\tdef\\n\" #uuid \"f81d4fae-7dec-11d0-a765-00a0c91e6bf6\")" +
            " \\formfeed [#inst \"2010\", #inst \"2010-11\", #inst \"2010-11-12T09:08:07.123+02:00\"]" +
            " :omega [a b c d \\a\\b\\c #{}]}");
    }
}

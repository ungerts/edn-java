/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.printer;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.*;

import org.junit.Test;

import org.jabref.edn.Keyword;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;


public class PrinterTest {

    @Test
    public void testSingleValues() {
        assertRoundTrip("nil");
        assertRoundTrip("a");
        assertRoundTrip("a/b");
        assertRoundTrip("/");
        assertRoundTrip("true");
        assertRoundTrip("false");
        assertRoundTrip(":a");
        assertRoundTrip(":a/b");
        assertRoundTrip("1");
        assertRoundTrip("1N");
        assertRoundTrip("3.14159");
        assertRoundTrip("123456789101112131415.1617181920M");
        assertRoundTrip("\\space");
        assertRoundTrip("\\a");
        assertRoundTrip("\"ab\\ncd\"");
        assertRoundTrip("[]");
        assertRoundTrip("()");
        assertRoundTrip("{}");
        assertRoundTrip("#{}");
        assertRoundTrip("{#{},()}");
        assertRoundTrip("#uuid \"f81d4fae-7dec-11d0-a765-00a0c91e6bf6\"");
        assertRoundTrip("\"\\\\\\\"\\'\\b\\t\\n\\r\\f\"");
    }

    @Test
    public void testRoundTripCommaCharacterLiteralIssue45() {
        assertRoundTrip("\\,");
    }

    @Test
    public void testSymbolAsMapKeyWithSetAsValue() {
        assertRoundTrip("{foo #{}}");
    }

    @Test
    public void testTaggedSymbol() {
        assertRoundTrip("[#foo bar# baz]");
    }

    @Test
    public void testComplexValue() {
        assertRoundTrip("""
            {:foo [1 2.0 19023847928034709821374012938749N 91821234112347634.128937467E-3M]
             :bar/baz #{true false nil}
             / ("abc\\tdef\\n" #uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
             \\formfeed [#inst "2010", #inst "2010-11", #inst "2010-11-12T09:08:07.123+02:00"]
             :omega [a b c d \\a\\b\\c #{}]}""");
    }

    @Test
    public void testDefaultPrinter() {
        StringWriter sw = new StringWriter();
        Printer p = Printers.newPrinter(sw);

        ArrayList<Object> al = new ArrayList<>();
        al.add(1);
        al.add(2);
        p.printValue(al);
        assertEquals("[1 2]", sw.toString());
    }

    void assertRoundTrip(String ednText) {
        Parser parser;
        Parseable pbr;
        pbr = Parsers.newParseable(ednText);
        parser = Parsers.newParser(Parsers.defaultConfiguration());
        Object originalParsedValue = parser.nextValue(pbr);

        StringWriter sw = new StringWriter();
        Printer ew = Printers.newPrinter(Printers.defaultPrinterProtocol(), sw);
        ew.printValue(originalParsedValue);
        ew.close();

        pbr = Parsers.newParseable(sw.toString());
        parser = Parsers.newParser(Parsers.defaultConfiguration());
        Object secondGenerationParsedValue = parser.nextValue(pbr);
        assertEquals("'" + ednText + "' => '" + sw
                + "' did not round-trip.", originalParsedValue,
                secondGenerationParsedValue);
        assertEquals(Parser.END_OF_INPUT, parser.nextValue(pbr));
    }

    @Test
    public void issue31() {
        assertEquals("\"'\"", Printers.printString("'"));
        assertEquals("\"'\"", Printers.printString("'"));
    }

    @Test
    public void testLoosePrinter() {
        StringWriter sw = new StringWriter();
        Printer p = LoosePrinter.newLoosePrinter(sw);
        ArrayList<Object> al = new ArrayList<>();
        al.add(Keyword.newKeyword("test"));
        al.add("test");
        al.add(Keyword.newKeyword("value"));
        Map<Keyword, String> map = Map.of(Keyword.newKeyword("name"), "Test");
        al.add(map);
        p.printValue(al);
        assertEquals("[:test \"test\" :value {:name \"Test\"}]", sw.toString());
    }


}

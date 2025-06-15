/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.net.URI;

import org.junit.Test;

import org.jabref.edn.Tag;
import org.jabref.edn.printer.Printer;
import org.jabref.edn.printer.Printer.Fn;
import org.jabref.edn.printer.Printers;
import org.jabref.edn.protocols.Protocol;

public class CustomTagPrinterTest {
    private static final Tag BPSM_URI = Tag.newTag("org.jabref", "uri");
    @Test
    public void test() {
        Protocol<Fn<?>> fns = Printers.defaultProtocolBuilder()
                .put(URI.class, (Fn<URI>) (self, writer) -> writer.printValue(BPSM_URI).printValue(self.toString()))
                    .build();
        StringWriter w = new StringWriter();
        Printer p = Printers.newPrinter(fns, w);
        p.printValue(URI.create("http://example.com"));
        p.close();
        assertEquals("#org.jabref/uri\"http://example.com\"", w.toString());
    }
}

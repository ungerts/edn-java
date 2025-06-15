/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.examples;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;
import org.jabref.edn.printer.Printers;

import java.util.Arrays;
import java.util.List;

public class PrintingExamples {
    @Test
    public void printCompactly() {
        Assert.assertThat(ACCEPTABLE_COMPACT_RENDERINGS,
                CoreMatchers.hasItem(Printers.printString(
                        Printers.defaultPrinterProtocol(),
                        VALUE_TO_PRINT)));
    }

    static final Object VALUE_TO_PRINT;
    static {
        Parser parser = Parsers.newParser(Parsers.defaultConfiguration());
        VALUE_TO_PRINT = parser.nextValue(Parsers.newParseable("""
            {:a [1 2 3],
             [x/y] 3.14159}
            """));
    }

    static final List<String> ACCEPTABLE_COMPACT_RENDERINGS = Arrays.asList(
            "{:a[1 2 3][x/y]3.14159}",
            "{[x/y]3.14159 :a[1 2 3]}"
    );

    static final List<String> ACCEPTABLE_PRETTY_RENDERINGS = Arrays.asList("""
        {
          :a [
            1
            2
            3
          ]
          [
            x/y
          ] 3.14159
        }""", """
            {
              [
                x/y
              ] 3.14159
              :a [
                1
                2
                3
              ]
            }"""
    );
}

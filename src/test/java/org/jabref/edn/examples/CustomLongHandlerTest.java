/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.junit.Test;

import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;

public class CustomLongHandlerTest {
    @Test
    public void test() {
        Parser.Config cfg =
            Parsers.newParserConfigBuilder()
                .putTagHandler(Parser.Config.LONG_TAG, (tag, value) -> {
                    long n = (Long) value;
                    if (Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE) {
                        return (int) n;
                    } else {
                        return BigInteger.valueOf(n);
                    }
                }).build();
        Parser p = Parsers.newParser(cfg);
        Parseable pbr = Parsers.newParseable("1024, 2147483648");
        assertEquals(1024, p.nextValue(pbr));
        assertEquals(BigInteger.valueOf(2147483648L), p.nextValue(pbr));
    }
}

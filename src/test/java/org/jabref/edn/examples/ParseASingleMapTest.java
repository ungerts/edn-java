/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;
import static org.jabref.edn.Keyword.newKeyword;
import static org.jabref.edn.parser.Parsers.defaultConfiguration;

import java.util.Map;

import org.junit.Test;

import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;


public class ParseASingleMapTest {
    @Test
    public void simpleUsageExample() {
        Parseable pbr = Parsers.newParseable("{:x 1, :y 2}");
        Parser p = Parsers.newParser(defaultConfiguration());
        Map<?, ?> m = (Map<?, ?>) p.nextValue(pbr);
        assertEquals(1L, m.get(newKeyword("x")));
        assertEquals(2L, m.get(newKeyword("y")));
        assertEquals(Parser.END_OF_INPUT, p.nextValue(pbr));
    }
}

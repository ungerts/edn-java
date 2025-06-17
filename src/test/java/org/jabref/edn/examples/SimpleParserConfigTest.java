/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;
import static org.jabref.edn.parser.Parsers.newParseable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import org.jabref.edn.EdnSyntaxException;
import org.jabref.edn.parser.CollectionBuilder;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;


public class SimpleParserConfigTest {
    @Test
    public void test() {
        Parser.Config cfg =
            Parsers.newParserConfigBuilder().setSetFactory(() -> new CollectionBuilder() {
                final SortedSet<Object> s = new TreeSet<>();
                public void add(Object o) {
                    if (!s.add(o)) {
                        throw new EdnSyntaxException(
                          "Set contains duplicate element '" + o + "'."
                        );
                    }
                }
                public Object build() { return s; }
            }).build();
        Parseable pbr = newParseable("#{1 0 2 9 3 8 4 7 5 6}");
        Parser p = Parsers.newParser(cfg);
        SortedSet<?> s = (SortedSet<?>) p.nextValue(pbr);
        // The elements of s are sorted since our SetFactory
        // builds a SortedSet, not a (Hash)Set.
        assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
            new ArrayList<Object>(s));
    }
}

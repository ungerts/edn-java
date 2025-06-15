/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import org.jabref.edn.Tag;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;
import org.jabref.edn.parser.TagHandler;


public class CustomTagHandler {
    @Test
    public void test() throws IOException, URISyntaxException {
        Parser.Config cfg =
            Parsers.newParserConfigBuilder()
            .putTagHandler(Tag.newTag("org.jabref", "uri"), (tag, value) -> URI.create((String) value)).build();
        Parser p = Parsers.newParser(cfg);
        Parseable pbr = Parsers.newParseable(
            "#org.jabref/uri \"http://example.com\"");
        assertEquals(new URI("http://example.com"), p.nextValue(pbr));
    }
}

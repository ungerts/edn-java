/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn;

import org.junit.Test;
import org.jabref.edn.parser.IOUtil;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;

import java.io.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;


public class SerializabilityTest {

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream objectsOut = new ObjectOutputStream(bytesOut);
        objectsOut.writeObject(o);
        objectsOut.close();
        return bytesOut.toByteArray();
    }

    private static Object deserialize(byte[] bytes)
      throws IOException, ClassNotFoundException {
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
        ObjectInputStream objectsIn = new ObjectInputStream(bytesIn);
        return objectsIn.readObject();
    }

    @Test
    public void testSerializability()
      throws IOException, ClassNotFoundException {
        Parseable pbr = Parsers.newParseable(IOUtil.stringFromResource(
          "org/jabref/edn/serializability.edn"));
        Parser parser = Parsers.newParser(Parsers.defaultConfiguration());
        Object expected = parser.nextValue(pbr);
        assertNotEquals(Parser.END_OF_INPUT, expected);
        List<Object> result = (List<Object>) deserialize(serialize(expected));
        assertEquals(expected, result);
    }

    @Test
    public void testKeywordIdentity()
      throws IOException, ClassNotFoundException {
        Parseable pbr = Parsers.newParseable(":keyword");
        Parser parser = Parsers.newParser(Parsers.defaultConfiguration());
        Keyword expected = (Keyword) parser.nextValue(pbr);
        Keyword result = (Keyword) deserialize(serialize(expected));
        assertSame(expected, result);
    }

}

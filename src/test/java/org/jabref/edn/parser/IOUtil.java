/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.jabref.edn.EdnException;


public class IOUtil {

    static final String ENCODING = "UTF-8";
    private static final int BUFFER_SIZE = 8*1024;
    private static final int INITIAL_BUILDER_SIZE = 8*1024;

    public static String stringFromResource(String resourceName) {
        URL url = IOUtil.class.getClassLoader().getResource(resourceName);
        if (url == null) {
            throw new EdnException("resource '"+ resourceName +"' not found.");
        }
        return stringFromURL(url);
    }

    static String stringFromURL(URL url) {
        try {
          try (InputStream urlStream = url.openStream()) {
            return stringFromInputStream(urlStream);
          }
        } catch (IOException e) {
            throw new EdnException(e);
        }
    }

    static String stringFromInputStream(InputStream urlStream) {
        try {
          try (Reader reader = new InputStreamReader(urlStream, ENCODING)) {
            char[] buffer = new char[BUFFER_SIZE];
            StringBuilder b = new StringBuilder(INITIAL_BUILDER_SIZE);
            int n;
            while ((n = reader.read(buffer)) >= 0) {
              b.append(buffer, 0, n);
            }
            return b.toString();
          }
        } catch (IOException e) {
            throw new EdnException(e);
        }
    }

}

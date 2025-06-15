/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

/**
 * Factory for creating {@link Scanner}s.
 */
public class Scanners {

    private static final Scanner DEFAULT_SCANNER =
        new ScannerImpl(Parsers.defaultConfiguration());

    /**
     * Provides a {@link Scanner}.
     *
     * @return a {@link Scanner}, never null.
     */
    public static Scanner newScanner() {
        return DEFAULT_SCANNER;
    }

    private Scanners() {
        throw new UnsupportedOperationException();
    }

}

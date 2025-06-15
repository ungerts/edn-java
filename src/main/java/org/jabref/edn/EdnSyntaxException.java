/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn;

/**
 * EdnSyntaxException is thrown when a syntax error is discovered
 * during parsing.
 */
public class EdnSyntaxException extends EdnException {
    private static final long serialVersionUID = 1L;

    public EdnSyntaxException() {
        super();
    }

    public EdnSyntaxException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public EdnSyntaxException(String msg) {
        super(msg);
    }

    public EdnSyntaxException(Throwable cause) {
        super(cause);
    }
}

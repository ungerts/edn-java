/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn;

/**
 * EdnException is thrown when something goes wrong during the
 * operation of edn-java.  During parsing, this generally, the
 * indicates some kind of syntax error in the input source
 * (see {@link EdnSyntaxException}), or an I/O error (see
 * {@link EdnIOException}).
 */
public class EdnException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EdnException() {
        super();
    }

    public EdnException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public EdnException(String msg) {
        super(msg);
    }

    public EdnException(Throwable cause) {
        super(cause);
    }
}

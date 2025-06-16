/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn;

import java.io.IOException;

/**
 * Indicates that an I/O error occurred. The cause will be an
 * {@link IOException}.
 */
public class EdnIOException extends EdnException {
    private static final long serialVersionUID = 1L;

    public EdnIOException(String msg, IOException cause) {
        super(msg, cause);
    }

    public EdnIOException(IOException cause) {
        super(cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }

}

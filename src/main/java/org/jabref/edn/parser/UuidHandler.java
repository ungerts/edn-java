/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import java.util.UUID;

import org.jabref.edn.EdnSyntaxException;
import org.jabref.edn.Tag;


class UuidHandler implements TagHandler {

    public Object transform(Tag tag, Object value) {
        if (!(value instanceof String)) {
             throw new EdnSyntaxException(tag.toString() +
                                          " expects a String.");
        }
        return UUID.fromString((String) value);
    }

}

/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import org.jabref.edn.Tag;

/**
 * When a {@link Parser} encounters {@code #tag someEdnValue}, it uses
 * the  TagHandler registered  for  {@code #tag}  to transform  {@code
 * someEdnValue} before including it in the results of the parse.
 */
public interface TagHandler {

    /**
     * Consume {@code originalValue}, which is some edn value,
     * returning the value to replace it.
     *
     * @param tag the tag which preceded value, never null.
     * @param originalValue as parsed from the input, may be null.

     * @return a value to be used by the parser as the replacement for
     * {@code originalValue}.
     */
    Object transform(Tag tag, Object originalValue);

}

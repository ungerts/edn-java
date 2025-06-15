/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn;

/**
 * A named thing has a local {@code name} which may be further qualified by a
 * {@code prefix}. A prefix will always be present (not null), but may be empty.
 */
public interface Named {

    String EMPTY = "";

    /**
     * The name of this named thing, not including any prefix.
     * 
     * @return a non-empty string.
     */
    String getName();

    /**
     * The prefix, (also called namespace), which may be empty. A Named object
     * with an empty prefix is said to have no prefix.
     * 
     * @return a possibly empty string; never null.
     */
    String getPrefix();
}

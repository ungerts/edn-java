/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import org.jabref.edn.EdnSyntaxException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class DefaultSetFactory implements CollectionBuilder.Factory {
    public CollectionBuilder builder() {
        return new CollectionBuilder() {
            final Set<Object> set = new HashSet<>();
            public void add(Object o) {
                if (!set.add(o)) {
                    throw new EdnSyntaxException(
                      "Set contains duplicate element '" + o + "'.");
                }
            }
            public Object build() {
                return Collections.unmodifiableSet(set);
            }
        };
    }
}
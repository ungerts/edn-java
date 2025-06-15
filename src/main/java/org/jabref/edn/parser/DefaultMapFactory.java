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
import java.util.HashMap;
import java.util.Map;

final class DefaultMapFactory implements CollectionBuilder.Factory {
    public CollectionBuilder builder() {
        return new CollectionBuilder() {
            final Object none = new Object();
            final Map<Object,Object> map = new HashMap<>();
            Object key = none;
            public void add(Object o) {
                if (key == none) {
                    key = o;
                    if (map.containsKey(key)) {
                        throw new EdnSyntaxException(
                          "Map contains duplicate key '" + key + "'.");
                    }
                } else {
                    map.put(key, o);
                    key = none;
                }
            }
            public Object build() {
                if (key != none) {
                    throw new EdnSyntaxException(
                            "Every map must have an equal number of keys and values.");
                }
                return Collections.unmodifiableMap(map);
            }
        };
    }
}
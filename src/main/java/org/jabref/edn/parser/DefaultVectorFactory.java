/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import java.util.ArrayList;
import java.util.Collections;

final class DefaultVectorFactory implements CollectionBuilder.Factory {
    public CollectionBuilder builder() {
        return new CollectionBuilder() {
            final ArrayList<Object> list = new ArrayList<>();
            public void add(Object o) {
                list.add(o);
            }
            public Object build() {
                return Collections.unmodifiableList(list);
            }
        };
    }
}
/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

/**
 * The parser uses each CollectionBuilder to build a set, map, vector
 * or list.
 */
public interface CollectionBuilder {

    /**
     * Add an item to the collection being built. In the case of a
     * map, this will be called an even number of times, first for a
     * key and then for its corresponding value until all key-value
     * pairs of the map have been added.
     * <p>
     * For other collections is can be called any number of times.
     * <p>
     * Implementations which construct Maps or Sets an should throw an
     * EdnSyntaxException if they detect a duplicate key (in the case of
     * a map) or a duplicate element (in the case of a set).
     * </p>
     *
     * <p>{@code add()} may not be called after {@code build()}.
     * @param o an object to add to the collection under construction. o may
     *          be null.
     */
    void add(Object o);

    /**
     * Return the collection containing all the elements previously
     * added. {@code build()} may only be called once. After {@code
     * build()} has been called, the builder is rendered useless and
     * can be discarded.
     *
     * @return The collection. Generally a Set, Map or some kind of List.
     */
    Object build();

    /**
     * The parser uses CollectionBuilder.Factory instances to get a
     * fresh CollectionBuilder each time it needs to build a set, map,
     * vector or list. (Any given Factory produces Collection builders
     * for either sets, maps, lists or vectors.)
     */
    interface Factory {

        /**
         * Returns a new CollectionBuilder.
         * @return a new CollectionBuilder, never null.
         */
        CollectionBuilder builder();
    }
}

/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import static org.jabref.edn.Symbol.newSymbol;

/**
 * A Keyword is {@linkplain Named}. Additionally it obeys the syntactic
 * restrictions defined for <a
 * href="https://github.com/edn-format/edn#keywords">edn Keywords</a>.
 * <p>
 * Note: Keywords print with a leading colon, but this is not part of the
 * keyword's name:
 *
 * <pre>
 * {@code // For the keyword ":foo/bar"
 * Keyword k = newKeyword("foo", "bar");
 * k.getName()   => "bar"
 * k.getPrefix() => "foo"
 * k.toString()  => ":foo/bar"}
 * </pre>
 */
public final class Keyword implements Named, Comparable<Keyword>, Serializable {
    private final Symbol sym;

    /** {@inheritDoc} */
    public String getPrefix() {
        return sym.getPrefix();
    }

    /** {@inheritDoc} */
    public String getName() {
        return sym.getName();
    }

    public static Keyword newKeyword(Symbol sym) {
        return INTERNER.intern(sym, new Keyword(sym));
    }

    /**
     * Provide a Keyword with the given prefix and name.
     * <p>
     * Keywords are interned, which means that any two keywords which are equal
     * (by value) will also be identical (by reference).
     *
     * @param prefix
     *            An empty String or a non-empty String obeying the restrictions
     *            specified by edn. Never null.
     * @param name
     *            A non-empty string obeying the restrictions specified by edn.
     *            Never null.
     * @return a Keyword, never null.
     */
    public static Keyword newKeyword(String prefix, String name) {
        return newKeyword(newSymbol(prefix, name));
    }

    /**
     * This is equivalent to {@code newKeyword("", name)}.
     *
     * @param name
     *            A non-empty string obeying the restrictions specified by edn.
     *            Never null.
     * @return a Keyword without a prefix, never null.
     * @see #newKeyword(String, String)
     */
    public static Keyword newKeyword(String name) {
        return newKeyword(newSymbol(EMPTY, name));
    }

    /**
     * Return a Keyword with the same prefix and name as {@code sym}.
     * @param sym a Symbol, never null
     */
    private Keyword(Symbol sym) {
        if (sym == null) {
            throw new NullPointerException();
        }
        this.sym = sym;
    }

    public String toString() {
        return ":" + sym;
    }

    public int compareTo(Keyword o) {
        if (this == o) {
            return 0;
        }
        return sym.compareTo(o.sym);
    }

    private static final Interner<Symbol, Keyword> INTERNER = new Interner<>();

    private Object writeReplace() {
        return new SerializationProxy(sym);
    }

    private void readObject(ObjectInputStream stream)
      throws InvalidObjectException {
        throw new InvalidObjectException("only proxy can be serialized");
    }

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Symbol sym;
        private SerializationProxy(Symbol sym) {
            this.sym = sym;
        }
        private Object readResolve() throws ObjectStreamException {
            return newKeyword(sym);
        }
    }
}

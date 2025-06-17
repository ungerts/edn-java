/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn;

import static org.jabref.edn.util.CharClassify.isDigit;
import static org.jabref.edn.util.CharClassify.symbolStart;
import org.jabref.edn.util.CharClassify;

import java.io.Serializable;

/**
 * A Symbol is {@linkplain Named}. Additionally it obeys the syntactic
 * restrictions defined for
 * <a href="https://github.com/edn-format/edn#symbols">edn Symbols</a>.
 */
public final class Symbol implements Named, Comparable<Symbol>, Serializable {

    private final String prefix;
    private final String name;

    /**
     * {@inheritDoc}
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    private Symbol(String prefix, String name) {
        this.prefix = !prefix.isEmpty() ? prefix : EMPTY;
        this.name = name;
    }

    /**
     * Provide a Symbol with the given prefix and name.
     * 
     * @param prefix
     *            An empty String or a non-empty String obeying the
     *            restrictions specified by edn. Never null.
     * @param name
     *            A non-empty string obeying the restrictions specified by edn.
     *            Never null.
     * @return a Symbol, never null.
     */
    public static Symbol newSymbol(String prefix, String name) {
        checkArguments(prefix, name);
        return new Symbol(prefix, name);
    }

    /**
     * Equivalent to {@code newSymbol("", name)}.
     * 
     * @param name
     *            A non-empty string obeying the restrictions specified by edn.
     *            Never null.
     * @return a Symbol with the given name and no prefix.
     */
    public static Symbol newSymbol(String name) {
        return newSymbol(EMPTY, name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getClass().getName().hashCode();
        result = prime * result + name.hashCode();
        result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Symbol other = (Symbol) obj;
        if (!name.equals(other.name)) {
            return false;
        }
        if (prefix == null) {
          return other.prefix == null;
        } else
          return prefix.equals(other.prefix);
    }

    @Override
    public String toString() {
        if (prefix.isEmpty()) {
            return name;
        }
        return prefix + "/" + name;
    }

    private static void checkArguments(String prefix, String name) {
        if (prefix == null) {
            throw new EdnException("prefix must not be null.");
        }
        if (name == null) {
            throw new EdnException("name must not be null.");
        }
        checkName("name", name);
        if (!prefix.isEmpty()) {
            checkName("prefix", prefix);
        }
    }

    private static void checkName(String label, String ident) {
        if (ident.isEmpty()) {
            throw new EdnSyntaxException("The " + label +
                                         " must not be empty.");
        }
        char first = ident.charAt(0);
        if (isDigit(first)) {
            throw new EdnSyntaxException("The " + label + " '" + ident
                    + "' must not begin with a digit.");
        }
        if (!symbolStart(first)) {
            throw new EdnSyntaxException("The " + label + " '" + ident
                    + "' begins with a forbidden character.");
        }
        if ((first == '.' || first == '-')
                && ident.length() > 1 && isDigit(ident.charAt(1))) {
            throw new EdnSyntaxException("The " + label + " '" + ident
                    + "' begins with a '-' or '.' followed by digit, "
                    + "which is forbidden.");
        }
        int n = ident.length();
        for (int i = 1; i < n; i++) {
            if (!CharClassify.symbolConstituent(ident.charAt(i))) {
                throw new EdnSyntaxException("The " + label + " '" + ident
                        + "' contains the illegal character '"
                        + ident.charAt(i) + "' at offset " + i + ".");
            }
        }
    }

    public int compareTo(Symbol right) {
        int cmp = prefix.compareTo(right.prefix);
        return cmp != 0 ? cmp : name.compareTo(right.name);
    }

}

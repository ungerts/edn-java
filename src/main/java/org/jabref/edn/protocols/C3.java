/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.protocols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jabref.edn.EdnException;

/**
 * Implements the <a
 * href="http://en.wikipedia.org/wiki/C3_linearization">C3 superclass
 * linearization</a> algorithm.
 */
class C3 {

    /**
     * Return a linearization for the inheritance hierarchy of the
     * {@code class} {@code c}. The linearization will includes
     * interfaces as well as classes and considers {@link Object} to
     * be the ultimate superclass.
     *
     * @param c represents an actual <em>class</em>, not an
     *        interface. Never null.
     *
     * @return The linearization of c: never null and never empty.
     *
     * @throws EdnException if the inheritance hierarchy of {@code c}
     *         makes it impossible to compute a consistent hierarchy
     *         for {@code c}.
     */
    static List<Class<?>> methodResolutionOrder(Class<?> c) {
        try {
            List<Class<?>> result = mro(c);
            if (c.getSuperclass() != null) {
                result.add(Object.class);
            }
            return result;
        } catch (InconsistentHierarchy e) {
            StringBuilder b = new StringBuilder()
                .append("Unable to compute a consistent ")
                .append("method resolution order for ")
                .append(c.getName());
            if (c.equals(e.problematicClass)) {
                b.append(".");
            } else {
                b.append(" because ").append(e.problematicClass.getName())
                .append(" has no consistent method resolution order.");
            }
            throw new EdnException(b.toString());
        }
    }

    private static List<Class<?>> mro(Class<?> c) throws InconsistentHierarchy {
        List<List<Class<?>>> seqsToMerge = new ArrayList<>();
        seqsToMerge.add(asList(c));
        List<Class<?>> supers = supers(c);
        for (Class<?> s : supers) {
            seqsToMerge.add(mro(s));
        }
        seqsToMerge.add(supers);
        try {
            return merge(seqsToMerge);
        } catch (InconsistentHierarchy e) {
            throw new InconsistentHierarchy(c);
        }
    }

    private static List<Class<?>> asList(Class<?> c) {
        List<Class<?>> result = new ArrayList<>(1);
        result.add(c);
        return result;
    }

    private static List<Class<?>> supers(Class<?> c) {
        Class<?> sc = c.getSuperclass();
        Class<?>[] interfaces = c.getInterfaces();
        List<Class<?>> result = new ArrayList<>();
        if (sc != null && sc != Object.class) {
            result.add(sc);
        }
      result.addAll(Arrays.asList(interfaces));
        return result;
    }

    private static List<Class<?>> merge(List<List<Class<?>>> seqsToMerge)
            throws InconsistentHierarchy {
        List<Class<?>> result = new ArrayList<>();
        while (!allAreEmpty(seqsToMerge)) {
            Class<?> candidate = findCandidate(seqsToMerge);
            if (candidate == null) {
                throw new InconsistentHierarchy();
            }
            result.add(candidate);
            removeCandidate(seqsToMerge, candidate);
        }
        return result;
    }

    private static boolean allAreEmpty(List<List<Class<?>>> lists) {
        for (List<?> l : lists) {
            if (!l.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> findCandidate(List<List<Class<?>>> seqsToMerge) {
        for (List<Class<?>> seq : seqsToMerge) {
            if (!seq.isEmpty() && !occursInSomeTail(seqsToMerge, seq.getFirst())) {
                return seq.getFirst();
            }
        }
        return null;
    }

    private static boolean occursInSomeTail(List<List<Class<?>>> seqsToMerge,
            Object c) {
        for (List<?> seq : seqsToMerge) {
            for (int i = 1; i < seq.size(); i++) {
                if (c.equals(seq.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void removeCandidate(List<List<Class<?>>> seqsToMerge,
            Class<?> candidate) {
        for (List<Class<?>> seq : seqsToMerge) {
            if (!seq.isEmpty() && candidate.equals(seq.getFirst())) {
                seq.removeFirst();
            }
        }
    }

    static class InconsistentHierarchy extends Exception {
        private static final long serialVersionUID = 1L;
        Class<?> problematicClass;

        InconsistentHierarchy(Class<?> problematicClass) {
            super();
            this.problematicClass = problematicClass;
        }

        InconsistentHierarchy() {
            super();
        }
    }

}

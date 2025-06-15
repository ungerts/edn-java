/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import static org.jabref.edn.TaggedValue.newTaggedValue;
import static org.jabref.edn.parser.Token.END_LIST;
import static org.jabref.edn.parser.Token.END_MAP_OR_SET;
import static org.jabref.edn.parser.Token.END_VECTOR;

import org.jabref.edn.*;


class ParserImpl implements Parser {

    private static final Object DISCARDED_VALUE = new Object() {
        @Override
        public String toString() { return "##discarded value##"; }
    };

    private final Config cfg;
    private final Scanner scanner;

    ParserImpl(Config cfg, Scanner scanner) {
        this.scanner = scanner;
        this.cfg = cfg;
    }

    public Object nextValue(Parseable pbr) {
        Object value = nextValue(pbr, false);
        if (value instanceof Token && value != END_OF_INPUT) {
            throw new EdnSyntaxException("Unexpected "+ value);
        }
        return value;
    }

    private Object nextValue(Parseable pbr, boolean discard) {
        Object curr = scanner.nextToken(pbr);
        if (curr instanceof Token) {
            switch ((Token) curr) {
            case BEGIN_LIST:
                return parseIntoCollection(cfg.getListFactory(),
                                           END_LIST, pbr, discard);
            case BEGIN_VECTOR:
                return parseIntoCollection(cfg.getVectorFactory(),
                                           END_VECTOR, pbr, discard);
            case BEGIN_SET:
                return parseIntoCollection(cfg.getSetFactory(),
                                           END_MAP_OR_SET, pbr, discard);
            case BEGIN_MAP:
                return parseIntoCollection(cfg.getMapFactory(),
                                           END_MAP_OR_SET, pbr, discard);
            case DEFAULT_NAMESPACE_FOLLOWS: {
                final String ns = parseNamespaceName(pbr, discard);
                Object t = scanner.nextToken(pbr);
                if (t != Token.BEGIN_MAP) {
                    throw new EdnSyntaxException(
                      "Expected #:" + ns + " to be followed by a map.");
                }
                return parseIntoCollection(new NamespacedMapFactory(ns),
                  END_MAP_OR_SET, pbr, discard);
            }
            case DISCARD:
                nextValue(pbr, true);
                return nextValue(pbr, discard);
            case NIL:
                return null;
            case END_OF_INPUT:
            case END_LIST:
            case END_MAP_OR_SET:
            case END_VECTOR:
                return curr;
            default:
                throw new EdnSyntaxException("Unrecognized Token: " + curr);
            }
        } else if (curr instanceof Tag) {
            return nextValue((Tag)curr, pbr, discard);
        } else {
            return curr;
        }
    }

    private String parseNamespaceName(Parseable pbr, boolean discard) {
        final Object nsObj = nextValue(pbr, discard);
        if (!(nsObj instanceof Symbol nsSym)) {
            throw new EdnSyntaxException(
              "Expected symbol following #:, but found: " + nsObj);
        }
      if (!nsSym.getPrefix().isEmpty()) {
            throw new EdnSyntaxException(
              "Expected symbol following #: to be namespaceless, " +
                "but found: " + nsSym);
        }
        return nsSym.getName();
    }

    private Object nextValue(Tag t, Parseable pbr, boolean discard) {
        Object v = nextValue(pbr, discard);
        if (discard) {
            // It doesn't matter what we return here, as it will be discarded.
            return DISCARDED_VALUE;
        }
        TagHandler x = cfg.getTagHandler(t);
        return x != null ? x.transform(t, v) : newTaggedValue(t, v);
    }

    private Object parseIntoCollection(CollectionBuilder.Factory f, Token end,
                                       Parseable pbr, boolean discard) {
        CollectionBuilder b = !discard ? f.builder() : null;
        for (Object o = nextValue(pbr, discard); 
             o != end; 
             o = nextValue(pbr, discard)) {
            if (o instanceof Token) {
                throw new EdnSyntaxException("Expected " + end +
                                             ", but found " + o);
            }
            if (!discard) {
                b.add(o);
            }
        }
        return !discard ? b.build() : null;
    }

    private class NamespacedMapFactory implements CollectionBuilder.Factory {
        private final String defaultNs;

        public NamespacedMapFactory(String defaultNs) {
            this.defaultNs = defaultNs;
        }

        @Override
        public CollectionBuilder builder() {
            return new NamespacedMapBuilder();
        }

        private class NamespacedMapBuilder implements CollectionBuilder {
            private final CollectionBuilder cfgBuilder =
              cfg.getMapFactory().builder();
            boolean key = true;

            @Override
            public void add(Object o) {
                if (key) {
                    o = maybeApplyDefaultNamespace(o);
                }
                key = !key;
                cfgBuilder.add(o);
            }

            @Override
            public Object build() {
                return cfgBuilder.build();
            }

            private Object maybeApplyDefaultNamespace(final Object o) {
                if (!(o instanceof Symbol || o instanceof Keyword)) {
                    return o;
                }

                final Named named = (Named) o;

                final String prefix = named.getPrefix();
                final String ns;
                if ("".equals(prefix)) {
                    ns = defaultNs;
                } else if ("_".equals(prefix)) {
                    ns = "";
                } else {
                    return o;
                }

                final String name = named.getName();
                if (o instanceof Symbol) {
                    return Symbol.newSymbol(ns, name);
                } else {
                    assert o instanceof Keyword;
                    return Keyword.newKeyword(ns, name);
                }
            }
        }
    }
}

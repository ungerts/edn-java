/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import static org.jabref.edn.parser.Parser.Config.BIG_DECIMAL_TAG;
import static org.jabref.edn.parser.Parser.Config.BIG_INTEGER_TAG;
import static org.jabref.edn.parser.Parser.Config.DOUBLE_TAG;
import static org.jabref.edn.parser.Parser.Config.EDN_INSTANT;
import static org.jabref.edn.parser.Parser.Config.EDN_UUID;
import static org.jabref.edn.parser.Parser.Config.LONG_TAG;

import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jabref.edn.Tag;
import org.jabref.edn.parser.CollectionBuilder.Factory;
import org.jabref.edn.parser.Parser.Config;
import org.jabref.edn.parser.Parser.Config.Builder;
import org.jspecify.annotations.NonNull;

/**
 * Factory methods for all things related to {@link Parser}.
 *
 * Typical usage is as follows:
 *
 * <ol>
 *
 * <li>Ask
 *     {@link #newParserConfigBuilder()} for a new
 *     {@link Parser.Config.Builder}.</li>
 *
 * <li>Make any desired customizations. Primarily, this will mean using
 *     {@link Parser.Config.Builder#putTagHandler(Tag, TagHandler)}
 *     to register {@link TagHandler}s for any custom
 *     {@link org.jabref.edn.Tag}s used by your expected input documents.</li>
 *
 * <li>Pass the resulting {@link Parser.Config} to
 *     {@link #newParser(Parser.Config)} to create a {@link Parser}.</li>
 *
 * <li>Create one or more {@link Parseable}s using
 *     {@link #newParseable(CharSequence)} or
 *     {@link #newParseable(Readable)}.</li>
 *
 * <li>Use {@link Parser#nextValue(Parseable)} to get
 *     the edn values contained in your Parseable</li>
 *
 * </ol>
 */
public class Parsers {

    static final CollectionBuilder.Factory DEFAULT_LIST_FACTORY =
            new DefaultListFactory();

    static final CollectionBuilder.Factory DEFAULT_VECTOR_FACTORY =
            new DefaultVectorFactory();

    static final CollectionBuilder.Factory DEFAULT_SET_FACTORY =
            new DefaultSetFactory();

    static final CollectionBuilder.Factory DEFAULT_MAP_FACTORY =
            new DefaultMapFactory();

    static final TagHandler INSTANT_TO_DATE = new InstantToDate();

    static final TagHandler UUID_HANDLER = new UuidHandler();

    static final TagHandler IDENTITY = (tag, value) -> value;

    private Parsers() {
        throw new UnsupportedOperationException();
    }

    /**
     * Return a Parser configured by the given {@link Parser.Config}.
     *
     * @param cfg The configuration of the Parser. Must not be null.
     * @return a Parser, never null.
     */
    public static @NonNull Parser newParser(Parser.@NonNull Config cfg) {
        return new ParserImpl(cfg, new ScannerImpl(cfg));
    }

    static final int BUFFER_SIZE = 4096;

    static boolean readIntoBuffer(CharBuffer b, Readable r) throws IOException {
        b.clear();
        int n = r.read(b);
        b.flip();
        return n > 0;
    }

    static CharBuffer emptyBuffer() {
        CharBuffer b = CharBuffer.allocate(BUFFER_SIZE);
        b.limit(0);
        return b;
    }

    /**
     * Create a new {@link Parseable} wrapping the given {@link
     * CharSequence}.
     *
     * <p>The {@link java.io.Closeable#close()} method of the resulting
     * Parseable is a no-op.
     *
     * @param cs must not be null.
     *
     * @return a Parseable, never null.
     */
    public static @NonNull Parseable newParseable(@NonNull final CharSequence cs) {
        return new Parseable() {
            int i = 0;

            public void close() {
            }

            public int read() {
                try {
                    return cs.charAt(i++);
                } catch (IndexOutOfBoundsException suppressed) {
                    return Parseable.END_OF_INPUT;
                }
            }

            public void unread(int ch) {
                i--;
            }
        };
    }

    /**
     * Create a new {@link Parseable} wrapping the given {@link Readable}.
     *
     * <p>The {@link java.io.Closeable#close()} method of the
     * resulting Parseable closes the {@code r} if {@code r} is itself
     * Closable.
     * @param r a Readable which must not be null.
     * @return a Parseable over the given Readable.
     */
    public static @NonNull Parseable newParseable(@NonNull final Readable r) {
        return new Parseable() {
            final CharBuffer buff = emptyBuffer();
            int unread = Integer.MIN_VALUE;
            boolean end = false;
            boolean closed = false;

            public void close() throws IOException {
                closed = true;
                if (r instanceof Closeable closeable) {
                    closeable.close();
                }
            }

            public int read() throws IOException {
                if (closed) {
                    throw new IOException("Can not read from closed Parseable");
                }
                if (unread != Integer.MIN_VALUE) {
                    int ch = unread;
                    unread = Integer.MIN_VALUE;
                    return ch;
                }
                if (end) {
                    return Parseable.END_OF_INPUT;
                }
                if (buff.position() < buff.limit()) {
                    return buff.get();
                }
                if (readIntoBuffer(buff, r)) {
                    return buff.get();
                } else {
                    end = true;
                    return Parseable.END_OF_INPUT;
                }
            }

            public void unread(int ch) throws IOException {
                if (unread != Integer.MIN_VALUE) {
                    throw new IOException("Can't unread after unread.");
                }
                unread = ch;
            }
        };
    }

    /**
     * Return a new {@link Parser.Config.Builder}.
     *
     * @return a new {@link Parser.Config.Builder}, never null.
     */
    public static @NonNull Builder newParserConfigBuilder() {
        return new Builder() {
            boolean used = false;
            boolean acceptUnicodeEscapes = true;
            CollectionBuilder.Factory listFactory = DEFAULT_LIST_FACTORY;
            CollectionBuilder.Factory vectorFactory = DEFAULT_VECTOR_FACTORY;
            CollectionBuilder.Factory setFactory = DEFAULT_SET_FACTORY;
            CollectionBuilder.Factory mapFactory = DEFAULT_MAP_FACTORY;
            final Map<Tag, TagHandler> tagHandlers = defaultTagHandlers();

            public Builder setListFactory(CollectionBuilder.Factory listFactory) {
                checkState();
                this.listFactory = listFactory;
                return this;
            }

            public Builder setVectorFactory(CollectionBuilder.Factory vectorFactory) {
                checkState();
                this.vectorFactory = vectorFactory;
                return this;
            }

            public Builder setSetFactory(CollectionBuilder.Factory setFactory) {
                checkState();
                this.setFactory = setFactory;
                return this;
            }

            public Builder setMapFactory(CollectionBuilder.Factory mapFactory) {
                checkState();
                this.mapFactory = mapFactory;
                return this;
            }

            public Builder putTagHandler(Tag tag, TagHandler handler) {
                checkState();
                this.tagHandlers.put(tag, handler);
                return this;
            }

            @Override
            public Builder acceptUnicodeEscapesInStringLiterals(boolean acceptUnicodeEscapes) {
                checkState();
                this.acceptUnicodeEscapes = acceptUnicodeEscapes;
                return this;
            }

            public Config build() {
                checkState();
                used = true;
                return new Config() {
                    public Factory getListFactory() {
                        return listFactory;
                    }

                    public Factory getVectorFactory() {
                        return vectorFactory;
                    }

                    public Factory getSetFactory() {
                        return setFactory;
                    }

                    public Factory getMapFactory() {
                        return mapFactory;
                    }

                    public TagHandler getTagHandler(Tag tag) {
                        return tagHandlers.get(tag);
                    }

                    @Override
                    public boolean unicodeEscapesInStringLiteralsAreAccepted() {
                        return acceptUnicodeEscapes;
                    }
                };
            }

            private void checkState() {
                if (used) {
                    throw new IllegalStateException(
                            "Builder is single-use. Not usable after build()");
                }
            }
        };
    }

    static Map<Tag, TagHandler> defaultTagHandlers() {
        Map<Tag, TagHandler> m = new HashMap<>();
        m.put(EDN_UUID, UUID_HANDLER);
        m.put(EDN_INSTANT, INSTANT_TO_DATE);
        m.put(BIG_DECIMAL_TAG, IDENTITY);
        m.put(DOUBLE_TAG, IDENTITY);
        m.put(BIG_INTEGER_TAG, IDENTITY);
        m.put(LONG_TAG, IDENTITY);
        return m;
    }

    /**
     * Return the default configuration. This is equivalent to {@code
     * newParserConfigBuilder().build()}.
     *
     * @return a new {@link Parser.Config}, never null.
     */
    public static @NonNull Config defaultConfiguration() {
        return DEFAULT_CONFIGURATION;
    }

    public static final Config DEFAULT_CONFIGURATION = newParserConfigBuilder().build();

}

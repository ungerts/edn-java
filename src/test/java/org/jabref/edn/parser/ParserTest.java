/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.jabref.edn.Symbol.newSymbol;
import static org.jabref.edn.Tag.newTag;
import static org.jabref.edn.TaggedValue.newTaggedValue;
import static org.jabref.edn.parser.Parser.Config.BIG_DECIMAL_TAG;
import static org.jabref.edn.parser.Parser.Config.BIG_INTEGER_TAG;
import static org.jabref.edn.parser.Parser.Config.DOUBLE_TAG;
import static org.jabref.edn.parser.Parser.Config.LONG_TAG;
import static org.jabref.edn.parser.Parsers.defaultConfiguration;
import static org.jabref.edn.parser.Parsers.newParserConfigBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.RandomAccess;

import org.junit.Test;

import org.jabref.edn.EdnSyntaxException;
import org.jabref.edn.Keyword;
import org.jabref.edn.Symbol;



public class ParserTest {

    @Test
    public void parseEdnSample() {
        Parseable pbr = Parsers.newParseable(IOUtil.stringFromResource("org/jabref/edn/edn-sample.txt"));
        Parser parser = Parsers.newParser(Parsers.defaultConfiguration());

        List<Object> expected = Arrays.asList(
            map(ScannerTest.key("keyword"), ScannerTest.sym("symbol"), 1L,
                2.0d, new BigInteger("3"), new BigDecimal("4.0")),
                Arrays.asList(1L, 1L, 2L, 3L, 5L, 8L),
                new HashSet<Object>(Arrays.asList('\n', '\t')),
            List.of(List.of(Arrays.asList(true, false, null))));

        List<Object> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            results.add(parser.nextValue(pbr));
        }
        assertEquals(expected, results);
    }

    @Test
    public void parseTaggedValueWithUnkownTag() {
        assertEquals(newTaggedValue(newTag(newSymbol("foo", "bar")), 1L), parse("#foo/bar 1"));
    }

    @Test
    public void parseTaggedInstant() {
        assertEquals(1347235200000L, ((Date)parse("#inst \"2012-09-10\"")).getTime());
    }

    @Test
    public void parseTaggedUUID() {
        assertEquals(UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6"),
            parse("#uuid \"f81d4fae-7dec-11d0-a765-00a0c91e6bf6\""));
    }

    private static final String INVALID_UUID = "#uuid \"f81d4fae-XXXX-11d0-a765-00a0c91e6bf6\"";

    @Test(expected=NumberFormatException.class)
    public void invalidUUIDCausesException() {
        parse(INVALID_UUID);
    }

    @Test
    public void discardedTaggedValuesDoNotCallTransformer() {
        // The given UUID is invalid, as demonstrated in the test above.
        // were the transformer for #uuid to be called despite the #_,
        // it would throw an exception and cause this test to fail.

        assertEquals(123L, parse("#_ " + INVALID_UUID + " 123"));
    }

    /**
     * <p>
     * This tests parsing of Namespaced maps as per
     * <a href="http://dev.clojure.org/jira/browse/CLJ-1910">CLJ-1910</a>.
     * </p>
     * <p>
     * A map may be optionally preceded by #:SYM, where SYM will be taken to be the
     * namespace off all unnamespaced symbol or keyword keys in the map so introduced.
     * Furthermore, symbol and keyword keys in the map with the namespace "_" will
     * emerge unnamespaced from the parsing.
     * </p>
     */
    @Test
    public void parserUnderstandsNamespacedMaps() {
        assertEquals(
          parse("#:foo{ :a 1, b 2, _/c 3, :_/d 4, bar/e 5, :bar/f 6}"),
          parse("{:foo/a 1, foo/b 2, c 3, :d 4, bar/e 5, :bar/f 6}")
        );
    }

    /**
     * This is just a sanity check to make sure that the fact that we add
     * support of namespaced maps (which assign "_" a special meaning as a
     * namespace prefix on keys) does not interfere with the use of "_" as
     * a namespace on keys in non-namespaced maps.
     */
    @Test
    public void parserShouldNotBeConfusedByUnderscoreInNonNamespacedMaps() {
        Map<?,?> m = (Map<?, ?>) parse("{:_/foo 1, _/bar 2}");
        assertEquals(1L, m.get(Keyword.newKeyword("_", "foo")));
        assertEquals(2L, m.get(Symbol.newSymbol("_", "bar")));
    }

    @Test(expected=EdnSyntaxException.class)
    public void parserShouldDetectDuplicateMapKeys() {
        parse("{:a 1, :a 2}");
    }

    @Test(expected=EdnSyntaxException.class)
    public void parserShouldDetectDuplicateMapKeysInNamespacedMaps() {
        parse("#:foo{:foo/a 1, :a 2}");
    }

    @Test(expected=EdnSyntaxException.class)
    public void parserShouldDetectDuplicateSetElements() {
        parse("#{1 1}");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void parserShouldReturnUnmodifiableListByDefault() {
        ((List<?>)parse("(1)")).removeFirst();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void parserShouldReturnUnmodifiableVectorByDefault() {
        ((List<?>)parse("[1]")).removeFirst();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void parserShouldReturnUnmodifiableSetByDefault() {
        ((Set<?>)parse("#{1}")).remove(1);

    }

    @Test(expected=UnsupportedOperationException.class)
    public void parserShouldReturnUnmodifiableMapByDefault() {
        ((Map<?,?>)parse("{1,-1}")).remove(1);

    }

    @Test
    public void integersParseAsLongByDefault() {
        List<?> expected = Arrays.asList(
            Long.MIN_VALUE, (long)Integer.MIN_VALUE,
            -1L, 0L, 1L,
            (long)Integer.MAX_VALUE, Long.MAX_VALUE);
        List<?> results = (List<?>)parse("[" +
            Long.MIN_VALUE + ", " + Integer.MIN_VALUE +
            ", -1, 0, 1, " +
            Integer.MAX_VALUE + ", " + Long.MAX_VALUE + "]");
        // In Java Integer and Long are never equal(), even if they have
        // the same value.
        assertEquals(expected, results);
    }

    @Test
    public void integersAutoPromoteToBigIfTooBig() {
        BigInteger tooNegative = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
        BigInteger tooPositive = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        List<?> expected = Arrays.asList(tooNegative, tooPositive);
        List<?> results = (List<?>)parse("[" + tooNegative +" " + tooPositive + "]");
        assertEquals(expected, results);
    }

    @Test
    public void canCustomizeParsingOfInteger() {
        Parser.Config cfg = newParserConfigBuilder()
            .putTagHandler(LONG_TAG, (tag, value) -> ((Long) value).intValue())
                .putTagHandler(BIG_INTEGER_TAG, (tag, value) -> ((BigInteger) value).intValue())
                    .build();
        List<Integer> expected = Arrays.asList(-1, 0, 0, 1);
        List<?> results = (List<?>) parse(cfg, "[-1N, 0, 0N, 1]");
        assertEquals(expected, results);
    }

    @Test
    public void canCustomizeParsingOfFloats() {
        Parser.Config cfg = newParserConfigBuilder()
            .putTagHandler(DOUBLE_TAG, (tag, value) -> {
                Double d = (Double) value;
                return d * 2.0;
            })
                .putTagHandler(BIG_DECIMAL_TAG, (tag, value) -> {
                    BigDecimal d = (BigDecimal)value;
                    return d.multiply(BigDecimal.TEN);
                })
                    .build();
        List<?> expected = Arrays.asList(BigDecimal.TEN.negate(),
            BigDecimal.ZERO,
            BigDecimal.TEN,
            -2.0d, 0.0d, 2.0d);
        List<?> results = (List<?>) parse(cfg, "[-1M, 0M, 1M, -1.0, 0.0, 1.0]");
        assertEquals(expected, results);
    }

    @Test
    public void issue32() {
        assertFalse(parse("()") instanceof RandomAccess);
        assertTrue(parse("[]") instanceof RandomAccess);
        assertFalse(parse("(1)") instanceof RandomAccess);
        assertTrue(parse("[1]") instanceof RandomAccess);
    }

    static Object parse(String input) {
        return parse(defaultConfiguration(), input);
    }

    static Object parse(Parser.Config cfg, String input) {
        return Parsers.newParser(cfg).nextValue(Parsers.newParseable(input));
    }

    private Map<Object, Object> map(Object... kvs) {
        Map<Object, Object> m = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            m.put(kvs[i], kvs[i + 1]);
        }
        return m;
    }

}

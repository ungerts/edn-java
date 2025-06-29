/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn;

import org.junit.Test;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;

public class EdnFormatTest {

// edn                      a51127a 2014-01-08
// ===

// extensible data notation [eed-n]

// # Rationale

// **edn** is an extensible data notation. A superset of **edn** is
// used by Clojure to represent programs, and it is used by Datomic
// and other applications as a data transfer format. This spec
// describes **edn** in isolation from those and other specific use
// cases, to help facilitate implementation of readers and writers in
// other languages, and for other uses.

// **edn** supports a rich set of built-in elements, and the
// definition of extension elements in terms of the others. Users of
// data formats without such facilities must rely on either convention
// or context to convey elements not included in the base set. This
// greatly complicates application logic, betraying the apparent
// simplicity of the format. **edn** is simple, yet powerful enough to
// meet the demands of applications without convention or complex
// context-sensitive logic.

// **edn** is a system for the conveyance of _values_. It is not a
// type system, and has no schemas.  Nor is it a system for
// representing objects - there are no reference types, nor should a
// consumer have an expectation that two equivalent elements in some
// body of **edn** will yield distinct object identities when read,
// unless a reader implementation goes out of its way to make such a
// promise.  Thus the resulting values should be considered immutable,
// and a reader implementation should yield values that ensure this,
// to the extent possible.

// **edn** is a set of definitions for acceptable _elements_. A use of
// **edn** might be a stream or file containing elements, but it could
// be as small as the conveyance of a single element in e.g. an HTTP
// query param.

// There is no enclosing element at the top level. Thus **edn** is
// suitable for streaming and interactive applications.

// The base set of elements in **edn** is meant to cover the basic set
// of data structures common to most programming languages. While
// **edn** specifies how those elements are formatted in text, it does
// not dictate the representation that results on the consumer side. A
// well behaved reader library should endeavor to map the elements to
// programming language types with similar semantics.

// # Spec

// Currently this specification is casual, as we gather feedback from
// implementors. A more rigorous e.g. BNF will follow.

// ## General considerations

// **edn** elements, streams and files should be encoded using
// **[UTF-8](http://en.wikipedia.org/wiki/UTF-8).

// Elements are generally separated by whitespace. Whitespace, other
// than within strings, is not otherwise significant, nor need
// redundant whitespace be preserved during transmissions. Commas `,`
// are also considered whitespace, other than within strings.

    @Test
    public void commaIsWhitespace() {
        assertEquals(parse(""), parse(","));
        assertEquals(parse(" "), parse(","));
        assertEquals(parseall("a      b"), parseall("a,b"));
    }

// The delimiters `{ } ( ) [ ]` need not be separated from adjacent
// elements by whitespace.

    @Test
    public void delimetersNeedNoWhitespace() {
        assertEquals(parse("((1)[2](3))"), parse("( ( 1 ),[ 2 ],( 3 ) )"));
    }

// ### # dispatch character

// Tokens beginning with `#` are reserved. The character following `#`
// determines the behavior. The dispatches `#{` (sets), `#_`
// (discard), #alphabetic-char (tag) are defined below. `#` is not a
// delimiter.

// ## Built-in elements

// ### nil

// `nil` represents nil, null or nothing. It should be read as an
// object with similar meaning on the target platform.

    @Test
    public void nilReadsAsNull() {
        assertNull(parse("nil"));
    }

// ### booleans

// `true` and `false` should be mapped to booleans.

// If a platform has canonic values for true and false, it is a
// further semantic of booleans that all instances of `true` yield
// that (identical) value, and similarly for `false`.

    @Test
    public void trueAndFalseReadAsBoolean() {
        assertTrue((boolean)parse("true"));
        assertFalse((boolean)parse("false"));
    }

// ### strings

// Strings are enclosed in `"double quotes"`. May span multiple
// lines. Standard C/Java escape characters `\t, \r, \n, \\ and \"`
// are supported.

    @Test
    public void stringsSupportJavaStyleEscapes() {
        assertEquals("\thello\rWORLD\n\"\\!",
          parse("\"\\thello\\rWORLD\\n\\\"\\\\!\""));
    }

    @Test
    public void stringsCanBeMultiline() {
        assertEquals("hello\nworld\r\n!",
          parse("\"hello\nworld\r\n!\""));
    }

    @Test /* This is an edn-java extension to edn */
    public void stringsAllowUnicodeEscapesOptionally() {
        assertEquals("\u123f", parse("\"\\" + "u123f\""));
    }

// ### characters

// Characters are preceded by a backslash: `\c`, `\newline`,
// `\return`, `\space` and `\tab` yield the corresponding
// characters. Unicode characters are represented with \ u NNNN as in
// Java. Backslash cannot be followed by whitespace.

    @Test
    public void characters() {
        assertEquals('a', parse("\\a"));
        assertEquals('0', parse("\\0"));
        assertEquals('ű', parse("\\ű"));
        assertEquals(' ', parse("\\space"));
        assertEquals('\r', parse("\\return"));
        assertEquals('\t', parse("\\tab"));
        assertEquals('\n', parse("\\newline"));
        assertEquals('\u123f', parse("\\" + "u123f"));
    }

// ### symbols

// Symbols are used to represent identifiers, and should map to
// something other than strings, if possible.

// Symbols begin with a non-numeric character and can contain
// alphanumeric characters and `. * + ! - _ ?  $ % & = < >`. If `-`,
// `+` or `.` are the first character, the second character (if any)
// must be non-numeric. Additionally, `: #` are allowed as constituent
// characters in symbols other than as the first character.

    @Test
    public void symbols() {
        for (int i = 0; i < 1000; i++) {
            final String s = generateRandomSymbolName(1, 2, 3, 31);
            assertEquals(Symbol.newSymbol(s), parse(s));
        }
    }

// `/` has special meaning in symbols. It can be used once only in the
// middle of a symbol to separate the _prefix_ (often a namespace)
// from the _name_, e.g. `my-namespace/foo`. `/` by itself is a legal
// symbol, but otherwise neither the _prefix_ nor the _name_ part can
// be empty when the symbol contains `/`.

// If a symbol has a _prefix_ and `/`, the following _name_ component
// should follow the first-character restrictions for symbols as a
// whole. This is to avoid ambiguity in reading contexts where
// prefixes might be presumed as implicitly included namespaces and
// elided thereafter.

    @Test
    public void namespacedSymbols() {
        for (int i = 0; i < 1000; i++) {
            String prefix;
            while((prefix = generateRandomSymbolName(1, 2, 3, 17)).equals("/"));
            String name;
            while((name = generateRandomSymbolName(1, 2, 3, 17)).equals("/"));
            Symbol expected = Symbol.newSymbol(prefix, name);
            String edntext = prefix + "/" + name;
            assertEquals(expected, parse(edntext));
        }
    }

// ### keywords

// Keywords are identifiers that typically designate themselves. They
// are semantically akin to enumeration values. Keywords follow the
// rules of symbols, except they can (and must) begin with `:`,
// e.g. `:fred` or `:my/fred`. If the target platform does not have a
// keyword type distinct from a symbol type, the same type can be used
// without conflict, since the mandatory leading `:` of keywords is
// disallowed for symbols. Per the symbol rules above, :/ and
// :/anything are not legal keywords.  A keyword cannot begin with ::

    @Test
    public void keywords() {
        for (int i = 0; i < 1000; i++) {
            String name;
            while ((name = generateRandomSymbolName(1, 2, 3, 17)).equals("/")) ;
            Keyword expected = Keyword.newKeyword(name);
            String edntext = ":" + name;
            assertEquals(expected, parse(edntext));

            String prefix;
            while((prefix = generateRandomSymbolName(1, 2, 3, 17)).equals("/"));

            expected = Keyword.newKeyword(prefix, name);
            edntext = ":" + prefix + "/" + name;
            assertEquals(expected, parse(edntext));
        }
    }

    @Test(expected = EdnSyntaxException.class)
    public void keywordJustSlashIsIllegal() {
        parse(":/");
    }

// If the target platform supports some notion of interning, it is a
// further semantic of keywords that all instances of the same keyword
// yield the identical object.

// ### integers

// Integers consist of the digits `0` - `9`, optionally prefixed by
// `-` to indicate a negative number, or (redundantly) by `+`. No
// integer other than 0 may begin with 0. 64-bit (signed integer)
// precision is expected. An integer can have the suffix `N` to
// indicate that arbitrary precision is desired. -0 is a valid integer
// not distinct from 0.

//     integer
//       int
//       int N
//     digit
//       0-9
//     int
//       digit
//       1-9 digits
//       + digit
//       + 1-9 digits
//       - digit
//       - 1-9 digits

    @Test
    public void integers() {
        assertEquals(0L, parse("0"));
        assertEquals(0L, parse("-0"));
        assertEquals(0L, parse("+0"));
        assertEquals(1L, parse("1"));
        assertEquals(-1L, parse("-1"));
        assertEquals(1L, parse("+1"));
        assertEquals((long)Short.MAX_VALUE, parse("" + Short.MAX_VALUE));
        assertEquals((long)Short.MIN_VALUE, parse("" + Short.MIN_VALUE));
        assertEquals((long)Integer.MAX_VALUE, parse("" + Integer.MAX_VALUE));
        assertEquals((long)Integer.MIN_VALUE, parse("" + Integer.MIN_VALUE));
        assertEquals(Long.MAX_VALUE, parse("" + Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, parse("" + Long.MIN_VALUE));
    }


// ### floating point numbers

// 64-bit (double) precision is expected.

//     floating-point-number
//       int M
//       int frac
//       int exp
//       int frac exp
//     digit
//       0-9
//     int
//       digit
//       1-9 digits
//       + digit
//       + 1-9 digits
//       - digit
//       - 1-9 digits
//     frac
//       . digits
//     exp
//       ex digits
//     digits
//       digit
//       digit digits
//     ex
//       e
//       e+
//       e-
//       E
//       E+
//       E-

    @Test public void floats() {
        assertEquals((double)999, parse("999E0"));
        assertEquals(1.234, parse("1.234"));
        assertEquals(1.234, parse("0.1234e+1"));
        assertEquals(1.234, parse("12.34E-1"));
        assertEquals(-1.234, parse("-1.234E0"));

        assertEquals(Double.MAX_VALUE, parse(Double.toString(Double.MAX_VALUE)));
        assertEquals(Double.MIN_NORMAL, parse(Double.toString(Double.MIN_NORMAL)));
        assertEquals(Double.MIN_VALUE, parse(Double.toString(Double.MIN_VALUE)));
    }

// In addition, a floating-point number may have the suffix `M` to
// indicate that exact precision is desired.

    @Test public void decimalFloats() {
        assertEquals(BigDecimal.valueOf(999L, 0), parse("999M"));
        assertEquals(BigDecimal.valueOf(1234, 3), parse("1.234M"));
        assertEquals(BigDecimal.valueOf(1234, 3), parse("0.1234e+1M"));
        assertEquals(BigDecimal.valueOf(1234, 3), parse("12.34E-1M"));
        assertEquals(BigDecimal.valueOf(-1234, 3), parse("-1.234E0M"));
    }

// ### lists

// A list is a sequence of values. Lists are represented by zero or
// more elements enclosed in parentheses `()`. Note that lists can be
// heterogeneous.
 
//     (a b 42)

    @Test public void lists() {
        assertEquals(Collections.emptyList(), parse("()"));
        assertEquals(List.of(1L), parse("(1)"));
        assertEquals(Arrays.asList(Symbol.newSymbol("foo", "bar"),
          Keyword.newKeyword("baz", "flux")), parse("(foo/bar :baz/flux)"));
    }

// ### vectors

// A vector is a sequence of values that supports random
// access. Vectors are represented by zero or more elements enclosed
// in square brackets `[]`. Note that vectors can be heterogeneous.

//     [a b 42]

    @Test public void vectors() {
        assertEquals(Collections.emptyList(), parse("[]"));
        assertEquals(List.of(1L), parse("[1]"));
        assertEquals(Arrays.asList(Symbol.newSymbol("foo", "bar"),
          Keyword.newKeyword("baz", "flux")), parse("[foo/bar :baz/flux]"));
    }

// ### maps

// A map is a collection of associations between keys and values. Maps
// are represented by zero or more key and value pairs enclosed in
// curly braces `{}`. Each key should appear at most once. No
// semantics should be associated with the order in which the pairs
// appear.

//     {:a 1, "foo" :bar, [1 2 3] four}

// Note that keys and values can be elements of any type. The use of
// commas above is optional, as they are parsed as whitespace.

// ### sets

// A set is a collection of unique values. Sets are represented by
// zero or more elements enclosed in curly braces preceded by `#`
// `#{}`. No semantics should be associated with the order in which
// the elements appear. Note that sets can be heterogeneous.

//     #{a b [1 2 3]}

// ## tagged elements

// **edn** supports extensibility through a simple mechanism. `#`
// followed immediately by a symbol starting with an alphabetic
// character indicates that _that symbol_ is a **_tag_**. A tag
// indicates the semantic interpretation of _the following
// element_. It is envisioned that a reader implementation will allow
// clients to register handlers for specific tags. Upon encountering a
// tag, the reader will first read the next element (which may itself
// be or comprise other tagged elements), then pass the result to the
// corresponding handler for further interpretation, and the result of
// the handler will be the data value yielded by the tag + tagged
// element, i.e. reading a tag and tagged element yields one
// value. This value is the value to be returned to the program and is
// not further interpreted as **edn** data by the reader.

// This process will bottom out on elements either understood or
// built-in.

// Thus you can build new distinct readable elements out of (and only
// out of) other readable elements, keeping extenders and extension
// consumers out of the text business.

// The semantics of a tag, and the type and interpretation of the
// tagged element are defined by the steward of the tag.

//     #myapp/Person {:first "Fred" :last "Mertz"}

// If a reader encounters a tag for which no handler is registered,
// the implementation can either report an error, call a designated
// 'unknown element' handler, or create a well-known generic
// representation that contains both the tag and the tagged element,
// as it sees fit. Note that the non-error strategies allow for
// readers which are capable of reading any and all **edn**, in spite
// of being unaware of the details of any extensions present.

// ### rules for tags

// Tag symbols without a prefix are reserved by **edn** for built-ins
// defined using the tag system.

// User tags _**must**_ contain a prefix component, which must be
// owned by the user (e.g. trademark or domain) or known unique in the
// communication context.

// A tag _may_ specify more than one format for the tagged element,
// e.g. both a string and a vector representation.

// Tags themselves are not elements. It is an error to have a tag
// without a corresponding tagged element.

// ## built-in tagged elements

// ### #inst "rfc-3339-format"

// An instant in time. The tagged element is a string in
// [RFC-3339](http://www.ietf.org/rfc/rfc3339.txt) format.

// `#inst "1985-04-12T23:20:50.52Z"`

// ### #uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"

// A [UUID](http://en.wikipedia.org/wiki/Universally_unique_identifier).
// The tagged element is a canonical UUID string representation.

// ## comments

// If a `;` character is encountered outside of a string, that
// character and all subsequent characters to the next newline should
// be ignored.

// ## discard

// `#` followed immediately by `_` is the discard sequence, indicating
// that the next element (whether separated from `#_` by whitespace or
// not) should be read and discarded. Note that the next element must
// still be a readable element. A reader should not call user-supplied
// tag handlers during the processing of the element to be discarded.

//     [a b #_foo 42] => [a b 42]

// The discard sequence is not an element. It is an error to have a
// discard sequence without a following element.

// ## equality

// Sets and maps have requirements that their elements and keys
// respectively be unique, which requires a mechanism for determining
// when 2 values are not unique (i.e. are equal).

// nil, booleans, strings, characters, and symbols are equal to values
// of the same type with the same **edn** representation.

// integers and floating point numbers should be considered equal to
// values only of the same magnitude, _type, and
// precision_. Comingling numeric types and precision in map/set
// key/elements, or constituents therein, is not advised.

// sequences (lists and vectors) are equal to other sequences whose
// count of elements is the same, and for which each corresponding
// pair of elements (by ordinal) is equal.

// sets are equal if they have the same count of elements and, for
// every element in one set, an equal element is in the other.

// maps are equal if they have the same number of entries, and for
// every key/value entry in one map an equal key is present and mapped
// to an equal value in the other.

// tagged elements must define their own equality semantics. #uuid
// elements are equal if their canonic representations are
// equal. #inst elements are equal if their representation strings
// designate the same timestamp per
// [RFC-3339](http://www.ietf.org/rfc/rfc3339.txt).
    
    Object parse(String edn) {
        return parse(edn, Parsers.defaultConfiguration());
    }

    Object parse(String edn, Parser.Config cfg) {
        Parseable pbr = Parsers.newParseable(edn);
        Parser par = Parsers.newParser(cfg);
        return par.nextValue(pbr);
    }

    List<Object> parseall(String edn) {
        Parseable pbr = Parsers.newParseable(edn);
        Parser par = Parsers.newParser(Parsers.defaultConfiguration());
        List<Object> res = new ArrayList<>();
        Object o;
        while ((o = par.nextValue(pbr)) != Parser.END_OF_INPUT) {
            res.add(o);
        }
        return res;
    }

    Random rnd = new Random();

    boolean maybe(float p) {
        return rnd.nextFloat() < p;
    }

    int rndint(int n) {
        return rnd.nextInt(n);
    }

    int choose(int ... n) {
        return n[rnd.nextInt(n.length)];
    }

    char choose(String s) {
        return s.charAt(rnd.nextInt(s.length()));
    }

    String generateRandomSymbolName(int ... lengths) {
        final String s;
        int n = choose(lengths);
        if (n == 1) {
            s = "" + choose("/.+-abcdzABCDZ*!_?$%&=<>");
        } else {
            StringBuilder b = new StringBuilder(n);
            char f;
            b.append(f = choose(".+-abcdzABCDZ*!_?$%&=<>"));
            if (f == '.' || f == '+' || f == '-')
                b.append(choose("#:'.+-abcdzABCDZ*!_?$%&=<>"));
            while (b.length() < n)
                b.append(choose("059#:'.+-abcdzABCDZ*!_?$%&=<>"));
            //s = b.subSequence(0, n).toString();
            while (b.length() > n)
                b.deleteCharAt(b.length()-1);
            s = b.toString();
        }
        return s;
    }

}

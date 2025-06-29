# edn-java

![Build Status](https://github.com/ungerts/edn-java/actions/workflows/ci.yml/badge.svg?branch=main)

*edn-java* is a library to parse (read) and print (write) [edn](https://github.com/edn-format/edn).

**Note:** This library is a fork of [bpsm/edn-java](https://github.com/bpsm/edn-java), adapted for use with recent Java versions and primarily focused on integration with JabRef. There is also experimental support for GitHub workflows.

**Status:** This library is still in an experimental state. Use with caution in production environments.

**Key changes in this fork:**

* Requires Java 21 and uses Java 21 syntax
* Experimental support for the java.time package
* Removed ThreadLocal usage to support virtual threads (pretty printing temporarily removed)
* Added module-info.java for Java modules
* Experimental support for Jspecify
* Still distributed under the Eclipse Public License 1.0

**TODOs:**

* Stabilize experimental features
* Improve test coverage
* Improve interoperability testing with EDN implementations like Clojure, etc.
* Improve softspace handling in the Printer implementation

## Installation

This is a Maven project with the following coordinates:

```xml
<dependency>
    <groupId>org.jabref</groupId>
    <artifactId>edn-java</artifactId>
    <version>0.8.2</version>
</dependency>
```

It is available through the OSS Sonatype Releases repository:

    https://oss.sonatype.org/content/repositories/releases

or the Gradle coordinates:
```groovy
compile 'org.jabref:edn-java:0.8.2'
```
## Parsing

You'll need to create a Parser and supply it with some input. Factory methods to create Parseable input are provided which accept either a `java.lang.CharSequence` or a `java.lang.Readable`. You can then call `nextValue()` on the Parser to get values form the input. When the input is exhausted, `nextValue()` will return `Parser.END_OF_INPUT`.


```java
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;
import static org.jabref.edn.Keyword.newKeyword;
import static org.jabref.edn.parser.Parsers.defaultConfiguration;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;

public class ParseASingleMapTest {
    @Test
    public void simpleUsageExample() throws IOException {
        Parseable pbr = Parsers.newParseable("{:x 1, :y 2}");
        Parser p = Parsers.newParser(defaultConfiguration());
        Map<?, ?> m = (Map<?, ?>) p.nextValue(pbr);
        assertEquals(m.get(newKeyword("x")), 1L);
        assertEquals(m.get(newKeyword("y")), 2L);
        assertEquals(Parser.END_OF_INPUT, p.nextValue(pbr));
    }
}
```

### Mapping from EDN to Java

Most *edn* values map to regular Java types, except in such cases where Java doesn't provide something suitable. Implementations of the types peculiar to edn are provided by the package `org.jabref.edn`.

`Symbol` and `Keyword` have an optional `prefix` and a mandatory `name`. Both implement the interface `Named`.

Integers map to, `Long` or `BigInteger` depending on the magnitude of the number. Appending `N` to an integer literal maps to `BigInteger` irrespective of the magnitude.

Floating point numbers with the suffix `M` are  mapped to `BigDecimal`. All others are mapped to `Double`.

Characters are mapped to `Character`, booleans to `Boolean` and strings to `String`. No great shock there, I trust.

Lists "(...)" and vectors "[...]" are both mapped to implementations of `java.util.List`. A vector maps to a List implementation that also implements the marker interface `java.util.RandomAccess`.

Maps map to `java.util.HashMap` and sets to `java.util.HashSet`.

The parser is provided a configuration when created:

    Parsers.newParser(Parsers.defaultConfiguration())

The parser can be customized to use different collection classes by first building the appropriate `Parser.Config`:

```java
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;
import static org.jabref.edn.parser.Parsers.newParseable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;
import org.jabref.edn.parser.CollectionBuilder;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;

public class SimpleParserConfigTest {
    @Test
    public void test() throws IOException {
        Parser.Config cfg =
            Parsers.newParserConfigBuilder().setSetFactory(() -> new CollectionBuilder() {
              SortedSet<Object> s = new TreeSet<>();

              public void add(Object o) {
                if (!s.add(o)) {
                  throw new EdnSyntaxException("Set contains duplicate element '" + o + "'.");
                }
              }

              public Object build() {
                return s;
              }
            }).build();
        Parseable pbr = newParseable("#{1 0 2 9 3 8 4 7 5 6}");
        Parser p = Parsers.newParser(cfg);
        SortedSet<?> s = (SortedSet<?>) p.nextValue(pbr);
        // The elements of s are sorted since our SetFactory
        // builds a SortedSet, not a (Hash)Set.
        assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
            new ArrayList<Object>(s));
    }
}
```

### Tagged Values

By default, handlers are provided automatically for `#inst` and `#uuid`, which return a `java.util.Date` and a `java.util.UUID` respectively. Tagged values with an unrecognized tag are mapped to `org.jabref.edn.TaggedValue`.

#### Customizing the parsing of instants

The package `org.jabref.edn.parser` makes three handlers for `#inst` available:

 - `InstantToDate` is the default and converts each `#inst` to a `java.util.Date`.
 - `InstantToCalendar` converts each `#inst` to a `java.util.Calendar`, which preserves the original GTM offset.
 - `InstantToTimestamp` converts each `#inst` to a `java.sql.Timstamp`, which preserves nanoseconds.

Extend `AbstractInstantHandler` to provide your own implementation of `#inst`.

#### Adding support for your own tags

Use custom handlers may by building an appropriate `Parser.Config`:

```java
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import org.jabref.edn.Tag;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;
import org.jabref.edn.parser.TagHandler;

public class CustomTagHandler {
    @Test
    public void test() throws IOException, URISyntaxException {
        Parser.Config cfg =
            Parsers.newParserConfigBuilder()
            .putTagHandler(Tag.newTag("org.jabref", "uri"), (tag, value) -> 
                URI.create((String) value)).build();
        Parser p = Parsers.newParser(cfg);
        Parseable pbr = Parsers.newParseable(
            "#org.jabref/uri \"http://example.com\"");
        assertEquals(new URI("http://example.com"), p.nextValue(pbr));
    }
}
```

#### Using pseudo-tags to influence the parsing of numbers

By default, integers not marked as arbitrary precision by the suffix "N" will parse as `java.lang.Long`. This can be influenced by installing handlers for the tag named by the constant `Parser.Config.LONG_TAG`.

```java
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.math.BigInteger;
import org.junit.Test;
import org.jabref.edn.Tag;
import org.jabref.edn.parser.Parseable;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;
import org.jabref.edn.parser.TagHandler;

public class CustomLongHandler {
    @Test
    public void test() throws IOException {
        Parser.Config cfg =
            Parsers.newParserConfigBuilder()
                .putTagHandler(Parser.Config.LONG_TAG, (tag, value) -> {
                  long n = (Long) value;
                  if (Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE) {
                    return (int) n;
                  } else {
                    return BigInteger.valueOf(n);
                  }
                }).build();
        Parser p = Parsers.newParser(cfg);
        Parseable pbr = Parsers.newParseable("1024, 2147483648");
        assertEquals(1024, p.nextValue(pbr));
        assertEquals(BigInteger.valueOf(2147483648L), p.nextValue(pbr));
    }
}
```

`Parser` also provides `BIG_DECIMAL_TAG`, `DOUBLE_TAG` and `BIG_INTEGER_TAG` to cover customizing all varieties of numbers.

## Printing

The package `org.jabref.edn.printer` provides an extensible printer for converting java data structures to valid *edn* text. The default configuration can print values of the following types, as well as Java's `null`, which prints as `nil`:

 - `org.jabref.edn.Keyword`
 - `org.jabref.edn.Symbol`
 - `org.jabref.edn.TaggedValue`
 - `java.lang.Boolean`
 - `java.lang.Byte`
 - `java.lang.CharSequence`, which includes `java.lang.String`.
 - `java.lang.Character`
 - `java.lang.Double`
 - `java.lang.Float`
 - `java.lang.Integer`
 - `java.lang.Long`
 - `java.lang.Short`
 - `java.math.BigInteger`
 - `java.meth.BigDecimal`
 - `java.sql.Timestamp`, as `#inst`.
 - `java.util.Date`, as `#inst`.
 - `java.util.GregorianCalendar`, as `#inst`.
 - `java.util.List`, as `[...]` or `(...)`.
 - `java.util.Map`
 - `java.util.Set`
 - `java.util.UUID`, as `#uuid`.

The `Printer` writes *characters* to the underlying `Writer`. To serialize this text to a file or across a network you'll need to arrange to convert the characters to bytes. Use *UTF-8*, as *edn* specifies.

### Formatting

The default Printer renders values as compactly as possible, which is beneficial when edn is used for communication. // pretty printing is no longer supported

```java
package org.jabref.edn.examples;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.parser.Parsers;
import org.jabref.edn.printer.Printers;

import java.util.Arrays;
import java.util.List;

public class PrintingExamples {
    @Test
    public void printCompactly() {
        Assert.assertThat(ACCEPTABLE_COMPACT_RENDERINGS,
                CoreMatchers.hasItem(Printers.printString(
                        Printers.defaultPrinterProtocol(),
                        VALUE_TO_PRINT)));
    }

    @Test
    public void printPretty() {
        Assert.assertThat(ACCEPTABLE_PRETTY_RENDERINGS,
                CoreMatchers.hasItem(Printers.printString(
                        Printers.prettyPrinterProtocol(),
                        VALUE_TO_PRINT)));
    }

    static final Object VALUE_TO_PRINT;
    static {
        Parser parser = Parsers.newParser(Parsers.defaultConfiguration());
        VALUE_TO_PRINT = parser.nextValue(Parsers.newParseable(
                "{:a [1 2 3],\n" +
                " [x/y] 3.14159}\n"));
    }

    static final List<String> ACCEPTABLE_COMPACT_RENDERINGS = Arrays.asList(
            "{:a[1 2 3][x/y]3.14159}",
            "{[x/y]3.14159 :a[1 2 3]}"
    );

    static final List<String> ACCEPTABLE_PRETTY_RENDERINGS = Arrays.asList(
            "{"           + "\n" +
            "  :a ["      + "\n" +
            "    1"       + "\n" +
            "    2"       + "\n" +
            "    3"       + "\n" +
            "  ]"         + "\n" +
            "  ["         + "\n" +
            "    x/y"     + "\n" +
            "  ] 3.14159" + "\n" +
            "}",
            "{"           + "\n" +
            "  ["         + "\n" +
            "    x/y"     + "\n" +
            "  ] 3.14159" + "\n" +
            "  :a ["      + "\n" +
            "    1"       + "\n" +
            "    2"       + "\n" +
            "    3"       + "\n" +
            "  ]"         + "\n" +
            "}"
    );
}
```

### Supporting additional types

To support additional types, you'll need to provide a `Protocol<Printer.Fn<?>>` to the `Printer` which binds your custom `Printer.Fn` implementations to the class (or interface) it is responsible for.

As an example, we'll add printing support for URIs:

```java
package org.jabref.edn.examples;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import org.junit.Test;
import org.jabref.edn.Tag;
import org.jabref.edn.printer.Printer;
import org.jabref.edn.printer.Printer.Fn;
import org.jabref.edn.printer.Printers;
import org.jabref.edn.protocols.Protocol;

public class CustomTagPrinter {
    private static final Tag BPSM_URI = Tag.newTag("org.jabref", "uri");
    @Test
    public void test() throws IOException {
        Protocol<Fn<?>> fns = Printers.defaultProtocolBuilder()
                .put(URI.class, (Fn<URI>) (self, writer) -> 
                    writer.printValue(BPSM_URI).printValue(self.toString()))
                    .build();
        StringWriter w = new StringWriter();
        Printer p = Printers.newPrinter(fns, w);
        p.printValue(URI.create("http://example.com"));
        p.close();
        assertEquals("#org.jabref/uri\"http://example.com\"", w.toString());
    }
}
```

### Limitations

 - Edn values must be *acyclic*. Any attempt to print a data structure containing cycles will surely end in a stack overflow.
 - The current Printing support strikes me a as a bit of a hack. The API may change with 1.0.0.
 - Edn-Java does not provide much by way of "convenience" methods. As a library it's still to young to really know what would be convenient, though I'm open to suggestions.

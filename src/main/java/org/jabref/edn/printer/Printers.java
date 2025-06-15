/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.printer;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

import org.jabref.edn.EdnException;
import org.jabref.edn.EdnIOException;
import org.jabref.edn.Keyword;
import org.jabref.edn.Symbol;
import org.jabref.edn.Tag;
import org.jabref.edn.TaggedValue;
import org.jabref.edn.parser.InstantUtils;
import org.jabref.edn.parser.Parser;
import org.jabref.edn.protocols.Protocol;
import org.jabref.edn.protocols.Protocols;
import org.jabref.edn.util.CharClassify;
import org.jspecify.annotations.NonNull;

/**
 * Factory for creating {@link Printer}s and related Objects.
 */
public class Printers {
  private Printers() {
    // Prevent instantiation
    throw new UnsupportedOperationException();
  }

  /**
   * Return a new Printer with the default printing
   * protocol. Everything the printer prints will be appended to
   * {@code out}. {@link Printer#close()} will close {@code
   * out}, provided {@code out} implements {@link Closeable}.
   *
   * @param out to which values will be printed. Never null.
   * @return a Printer with default configuration, never null.
   */
  public static @NonNull Printer newPrinter(@NonNull final Appendable out) {
    return newPrinter(defaultPrinterProtocol(), out);
  }

  /**
   * Print {@code ednValue} to a new String using the default
   * printing protocol.
   *
   * @param ednValue the value to be returned as a String in edn syntax.
   * @return A string in edn syntax. Not null, not empty.
   */
  public static @NonNull String printString(@NonNull Object ednValue) {
    return printString(defaultPrinterProtocol(), ednValue);
  }

  /**
   * Print {@code ednValue} to a new String using the printing
   * protocol given as {@code fns}.
   *
   * @param fns      a Protocol which knows how to print all the classes
   *                 of objects that we'll be asking our Printer to print.
   *                 Never null. Never null.
   * @param ednValue the value to be returned as a String in edn syntax.
   * @return A string in edn syntax. Not null, not empty.
   */
  public static @NonNull String printString(@NonNull final Protocol<Printer.Fn<?>> fns, @NonNull Object ednValue) {
    StringBuilder sb = new StringBuilder();
    newPrinter(fns, sb).printValue(ednValue);
    return sb.toString();
  }

  /**
   * Return a new Printer with the printing protocol given as {@code
   * fns}. Everything the printer prints will be appended to {@code
   * writer}. {@link Printer#close()} will close {@code out}, if
   * {@code out} implements {@link Closeable}.
   *
   * @param fns a Protocol which knows how to print all the classes
   *            of objects that we'll be asking our Printer to print.
   *            Never null. Never null.
   * @param out to which values will be printed. Never null.
   * @return a Printer, never null.
   */
  public static @NonNull Printer newPrinter(@NonNull final Protocol<Printer.Fn<?>> fns, @NonNull final Appendable out) {
    return new DefaultPrinter(fns, out);
  }

  /**
   * Default implementation of the Printer interface.
   */
  public static class DefaultPrinter implements Printer {
    private final Protocol<Printer.Fn<?>> fns;
    private final Appendable out;
    private int softspace = 0;

    public DefaultPrinter(Protocol<Printer.Fn<?>> fns, Appendable out) {
      this.fns = fns;
      this.out = out;
    }

    @Override
    public void close() {
      if (out instanceof Closeable closeable) {
        try {
          closeable.close();
        } catch (IOException e) {
          throw new EdnIOException(e);
        }
      }
    }

    @Override
    public Printer append(CharSequence csq) {
      try {
        if (softspace > 1 && !csq.isEmpty() && !CharClassify.isWhitespace(csq.charAt(0))) {
          out.append(' ');
        }
        softspace = 0;
        out.append(csq);
        return this;
      } catch (IOException e) {
        throw new EdnIOException(e);
      }
    }

    @Override
    public Printer append(char c) {
      try {
        if (softspace > 1 && !CharClassify.isWhitespace(c)) {
          out.append(' ');
        }
        softspace = 0;
        out.append(c);
        return this;
      } catch (IOException e) {
        throw new EdnIOException(e);
      }
    }

    @Override
    public Printer printValue(Object ednValue) {
      @SuppressWarnings("unchecked")
      Printer.Fn<Object> printFn = (Printer.Fn<Object>) fns.lookup(getClassOrNull(ednValue));
      if (printFn == null) {
        throw new EdnException(
            String.format("Don't know how to write '%s' of type '%s'", ednValue, getClassOrNull(ednValue)));
      }
      printFn.eval(ednValue, this);
      return this;
    }

    @Override
    public Printer softspace() {
      softspace += 1;
      return this;
    }
  }

  static Class<?> getClassOrNull(Object o) {
    return o == null ? null : o.getClass();
  }

  /**
   * Returns a {@link org.jabref.edn.protocols.Protocol.Builder}
   * configured to produce a Protocol which knows how to print
   * these types of values:
   *
   * <ul>
   * <li>{@link BigDecimal}</li>
   * <li>{@link BigInteger}</li>
   * <li>{@link Boolean}</li>
   * <li>{@link Byte} (as an integer)</li>
   * <li>{@link CharSequence} (as a string literal)</li>
   * <li>{@link Character} (as a character literal)</li>
   * <li>{@link Date} (as {@code #inst})</li>
   * <li>{@link Double}</li>
   * <li>{@link Float}</li>
   * <li>{@link GregorianCalendar} (as {@code #inst})</li>
   * <li>{@link Integer}</li>
   * <li>{@link Keyword}</li>
   * <li>{@link List}</li>
   * <li>{@link Long}</li>
   * <li>{@link Map}</li>
   * <li>{@link Set}</li>
   * <li>{@link Short} (as an integer)</li>
   * <li>{@link Symbol}</li>
   * <li>{@link Tag}</li>
   * <li>{@link TaggedValue}</li>
   * <li>{@link java.sql.Timestamp} (as {@code #inst})</li>
   * <li>{@link UUID} (as {@code #uuid})</li>
   * </ul>
   *
   * @return a Protocol.Builder initialized with the default implementations
   * for printing.
   */
  public static Protocol.Builder<Printer.Fn<?>> defaultProtocolBuilder() {
    Protocol.Builder<Printer.Fn<?>> builder = Protocols.builder("print");

    // Register basic type printers
    registerBasicTypePrinters(builder);

    // Register collection printers
    registerCollectionPrinters(builder);

    // Register string and character printers
    registerStringPrinters(builder);

    // Register symbol and tag-related printers
    registerSymbolPrinters(builder);

    // Register date and time printers
    registerDateTimePrinters(builder);

    return builder;
  }

  private static void registerBasicTypePrinters(Protocol.Builder<Printer.Fn<?>> builder) {
    builder.put(null, BasicPrinters.NULL_PRINTER)
        .put(Boolean.class, BasicPrinters.BOOLEAN_PRINTER)
        .put(Byte.class, NumericPrinters.LONG_VALUE_PRINTER)
        .put(Short.class, NumericPrinters.LONG_VALUE_PRINTER)
        .put(Integer.class, NumericPrinters.LONG_VALUE_PRINTER)
        .put(Long.class, NumericPrinters.LONG_VALUE_PRINTER)
        .put(Float.class, NumericPrinters.DOUBLE_VALUE_PRINTER)
        .put(Double.class, NumericPrinters.DOUBLE_VALUE_PRINTER)
        .put(BigInteger.class, NumericPrinters.BIG_INTEGER_PRINTER)
        .put(BigDecimal.class, NumericPrinters.BIG_DECIMAL_PRINTER);
  }

  private static void registerCollectionPrinters(Protocol.Builder<Printer.Fn<?>> builder) {
    builder.put(List.class, CollectionPrinters.LIST_PRINTER)
        .put(Set.class, CollectionPrinters.SET_PRINTER)
        .put(Map.class, CollectionPrinters.MAP_PRINTER);
  }

  private static void registerStringPrinters(Protocol.Builder<Printer.Fn<?>> builder) {
    builder.put(CharSequence.class, TextPrinters.CHAR_SEQUENCE_PRINTER)
        .put(Character.class, TextPrinters.CHARACTER_PRINTER);
  }

  private static void registerSymbolPrinters(Protocol.Builder<Printer.Fn<?>> builder) {
    builder.put(Keyword.class, SymbolPrinters.KEYWORD_PRINTER)
        .put(Symbol.class, SymbolPrinters.SYMBOL_PRINTER)
        .put(TaggedValue.class, SymbolPrinters.TAGGED_VALUE_PRINTER)
        .put(Tag.class, SymbolPrinters.TAG_PRINTER);
  }

  private static void registerDateTimePrinters(Protocol.Builder<Printer.Fn<?>> builder) {
    builder.put(UUID.class, DateTimePrinters.UUID_PRINTER)
        .put(Date.class, DateTimePrinters.DATE_PRINTER)
        .put(Timestamp.class, DateTimePrinters.TIMESTAMP_PRINTER)
        .put(GregorianCalendar.class, DateTimePrinters.CALENDAR_PRINTER)
        .put(ZonedDateTime.class, DateTimePrinters.ZONED_DATE_TIME_PRINTER)
        .put(Instant.class, DateTimePrinters.INSTANT_PRINTER)
        // Support for additional java.time types
        .put(java.time.LocalDate.class, DateTimePrinters.LOCAL_DATE_PRINTER)
        .put(java.time.LocalTime.class, DateTimePrinters.LOCAL_TIME_PRINTER)
        .put(java.time.LocalDateTime.class, DateTimePrinters.LOCAL_DATE_TIME_PRINTER)
        .put(java.time.OffsetDateTime.class, DateTimePrinters.OFFSET_DATE_TIME_PRINTER)
        .put(java.time.OffsetTime.class, DateTimePrinters.OFFSET_TIME_PRINTER)
        .put(java.time.Year.class, DateTimePrinters.YEAR_PRINTER)
        .put(java.time.YearMonth.class, DateTimePrinters.YEAR_MONTH_PRINTER)
        .put(java.time.MonthDay.class, DateTimePrinters.MONTH_DAY_PRINTER);
  }

  /**
   * Return the default printer {@link Protocol}. This is equivalent
   * to {@code defaultProtocolBuilder().build()}.
   *
   * @return the default printing {@link Protocol}, never null.
   */
  public static Protocol<Printer.Fn<?>> defaultPrinterProtocol() {
    return defaultProtocolBuilder().build();
  }

  /**
   * Basic type printer implementations
   */
  private static class BasicPrinters {
    static final Printer.Fn<Void> NULL_PRINTER =
        (self, writer) -> writer.softspace().append("nil").softspace();

    static final Printer.Fn<Boolean> BOOLEAN_PRINTER =
        (self, writer) -> writer.softspace().append(self ? "true" : "false").softspace();
  }

  /**
   * Numeric type printer implementations
   */
  private static class NumericPrinters {
    static final Printer.Fn<Number> LONG_VALUE_PRINTER =
        (self, writer) -> writer.softspace().append(String.valueOf(self.longValue())).softspace();

    static final Printer.Fn<Number> DOUBLE_VALUE_PRINTER =
        (self, writer) -> writer.softspace().append(String.valueOf(self.doubleValue())).softspace();

    static final Printer.Fn<BigInteger> BIG_INTEGER_PRINTER =
        (self, writer) -> writer.softspace().append(self.toString()).append('N').softspace();

    static final Printer.Fn<BigDecimal> BIG_DECIMAL_PRINTER =
        (self, writer) -> writer.softspace().append(self.toString()).append('M').softspace();
  }

  /**
   * Collection printer implementations
   */
  private static class CollectionPrinters {
    static final Printer.Fn<List<?>> LIST_PRINTER = (self, writer) -> {
      boolean vec = self instanceof RandomAccess;
      writer.append(vec ? '[' : '(');
      for (Object o : self) {
        writer.printValue(o);
      }
      writer.append(vec ? ']' : ')');
    };

    static final Printer.Fn<Set<?>> SET_PRINTER = (self, writer) -> {
      writer.softspace();
      writer.append("#{");
      for (Object o : self) {
        writer.printValue(o);
      }
      writer.append('}');
    };

    static final Printer.Fn<Map<?, ?>> MAP_PRINTER = (self, writer) -> {
      writer.append('{');
      for (Map.Entry<?, ?> p : self.entrySet()) {
        writer.printValue(p.getKey()).printValue(p.getValue());
      }
      writer.append('}');
    };
  }

  /**
   * Text printer implementations
   */
  private static class TextPrinters {
    static final Printer.Fn<CharSequence> CHAR_SEQUENCE_PRINTER = (self, writer) -> {
      writer.append('"');
      for (int i = 0; i < self.length(); i++) {
        final char c = self.charAt(i);
        switch (c) {
        case '"' -> writer.append('\\').append('"');
        case '\b' -> writer.append('\\').append('b');
        case '\t' -> writer.append('\\').append('t');
        case '\n' -> writer.append('\\').append('n');
        case '\r' -> writer.append('\\').append('r');
        case '\f' -> writer.append('\\').append('f');
        case '\\' -> writer.append('\\').append('\\');
        default -> writer.append(c);
        }
      }
      writer.append('"');
    };

    static final Printer.Fn<Character> CHARACTER_PRINTER = (self, writer) -> {
      final char c = self;
      if (!CharClassify.isWhitespace(c)) {
        writer.append('\\').append(c);
      } else {
        switch (c) {
        case '\b' -> writer.append("\\backspace");
        case '\t' -> writer.append("\\tab");
        case '\n' -> writer.append("\\newline");
        case '\r' -> writer.append("\\return");
        case '\f' -> writer.append("\\formfeed");
        case ' ' -> writer.append("\\space");
        case ',' -> writer.append("\\,");
        default -> throw new EdnException("Whitespace character 0x" + Integer.toHexString(c) + " is unsupported.");
        }
      }
      writer.softspace();
    };
  }

  /**
   * Symbol printer implementations
   */
  private static class SymbolPrinters {
    static final Printer.Fn<Keyword> KEYWORD_PRINTER =
        (self, writer) -> writer.softspace().append(self.toString()).softspace();

    static final Printer.Fn<Symbol> SYMBOL_PRINTER =
        (self, writer) -> writer.softspace().append(self.toString()).softspace();

    static final Printer.Fn<TaggedValue> TAGGED_VALUE_PRINTER =
        (self, writer) -> writer.printValue(self.getTag()).printValue(self.getValue());

    static final Printer.Fn<Tag> TAG_PRINTER =
        (self, writer) -> writer.softspace().append(self.toString()).softspace();
  }

  /**
   * Date and time printer implementations
   */
  private static class DateTimePrinters {
    static final Printer.Fn<UUID> UUID_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_UUID).printValue(self.toString());

    static final Printer.Fn<Date> DATE_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT).printValue(InstantUtils.dateToString(self));

    static final Printer.Fn<Timestamp> TIMESTAMP_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.timestampToString(self));

    static final Printer.Fn<GregorianCalendar> CALENDAR_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.calendarToString(self));

    static final Printer.Fn<ZonedDateTime> ZONED_DATE_TIME_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.zonedDateTimeToString(self));

    static final Printer.Fn<Instant> INSTANT_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.instantToString(self));

    static final Printer.Fn<java.time.LocalDate> LOCAL_DATE_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.localDateToString(self));
    static final Printer.Fn<java.time.LocalTime> LOCAL_TIME_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.localTimeToString(self));
    static final Printer.Fn<java.time.LocalDateTime> LOCAL_DATE_TIME_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.localDateTimeToString(self));
    static final Printer.Fn<java.time.OffsetDateTime> OFFSET_DATE_TIME_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.offsetDateTimeToString(self));
    static final Printer.Fn<java.time.OffsetTime> OFFSET_TIME_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.offsetTimeToString(self));
    static final Printer.Fn<java.time.Year> YEAR_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.yearToString(self));
    static final Printer.Fn<java.time.YearMonth> YEAR_MONTH_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.yearMonthToString(self));
    static final Printer.Fn<java.time.MonthDay> MONTH_DAY_PRINTER =
        (self, writer) -> writer.printValue(Parser.Config.EDN_INSTANT)
            .printValue(InstantUtils.monthDayToString(self));
  }
}

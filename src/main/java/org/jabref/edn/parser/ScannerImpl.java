/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import static org.jabref.edn.Symbol.newSymbol;
import static org.jabref.edn.Tag.newTag;
import static org.jabref.edn.parser.Parser.Config.BIG_DECIMAL_TAG;
import static org.jabref.edn.parser.Parser.Config.BIG_INTEGER_TAG;
import static org.jabref.edn.parser.Parser.Config.DOUBLE_TAG;
import static org.jabref.edn.parser.Parser.Config.LONG_TAG;
import static org.jabref.edn.util.CharClassify.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.jabref.edn.EdnSyntaxException;
import org.jabref.edn.EdnIOException;
import org.jabref.edn.Keyword;
import org.jabref.edn.Symbol;
import org.jabref.edn.util.CharClassify;


class ScannerImpl implements Scanner {

    static final Symbol NIL_SYMBOL = newSymbol("nil");
    static final Symbol TRUE_SYMBOL = newSymbol("true");
    static final Symbol FALSE_SYMBOL = newSymbol("false");
    static final Symbol SLASH_SYMBOL = newSymbol("/");

    static final int END = -1;

    private final TagHandler longHandler;
    private final TagHandler bigDecimalHandler;
    private final TagHandler bigIntegerHandler;
    private final TagHandler doubleHandler;
    private final boolean unicodeEscapesInStringLiteralsAreAccepted;

    /**
     * Scanner may throw an IOException during construction, in which case
     * an attempt will be made to close Reader cleanly.
     * @param cfg this scanner's configuration, never null.
     */
    ScannerImpl(Parser.Config cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("cfg must not be null");
        }

        this.longHandler = cfg.getTagHandler(LONG_TAG);
        this.bigIntegerHandler = cfg.getTagHandler(BIG_INTEGER_TAG);
        this.doubleHandler = cfg.getTagHandler(DOUBLE_TAG);
        this.bigDecimalHandler = cfg.getTagHandler(BIG_DECIMAL_TAG);
        this.unicodeEscapesInStringLiteralsAreAccepted =
          cfg.unicodeEscapesInStringLiteralsAreAccepted();
    }

    /* (non-Javadoc)
     * @see org.jabref.edn.parser.ScannerIf#nextToken(org.jabref.edn.parser.Parseable)
     */
    public Object nextToken(Parseable pbr) {
        try {
            return scanNextToken(pbr);
        } catch (IOException e) {
            throw new EdnIOException(e);
        }
    }

    private Object scanNextToken(Parseable pbr) throws IOException {
        skipWhitespaceAndComments(pbr);
        int curr = pbr.read();
      return switch (curr) {
        case END -> Token.END_OF_INPUT;
        case 'a', 'b', 'c', 'd', 'e', '.', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
             'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '*', '!', '_', '?',
             '/', '$', '%', '&', '>', '<', '=', 'o', 'p', 'q', 'r', 's', 'g', 'h', 'i', 'j', 'k', 'l', 'm' -> readSymbol(curr, pbr);
        case 'f' -> readSymbolOrFalse(curr, pbr);
        case 'n' -> readSymbolOrNil(curr, pbr);
        case 't' -> readSymbolOrTrue(curr, pbr);
        case '+', '-' -> readSymbolOrNumber(curr, pbr);
        case ':' -> readKeyword(pbr);
        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> readNumber(curr, pbr);
        case '{' -> Token.BEGIN_MAP;
        case '}' -> Token.END_MAP_OR_SET;
        case '[' -> Token.BEGIN_VECTOR;
        case ']' -> Token.END_VECTOR;
        case '(' -> Token.BEGIN_LIST;
        case ')' -> Token.END_LIST;
        case '#' -> readHashDispatched(pbr);
        case '"' -> readStringLiteral(pbr);
        case '\\' -> readCharacterLiteral(pbr);
        default ->
            throw new EdnSyntaxException(String.format("Unexpected character '%c', \\" + "u%04x", (char) curr, curr));
      };
    }

    private Object readHashDispatched(Parseable pbr) throws IOException {
        int peek = pbr.read();
      return switch (peek) {
        case END -> throw new EdnSyntaxException("Unexpected end of input following '#'");
        case '{' -> Token.BEGIN_SET;
        case '_' -> Token.DISCARD;
        case ':' -> Token.DEFAULT_NAMESPACE_FOLLOWS;
        default -> newTag(readSymbol(peek, pbr));
      };
    }

    private Object readSymbolOrNumber(int curr, Parseable pbr)
            throws IOException {
        int peek = pbr.read();
        if (peek == END) {
            return readSymbol(curr, pbr);
        } else {
            unread(pbr, peek);
            if (isDigit((char)peek)) {
                return readNumber(curr, pbr);
            } else {
                return readSymbol(curr, pbr);
            }
        }
    }

    private static Parseable unread(Parseable pbr, int ch) throws IOException {
        pbr.unread(ch);
        return pbr;
    }

    private Object readSymbolOrTrue(int curr, Parseable pbr)
            throws IOException {
        Symbol sym = readSymbol(curr, pbr);
        return TRUE_SYMBOL.equals(sym) ? true : sym;
    }

    private Object readSymbolOrNil(int curr, Parseable pbr)
            throws IOException {
        Symbol sym = readSymbol(curr, pbr);
        return NIL_SYMBOL.equals(sym) ? Token.NIL : sym;
    }

    private Object readSymbolOrFalse(int curr, Parseable pbr)
            throws IOException {
        Symbol sym = readSymbol(curr, pbr);
        return FALSE_SYMBOL.equals(sym) ? false : sym;
    }

    private void skipWhitespaceAndComments(Parseable pbr) throws IOException {
        for (;;) {
            skipWhitespace(pbr);
            int curr = pbr.read();
            if (curr != ';') {
                unread(pbr, curr);
                break;
            }
            skipComment(pbr);
        }
    }

    private void skipWhitespace(Parseable pbr) throws IOException {
        int curr;
        do {
            curr = pbr.read();
        } while (curr != END && isWhitespace((char)curr));
        unread(pbr, curr);
    }

    private void skipComment(Parseable pbr) throws IOException {
        int curr;
        do {
            curr = pbr.read();
        } while (curr != END && curr != '\n' && curr != '\r');
        unread(pbr, curr);
    }

    private char readCharacterLiteral(Parseable pbr) throws IOException {
        int curr = pbr.read();
        if (curr == END) {
            throw new EdnSyntaxException("Unexpected end of input following ''");
        } else if (isWhitespace((char)curr) && curr != ',') {
            throw new EdnSyntaxException(
                    "A backslash introducing character literal must not be "+
                    "immediately followed by whitespace.");
        }
        StringBuilder b = new StringBuilder();
        do {
            b.append((char)curr);
            curr = pbr.read();
        } while (curr != END && !separatesTokens((char)curr));
        unread(pbr, curr);
        if (b.length() == 1) {
            return b.charAt(0);
        } else {
            return charForName(b.toString());
        }
    }

    private static char charForName(String name) {
        switch (name.charAt(0)) {
        case 'u':
            if (name.length() == 5) {
                try {
                    int n = Integer.parseInt(name.substring(1), 16);
                    if (Character.MIN_VALUE <= n && n <= Character.MAX_VALUE) {
                        return (char) n;
                    }
                    // Fall through to default, which throws EdnSyntaxException
                } catch (NumberFormatException e) {
                    // Fall through to default, which throws EdnSyntaxException
                }
            }
        case 'n':
            if ("newline".equals(name)) {
                return '\n';
            }
            // fall through
        case 's':
            if ("space".equals(name)) {
                return ' ';
            }
            // fall through
        case 't':
            if ("tab".equals(name)) {
                return '\t';
            }
            // fall through
        case 'b':
            if ("backspace".equals(name)) {
                return '\b';
            }
            // fall through
        case 'f':
            if ("formfeed".equals(name)) {
                return '\f';
            }
            // fall through
        case 'r':
            if ("return".equals(name)) {
                return '\r';
            }
            // fall through
        default:
            throw new EdnSyntaxException(
                    "The character \\"+ name +" was not recognized.");
        }
    }

    private String readStringLiteral(Parseable pbr) throws IOException {
        StringBuilder b = new StringBuilder();
        for (;;) {
            int curr = pbr.read();
            switch (curr) {
            case END:
                throw new EdnSyntaxException(
                        "Unexpected end of input in string literal");
            case '"':
                return b.toString();
            case '\\':
                curr = pbr.read();
                switch (curr) {
                case END:
                    throw new EdnSyntaxException(
                            "Unexpected end of input in string literal");
                case 'b':
                    b.append('\b');
                    break;
                case 't':
                    b.append('\t');
                    break;
                case 'n':
                    b.append('\n');
                    break;
                case 'f':
                    b.append('\f');
                    break;
                case 'r':
                    b.append('\r');
                    break;
                case '"':
                    b.append('"');
                    break;
                case '\'':
                    b.append('\'');
                    break;
                case '\\':
                    b.append('\\');
                    break;
                case 'u':
                    if (!unicodeEscapesInStringLiteralsAreAccepted) {
                        throw new EdnSyntaxException(
                          "Unsupported '" + ((char) curr)
                            + "' escape in string. "
                            + "(Unicode escapes disabled by Parser.Config)"
                        );
                    }
                    /*
                    2020-05-01 Support for reading unicode escapes within
                    string literals is an extension to EDN. It is not part of
                    the spec described here: https://github.com/edn-format/edn
                    */
                    int v = 0;
                    for (int i = 0; i < 4; i++) {
                        curr = pbr.read();
                        int d = Character.digit(curr, 16);
                        if (d == -1) {
                            throw new EdnSyntaxException(
                              "Invalid \\u Unicode escape in string.");
                        }
                        v = v * 16 + d;
                    }
                    b.append((char)v);
                    break;
                default:
                    throw new EdnSyntaxException("Unsupported '"+ ((char)curr)
                            +"' escape in string");
                }
                break;
            default:
                b.append((char)curr);
            }
        }
    }

    private Object readNumber(int curr, Parseable pbr) throws IOException {
        assert curr != END && CharClassify.startsNumber((char)curr);
        StringBuilder digits = new StringBuilder();

        if (curr != '+') {
            digits.append((char)curr);
        }
        curr = pbr.read();
        while (curr != END && isDigit((char)curr)) {
            digits.append((char)curr);
            curr = pbr.read();
        }

        if (curr == '.' || curr == 'e' || curr == 'E' || curr == 'M') {
            if (curr == '.') {
                do {
                    digits.append((char)curr);
                    curr = pbr.read();
                } while (curr != END && isDigit((char) curr));
            }

            if (curr == 'e' || curr == 'E') {
                digits.append((char)curr);
                curr = pbr.read();
                if (curr == END) {
                    throw new EdnSyntaxException(
                            "Unexpected end of input in numeric literal");
                }
                if (!(curr == '-' || curr == '+' || isDigit((char)curr))) {
                    throw new EdnSyntaxException(
                            "Not a number: '"+ digits + ((char)curr) +"'.");
                }
                do {
                    digits.append((char)curr);
                    curr = pbr.read();
                } while (curr != END && isDigit((char)curr));
            }

            final boolean decimal = (curr == 'M');
            if (decimal) {
                curr = pbr.read();
            }

            if (curr != END && !separatesTokens((char)curr)) {
                throw new EdnSyntaxException(
                        "Not a number: '"+ digits + ((char)curr) +"'.");
            }
            unread(pbr, curr);

            if (decimal) {
                BigDecimal d = new BigDecimal(digits.toString());
                return bigDecimalHandler.transform(BIG_DECIMAL_TAG, d);
            } else {
                double d = Double.parseDouble(digits.toString());
                return doubleHandler.transform(DOUBLE_TAG, d);
            }
        } else {
            final boolean bigint = (curr == 'N');
            if (bigint) {
                curr = pbr.read();
            }

            if (curr != END && !separatesTokens((char)curr)) {
                throw new EdnSyntaxException(
                        "Not a number: '"+ digits + ((char)curr) +"'.");
            }
            unread(pbr, curr);

            final BigInteger n = new BigInteger(digits.toString());

            if (bigint || MIN_LONG.compareTo(n) > 0 || n.compareTo(MAX_LONG) > 0) {
                return bigIntegerHandler.transform(BIG_INTEGER_TAG, n);
            } else {
                return longHandler.transform(LONG_TAG, n.longValue());
            }
        }
    }

    private Keyword readKeyword(Parseable pbr) throws IOException {
        Symbol sym = readSymbol(pbr);
        if (SLASH_SYMBOL.equals(sym)) {
            throw new EdnSyntaxException("':/' is not a valid keyword.");
        }
        return Keyword.newKeyword(sym);
    }

    private Symbol readSymbol(Parseable pbr) throws IOException {
        return readSymbol(pbr.read(), pbr);
    }

    private Symbol readSymbol(int curr, Parseable pbr) throws IOException {
        if (curr == END) {
            throw new EdnSyntaxException(
                    "Unexpected end of input while reading an identifier");
        }
        StringBuilder b = new StringBuilder();
        int n = 0;
        int p = Integer.MIN_VALUE;
        do {
            if (curr == '/') {
                n += 1;
                p = b.length();
            }
            b.append((char)curr);
            curr = pbr.read();
        } while (curr != END && symbolConstituent((char)curr));
        unread(pbr, curr);

        validateUseOfSlash(b, n, p);
        return makeSymbol(b, n, p);
    }

    private Symbol makeSymbol(StringBuilder b, int slashCount, int slashPos) {
        if (slashCount == 0) {
            return newSymbol(b.toString());
        } else if (slashCount == 1) {
            if (slashPos == 0) {
                assert b.length() == 1 && b.charAt(0) == '/';
                return newSymbol(b.toString());
            } else {
                return newSymbol(b.substring(0, slashPos), b.substring(slashPos+1));
            }
        } else {
            assert slashCount == 2 && slashPos == b.length() - 1 && b.charAt(b.length() - 2) == '/';
            return newSymbol(b.substring(0, slashPos - 1), "/");
        }
    }

    private void validateUseOfSlash(CharSequence s, int slashCount, int lastSlashPos) {
        assert !s.isEmpty();
        if (slashCount == 1) {
            if (s.length() != 1) {
                if (lastSlashPos == s.length() - 1) {
                    throw new EdnSyntaxException(
                            "The name '"+ s +"' must not end with '/'.");
                } else if (lastSlashPos == 0) {
                    throw new EdnSyntaxException(
                            "The name '"+ s +"' must not start with '/'.");
                }
            }
        } else if (slashCount == 2) {
            if (s.length() == 2) {
                throw new EdnSyntaxException("The name '//' is not valid.");
            } else if (lastSlashPos != s.length() - 1 || s.charAt(lastSlashPos - 1) != '/') {
                throw new EdnSyntaxException("Incorrect use of '/' in name.");
            }
        } else if (slashCount > 3) {
            throw new EdnSyntaxException("Too many '/' in name.");
        }
    }

    private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);


}

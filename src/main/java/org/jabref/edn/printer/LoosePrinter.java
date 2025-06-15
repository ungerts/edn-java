/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.printer;

import org.jabref.edn.printer.Printer;
import org.jabref.edn.printer.Printers;
import org.jabref.edn.protocols.Protocol;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Loose printer prints almost as compactly as the default printer, except that it inserts
 * a few discretionary spaces to improve readability for debugging scenarios.
 * <p>
 * This class is experimental.
 */
/*
 * This implementation is written to use only the public API of Printers (despite being
 * in the same package), thus serving as an excellent argument for rethinking the
 * design of the API to better support composition and extension. In this particular case,
 * it would have been very helpful indeed if the various writeXxxFn() methods were public.
 */
public class LoosePrinter {

    public static Printer newLoosePrinter(final Appendable out) {
        final Printer.Fn<Map<?,?>> defaultWriteMap;
        final Printer.Fn<Set<?>> defaultWriteSet;
        final Printer.Fn<List<?>> defaultWriteList;
        final Printer.Fn<CharSequence> defaultWriteCharSequence;

        {
            final Protocol<Printer.Fn<?>> defaults = Printers.defaultPrinterProtocol();
            defaultWriteMap = (Printer.Fn<Map<?,?>>) defaults.lookup(Map.class);
            defaultWriteSet = (Printer.Fn<Set<?>>) defaults.lookup(Set.class);
            defaultWriteList = (Printer.Fn<List<?>>) defaults.lookup(List.class);
            defaultWriteCharSequence = (Printer.Fn<CharSequence>) defaults.lookup(CharSequence.class);
        }

        final Protocol<Printer.Fn<?>> loose =
                Printers.defaultProtocolBuilder()
                .put(Map.class, (Printer.Fn<Map<?, ?>>) (self, writer) -> {
                    writer.softspace();
                    defaultWriteMap.eval(self, writer);
                    writer.softspace();
                })
                .put(Set.class, (Printer.Fn<Set<?>>) (self, writer) -> {
                    writer.softspace();
                    defaultWriteSet.eval(self, writer);
                    writer.softspace();
                })
                .put(List.class, (Printer.Fn<List<?>>) (self, writer) -> {
                    writer.softspace();
                    defaultWriteList.eval(self, writer);
                    writer.softspace();
                })
                .put(CharSequence.class, (Printer.Fn<CharSequence>) (self, writer) -> {
                    writer.softspace();
                    defaultWriteCharSequence.eval(self, writer);
                    writer.softspace();
                })
                .build();

        return Printers.newPrinter(loose, out);
    }


}

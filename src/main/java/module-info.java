module org.jabref.edn {
    requires static org.jspecify;
  requires java.sql;
  exports org.jabref.edn;
    exports org.jabref.edn.parser;
    exports org.jabref.edn.printer;
    exports org.jabref.edn.protocols;
}


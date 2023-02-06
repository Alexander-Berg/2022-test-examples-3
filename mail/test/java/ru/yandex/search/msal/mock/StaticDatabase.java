package ru.yandex.search.msal.mock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StaticDatabase {
    private final String url;
    private final Map<String, StaticTable> tables;

    StaticDatabase(final String url) {
        this.url = url;
        this.tables = new ConcurrentHashMap<>();
    }

    public String url() {
        return url;
    }

    public StaticTable table(final String name) {
        return tables.get(name);
    }

    public StaticTable create(
        final String name,
        final Map<String, DataType> meta)
        throws IllegalStateException
    {
        StaticTable table = new StaticTable(name, meta);
        if (tables.putIfAbsent(name, table) != null) {
            throw new IllegalStateException("Table already exists " + name);
        }

        return table;
    }
}

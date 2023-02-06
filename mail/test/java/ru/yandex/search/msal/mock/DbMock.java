package ru.yandex.search.msal.mock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DbMock {
    private final Map<String, StaticDatabase> databases
        = new ConcurrentHashMap<>();

    public DbMock() {
    }

    public StaticDatabase create(
        final String url)
        throws IllegalStateException
    {
        StaticDatabase database = new StaticDatabase(url);
        if (databases.putIfAbsent(url, database) != null) {
            throw new IllegalStateException("Database exists " + url);
        }

        return database;
    }

    public StaticDatabase db(final String url) {
        return databases.get(url);
    }
}

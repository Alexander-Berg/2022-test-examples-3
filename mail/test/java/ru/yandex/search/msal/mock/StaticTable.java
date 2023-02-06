package ru.yandex.search.msal.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class StaticTable {
    private final String name;
    private final Map<String, DataType> meta;

    private Map<String, Supplier<List<Map<String, String>>>> suppliers;

    StaticTable(
        final String name,
        final Map<String, DataType> meta)
    {
        this.name = name;
        this.meta = meta;
        this.suppliers = new ConcurrentHashMap<>();
    }

    public synchronized StaticTable add(
        final String sql,
        final Supplier<List<Map<String, String>>> supplier)
    {
        this.suppliers.put(sql, supplier);
        return this;
    }

    public synchronized StaticTable addStatic(
        final String sql,
        final List<Map<String, String>> data)
    {
        this.suppliers.put(sql, new StaticSupplier(data));
        return this;
    }

    public synchronized StaticTable onetime(
        final String sql,
        final List<Map<String, String>> data)
    {
        this.suppliers.put(sql, new OneTimeSupplier(data));
        System.err.println("Adding " + data + " To " + name + " " + this.toString());
        return this;
    }

    public Map<String, Supplier<List<Map<String, String>>>> suppliers() {
        return suppliers;
    }

    public String name() {
        return name;
    }

    public Map<String, DataType> meta() {
        return meta;
    }

    public List<Map<String, String>> get(final String sql) {
        Supplier<List<Map<String, String>>> supplier = suppliers.get(sql);
        if (supplier != null) {
            return supplier.get();
        }

        return null;
    }

    private static final class OneTimeSupplier
        implements Supplier<List<Map<String, String>>>
    {
        private List<Map<String, String>> data;

        private OneTimeSupplier(final List<Map<String, String>> data) {
            this.data = new ArrayList<>(data);
        }

        @Override
        public synchronized List<Map<String, String>> get() {
            List<Map<String, String>> result = data;
            data = null;
            return result;
        }
    }

    private static final class StaticSupplier
        implements Supplier<List<Map<String, String>>>
    {
        private final List<Map<String, String>> data;

        private StaticSupplier(final List<Map<String, String>> data) {
            this.data = Collections.unmodifiableList(new ArrayList<>(data));
        }

        @Override
        public List<Map<String, String>> get() {
            return data;
        }
    }
}

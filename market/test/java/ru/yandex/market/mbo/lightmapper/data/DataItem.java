package ru.yandex.market.mbo.lightmapper.data;

import java.time.Instant;

public class DataItem {
    private int id;
    private String name;
    private Instant version;

    public DataItem() {
    }

    public DataItem(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public DataItem setId(int id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public DataItem setName(String name) {
        this.name = name;
        return this;
    }

    public Instant getVersion() {
        return version;
    }

    public DataItem setVersion(Instant version) {
        this.version = version;
        return this;
    }
}

package ru.yandex.market.mbo.lightmapper.data;

import java.time.Instant;
import java.util.Objects;

import ru.yandex.market.mbo.lightmapper.reflective.annotations.GeneratedValue;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Id;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Version;

public class LightDataItem {
    @Id
    @GeneratedValue
    private final int id;
    private final String name;
    @Version
    private final Instant version;

    public LightDataItem(String name) {
        this(0, name, null);
    }

    public LightDataItem(int id, String name, Instant version) {
        this.id = id;
        this.name = name;
        this.version = version;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getVersion() {
        return version;
    }

    public LightDataItem withName(String newName) {
        return new LightDataItem(id, newName, version);
    }

    public LightDataItem withVersion(Instant newVersion) {
        return new LightDataItem(id, name, newVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LightDataItem that = (LightDataItem) o;
        return id == that.id && Objects.equals(name, that.name) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, version);
    }

    @Override
    public String toString() {
        return "LightDataItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version=" + version +
                '}';
    }
}

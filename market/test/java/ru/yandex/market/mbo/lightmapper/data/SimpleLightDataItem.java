package ru.yandex.market.mbo.lightmapper.data;

import java.util.Objects;

import ru.yandex.market.mbo.lightmapper.reflective.annotations.Id;

/**
 * Without generated id and version
 */
public class SimpleLightDataItem {
    @Id
    private final int id;
    private final String name;

    public SimpleLightDataItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleLightDataItem that = (SimpleLightDataItem) o;
        return id == that.id && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "SimpleLightDataItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

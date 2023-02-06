package ru.yandex.market.crm.campaign.domain.promo.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author apershukov
 */
public abstract class TestIdsGroup<T, C extends TestIdsGroup<T, C>> {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("items")
    private List<T> items;

    public String getName() {
        return name;
    }

    public C setName(String name) {
        this.name = name;
        return getThis();
    }

    public List<T> getItems() {
        return items;
    }

    public C setItems(List<T> items) {
        this.items = items;
        return getThis();
    }

    public String getId() {
        return id;
    }

    public C setId(String id) {
        this.id = id;
        return getThis();
    }

    @Override
    public String toString() {
        return "TestIdsGroup{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", items=" + items +
                '}';
    }

    abstract protected C getThis();
}

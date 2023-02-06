package ru.yandex.market.mbo.lightmapper.benchmark.state;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import ru.yandex.market.mbo.lightmapper.benchmark.data.Item;

@State(Scope.Benchmark)
public class ItemState {
    private Item item;
    private Item itemJson;
    private List<Item> simpleItems;
    private List<Item> jsonItems;
    private List<Item> verySimpleItems;

    public Item getItem() {
        return item;
    }

    public Item getItemJson() {
        return itemJson;
    }

    public List<Item> getSimpleItems() {
        return simpleItems;
    }

    public List<Item> getJsonItems() {
        return jsonItems;
    }

    public List<Item> getVerySimpleItems() {
        return verySimpleItems;
    }

    @Setup
    public void setup() {
        item = Item.randomItemNoJson();
        itemJson = Item.randomItem();

        simpleItems = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            simpleItems.add(Item.randomItemNoJson());
        }

        jsonItems = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            jsonItems.add(Item.randomItem());
        }

        verySimpleItems = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            verySimpleItems.add(Item.randomItem().setDate(null).setData(null));
        }
    }
}

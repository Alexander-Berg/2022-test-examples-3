package ru.yandex.market.mbo.lightmapper.benchmark.state;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepositoryImpl;
import ru.yandex.market.mbo.lightmapper.benchmark.data.Item;

@State(Scope.Benchmark)
public class SelectStateJson {

    private GenericMapperRepositoryImpl<Item, Integer> repository;

    @Setup
    public void setup(MapperState mapperState) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            items.add(Item.randomItem());
        }

        repository = mapperState.getRepository();
        repository.insertBatch(items);
    }

    @TearDown
    public void shutdown() {
        repository.deleteAll();
    }
}

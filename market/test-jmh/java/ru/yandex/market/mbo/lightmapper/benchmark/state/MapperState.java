package ru.yandex.market.mbo.lightmapper.benchmark.state;

import com.fasterxml.jackson.core.type.TypeReference;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import ru.yandex.market.mbo.lightmapper.CompositeGenericMapper;
import ru.yandex.market.mbo.lightmapper.GenericMapper;
import ru.yandex.market.mbo.lightmapper.GenericMapperRepositoryImpl;
import ru.yandex.market.mbo.lightmapper.InstantMapper;
import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mbo.lightmapper.MapperBuilder;
import ru.yandex.market.mbo.lightmapper.benchmark.data.Data;
import ru.yandex.market.mbo.lightmapper.benchmark.data.Item;
import ru.yandex.market.mbo.lightmapper.reflective.LightMapper;

@State(Scope.Benchmark)
public class MapperState {
    private LightMapper<Item> mapper = LightMapper.forClass(Item.class);
    private GenericMapper.Insert<Item> insert = mapper.getInsert();
    private GenericMapperRepositoryImpl<Item, Integer> repository;
    private GenericMapperRepositoryImpl<Item, Integer> compositeRepository;


    public LightMapper<Item> getMapper() {
        return mapper;
    }

    public GenericMapper.Insert<Item> getInsert() {
        return insert;
    }

    public GenericMapperRepositoryImpl<Item, Integer> getRepository() {
        return repository;
    }

    public GenericMapperRepositoryImpl<Item, Integer> getCompositeRepository() {
        return compositeRepository;
    }

    @Setup
    public void setup(ConnectionState connectionState) {
        repository = new GenericMapperRepositoryImpl<>(mapper, connectionState.getJdbcTemplate(),
                connectionState.getTransactionTemplate(), "test");

        compositeRepository = new GenericMapperRepositoryImpl<>(new MapperBuilder<>(Item::new)
                .map("id", Item::getId, Item::setId).mark(
                        CompositeGenericMapper.PRIMARY_KEY, CompositeGenericMapper.GENERATED)
                .map("name", Item::getName, Item::setName)
                .map("date", Item::getDate, Item::setDate, new InstantMapper())
                .map("data", Item::getData, Item::setData, JsonMapper.builder(new TypeReference<Data>() {
                }).build())
                .map("mapping_id", Item::getMappingId, Item::setMappingId)
                .build(),
                connectionState.getJdbcTemplate(), connectionState.getTransactionTemplate(), "test");
    }
}

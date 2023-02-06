package ru.yandex.market.mbo.lightmapper.benchmark;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mbo.lightmapper.benchmark.data.Data;
import ru.yandex.market.mbo.lightmapper.benchmark.data.Item;
import ru.yandex.market.mbo.lightmapper.benchmark.state.ConnectionState;
import ru.yandex.market.mbo.lightmapper.benchmark.state.MapperState;
import ru.yandex.market.mbo.lightmapper.benchmark.state.SelectState;
import ru.yandex.market.mbo.lightmapper.benchmark.state.SelectStateJson;

@SuppressWarnings({"SqlResolve", "unused"})
@Warmup(time = 60)
@Measurement(time = 60)
//@Warmup(time = 1, timeUnit = TimeUnit.MILLISECONDS)
//@Measurement(time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 1)
public class SelectBenchmark {
    @Benchmark
    public List<Item> selectSimpleManualNamed(SelectState selectState, ConnectionState connectionState) {
        return connectionState.getJdbcTemplate().query("select * from test", Collections.emptyMap(), (rs, i) ->
                new Item(rs.getInt("id"),
                        rs.getString("name"),
                        rs.getTimestamp("date").toInstant(),
                        null,
                        rs.getLong("mapping_id")));
    }

    @Benchmark
    public List<Item> selectSimpleManualOrdered(SelectState selectState, ConnectionState connectionState) {
        return connectionState.getJdbcTemplate().query("select id, name, date, data, mapping_id from test",
                Collections.emptyMap(), (rs, i) ->
                        new Item(rs.getInt(1),
                                rs.getString(2),
                                rs.getTimestamp(3).toInstant(),
                                null,
                                rs.getLong(5)));
    }

    @Benchmark
    public List<Item> selectSimpleMapper(SelectState selectState, MapperState mapperState) {
        return mapperState.getRepository().findAll();
    }

    @Benchmark
    public List<Item> selectSimpleCompositeMapper(SelectState selectState, MapperState mapperState) {
        return mapperState.getCompositeRepository().findAll();
    }

    @Benchmark
    public List<Item> selectJsonManualNamed(SelectStateJson selectState, ConnectionState connectionState) {
        return connectionState.getJdbcTemplate().query("select * from test", Collections.emptyMap(), (rs, i) -> {
            try {
                return new Item(rs.getInt("id"),
                        rs.getString("name"),
                        rs.getTimestamp("date").toInstant(),
                        JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(rs.getString("data"), new TypeReference<Data>() {
                        }),
                        rs.getLong("mapping_id"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Benchmark
    public List<Item> selectJsonManualOrdered(SelectStateJson selectState, ConnectionState connectionState) {
        return connectionState.getJdbcTemplate().query("select id, name, date, data, mapping_id from test",
                Collections.emptyMap(), (rs, i) -> {
                    try {
                        return new Item(rs.getInt(1),
                                rs.getString(2),
                                rs.getTimestamp(3).toInstant(),
                                JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(rs.getString(4), new TypeReference<Data>() {
                                }),
                                rs.getLong(5));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Benchmark
    public List<Item> selectJsonMapper(SelectStateJson selectState, MapperState mapperState) {
        return mapperState.getRepository().findAll();
    }

    @Benchmark
    public List<Item> selectJsonCompositeMapper(SelectStateJson selectState, MapperState mapperState) {
        return mapperState.getCompositeRepository().findAll();
    }
}

package ru.yandex.market.mbo.lightmapper.benchmark;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.annotation.Nonnull;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mbo.lightmapper.PreparedStatementSetterImpl;
import ru.yandex.market.mbo.lightmapper.benchmark.data.Data;
import ru.yandex.market.mbo.lightmapper.benchmark.data.Item;
import ru.yandex.market.mbo.lightmapper.benchmark.state.ConnectionState;
import ru.yandex.market.mbo.lightmapper.benchmark.state.ItemState;
import ru.yandex.market.mbo.lightmapper.benchmark.state.MapperState;
import ru.yandex.market.mbo.lightmapper.benchmark.state.PreparedSimpleInsertState;

@Warmup(time = 60)
@Measurement(time = 60)
@SuppressWarnings("SqlResolve")
//@Warmup(time = 1)
//@Measurement(time = 1)
@Fork(value = 1)
public class InsertBenchmark {
    @Benchmark
    public void syntheticSetStatementJsonManual(PreparedSimpleInsertState state, ItemState itemState)
            throws SQLException {
        PreparedStatement statement = state.getStatement();
        Item item = itemState.getItemJson();
        setToStatement(statement, item);
    }


    @Benchmark
    public void syntheticSetStatementJsonMapper(PreparedSimpleInsertState state, ItemState itemState,
                                                MapperState mapperState) {
        PreparedStatement statement = state.getStatement();
        mapperState.getInsert().setValuesToStatement(itemState.getItemJson(),
                new PreparedStatementSetterImpl(statement));
    }

    @Benchmark
    public void syntheticSetStatementSimpleManual(PreparedSimpleInsertState state, ItemState itemState)
            throws SQLException {
        PreparedStatement statement = state.getStatement();
        Item item = itemState.getItem();
        setToStatement(statement, item);
    }

    @Benchmark
    public void syntheticSetStatementSimpleMapper(PreparedSimpleInsertState state, ItemState itemState,
                                                  MapperState mapperState) {
        PreparedStatement statement = state.getStatement();
        mapperState.getInsert().setValuesToStatement(itemState.getItem(), new PreparedStatementSetterImpl(statement));
    }

    @Benchmark
    public void insertItemsMapper(ItemState items, MapperState mapperState) {
        mapperState.getRepository().insertBatch(items.getSimpleItems());
    }

    @Benchmark
    public void insertItemsCompositeMapper(ItemState items, MapperState mapperState) {
        mapperState.getCompositeRepository().insertBatch(items.getSimpleItems());
    }

    @Benchmark
    public void insertItemsManualOrdered(ItemState items, ConnectionState connectionState) {
        insertManualOrdered(connectionState, items.getSimpleItems());
    }

    @Benchmark
    public void insertItemsManualNamed(ItemState items, ConnectionState connectionState) {
        insertManualNamed(connectionState, items.getSimpleItems());
    }

    @Benchmark
    public void insertJsonMapper(ItemState items, MapperState mapperState) {
        mapperState.getRepository().insertBatch(items.getJsonItems());
    }

    @Benchmark
    public void insertJsonCompositeMapper(ItemState items, MapperState mapperState) {
        mapperState.getCompositeRepository().insertBatch(items.getJsonItems());
    }

    @Benchmark
    public void insertJsonManualOrdered(ItemState itemsState, ConnectionState connectionState) {
        insertManualOrdered(connectionState, itemsState.getJsonItems());
    }

    @Benchmark
    public void insertJsonManualNamed(ItemState itemState, ConnectionState connectionState) {
        insertManualNamed(connectionState, itemState.getJsonItems());
    }

    @Benchmark
    public void insertSimpleMapper(ItemState items, MapperState mapperState) {
        mapperState.getRepository().insertBatch(items.getVerySimpleItems());
    }

    @Benchmark
    public void insertSimpleCompositeMapper(ItemState items, MapperState mapperState) {
        mapperState.getCompositeRepository().insertBatch(items.getVerySimpleItems());
    }

    @Benchmark
    public void insertSimpleManualOrdered(ItemState itemsState, ConnectionState connectionState) {
        insertManualOrdered(connectionState,  itemsState.getVerySimpleItems());
    }

    @Benchmark
    public void insertSimpleManualNamed(ItemState itemState, ConnectionState connectionState) {
        insertManualNamed(connectionState, itemState.getVerySimpleItems());
    }

    private void insertManualNamed(ConnectionState connectionState, List<Item> simpleItems) {
        connectionState.getTransactionTemplate().execute(status ->
                connectionState.getJdbcTemplate().batchUpdate(
                        "insert into test (name, date, data, mapping_id) values (:name, :date, :data, :mapping_id)" +
                                " returning id",
                        simpleItems.stream().map(item -> new MapSqlParameterSource()
                                .addValue("name", item.getName())
                                .addValue("date", item.getDate() == null ? null : Timestamp.from(item.getDate()))
                                .addValue("data", convertJson(item.getData()))
                                .addValue("mapping_id", item.getMappingId())).toArray(SqlParameterSource[]::new)
                ));
    }

    private void insertManualOrdered(ConnectionState connectionState, List<Item> items) {
        connectionState.getTransactionTemplate().execute(status ->
                connectionState.getJdbcTemplate().getJdbcOperations().batchUpdate(
                        "insert into test (name, date, data, mapping_id) values (?, ?, ?, ?) returning id",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {
                                Item item = items.get(i);
                                setToStatement(ps, item);
                            }

                            @Override
                            public int getBatchSize() {
                                return items.size();
                            }
                        })
        );
    }

    private void setToStatement(PreparedStatement statement, Item item) throws SQLException {
        statement.setString(1, item.getName());
        if (item.getDate() != null) {
            statement.setTimestamp(2, Timestamp.from(item.getDate()));
        } else {
            statement.setObject(2, null);
        }
        statement.setObject(3, convertJson(item.getData()));
        statement.setLong(4, item.getMappingId());
    }

    private PGobject convertJson(Data data) {
        if (data == null) {
            return null;
        }

        PGobject o = new PGobject();
        o.setType("json");
        try {
            o.setValue(JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return o;
    }
}

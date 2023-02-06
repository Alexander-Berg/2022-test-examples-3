package ru.yandex.market.mbo.cardrender.app.service.yql;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author apluhin
 * @created 1/17/22
 */
public class YqlTestReader extends YqlReader<Integer> {

    public List<Integer> inputForHandler = new ArrayList<>();
    public AtomicInteger atomicInteger = new AtomicInteger();
    public Long inputOffset;

    protected YqlTestReader(JdbcTemplate yqlJdbcTemplate, StorageKeyValueService storageKeyValueService) {
        super(yqlJdbcTemplate, storageKeyValueService);
    }

    @Override
    String selectSql(Long offset, String table) {
        inputOffset = offset;
        return "test";
    }

    @Override
    void handleBatch(List<Integer> models) {
        inputForHandler.addAll(models);
    }

    @Override
    Integer extractRow(ResultSet resultSet) throws Exception {
        return atomicInteger.incrementAndGet();
    }

    @Override
    ReadType readType() {
        return ReadType.FULL;
    }

}

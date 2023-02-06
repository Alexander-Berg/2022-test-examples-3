package ru.yandex.market.pers.grade.core.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.pers.grade.core.MockedTest;

/**
 * @author grigor-vlad
 * 25.03.2022
 */
public class LoadJdbcBatchedTest extends MockedTest {
    private static final int DEFAULT_BATCH_SIZE = 1000;

    @Before
    public void clearTestTable() {
        pgJdbcTemplate.update("delete from ext_models_for_tickets");
    }

    @Test
    public void testBatchUpdate() {
        createModel(1L);
        createModel(2L);
        createModel(3L);

        List<Long> result = performBatch(DEFAULT_BATCH_SIZE);
        Assert.assertEquals(3, result.size());
    }

    @Test
    public void testBigBatchUpdate() {
        //большой апдейт проверяем через небольшой размер батча
        createModel(1L);
        createModel(2L);
        createModel(3L);

        List<Long> result = performBatch(2);
        Assert.assertEquals(3, result.size());
    }

    @Test
    public void testEmptyBatchUpdate() {
        List<Long> result = performBatch(DEFAULT_BATCH_SIZE);
        Assert.assertEquals(0, result.size());
    }


    private void createModel(long modelId) {
        pgJdbcTemplate.update("insert into ext_models_for_tickets (model_id) values (?)", modelId);
    }

    private List<Long> performBatch(int batchSize) {
        List<Long> result = new ArrayList<>();
        String sql = "select * from ext_models_for_tickets";
        CommonUtils.loadJdbcBatched(
            pgJdbcTemplate, sql, List.of(),
            batchSize,
            (rs, i) -> rs.getLong("model_id"),
            result::addAll
        );

        return result;
    }
}

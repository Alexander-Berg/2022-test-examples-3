package ru.yandex.market.pers.qa.tms.imp;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.pers.qa.PersQaTmsTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author grigor-vlad
 * 14.04.2022
 */
public class ModelsForTicketsYtImportExecutorTest extends PersQaTmsTest {

    @Autowired
    @Qualifier("yqlJdbcTemplate")
    private JdbcTemplate yqlJdbcTemplate;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    @Autowired
    @Qualifier("modelsForTicketsYtImportExecutor")
    private ModelsForTicketsYtImportExecutor executor;

    @Test
    public void testModelsImportFromYt() {
        //add a some models to table
        pgJdbcTemplate.update("insert into qa.ext_models_for_tickets (model_id, vendor_id) values (?, ?)", 11L, 11L);
        pgJdbcTemplate.update("insert into qa.ext_models_for_tickets (model_id, vendor_id) values (?, ?)", 12L, 12L);

        //mock yql request
        List<Pair<Long, Long>> modelForTicketsVendorIdPairList =
            List.of(Pair.of(1L, 2L), Pair.of(3L, 4L), Pair.of(5L, 6L));
        when(yqlJdbcTemplate.query(anyString(), (RowMapper) any())).thenReturn(modelForTicketsVendorIdPairList);

        //execute
        executor.loadModels();

        List<Pair<Long, Long>> modelsForTicketsFromDb = pgJdbcTemplate.query(
            "select model_id, vendor_id from qa.ext_models_for_tickets",
            (rs, rowNum) -> Pair.of(rs.getLong("model_id"), rs.getLong("vendor_id"))
        );
        assertEquals(modelForTicketsVendorIdPairList, modelsForTicketsFromDb);
    }
}

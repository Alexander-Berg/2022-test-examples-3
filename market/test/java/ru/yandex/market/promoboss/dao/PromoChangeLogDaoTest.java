package ru.yandex.market.promoboss.dao;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.model.PromoChangeLogItem;
import ru.yandex.market.promoboss.model.SourceType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = {PromoChangeLogDao.class})
class PromoChangeLogDaoTest extends AbstractDaoTest {
    @Autowired
    private PromoChangeLogDao promoChangeLogDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<PromoChangeLogTestItem> rowMapper = (rs, rowNum) -> PromoChangeLogTestItem.builder()
            .transactionId(rs.getBigDecimal("transaction_id"))
            .updatedAt(rs.getTimestamp("updated_at"))
            .build();

    @Builder
    @AllArgsConstructor
    @ToString
    private static final class PromoChangeLogTestItem {
        private BigDecimal transactionId;
        private Timestamp updatedAt;
    }

    private List<PromoChangeLogTestItem> getAll() {
        return jdbcTemplate.query("select * from promos_change_log order by id", rowMapper);
    }

    @Test
    @DbUnitDataSet(
            after = "PromoChangeLogDaoTest.insert_one_ok.after.csv"
    )
    void insert_one_ok() {
        promoChangeLogDao.insert(PromoChangeLogItem.builder()
                .sourcePromoId("sourcePromoId")
                .requestId("requestId")
                .updatedBy("updatedBy")
                .source(SourceType.CATEGORYIFACE)
                .build());

        List<PromoChangeLogTestItem> actual = getAll();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertNotNull(actual.get(0));
        assertNotNull(actual.get(0).transactionId);
        assertNotNull(actual.get(0).updatedAt);
    }

    @Test
    @DbUnitDataSet(
            after = "PromoChangeLogDaoTest.insert_several_ok.after.csv"
    )
    void insert_several_ok() {
        for (int i = 0; i < 2; i++) {
            jdbcTemplate.execute("BEGIN TRANSACTION");

            promoChangeLogDao.insert(PromoChangeLogItem.builder()
                    .sourcePromoId(String.format("sourcePromoId %d", i))
                    .requestId(String.format("requestId %d", i))
                    .updatedBy(String.format("updatedBy %d", i))
                    .source(SourceType.CATEGORYIFACE)
                    .build());

            jdbcTemplate.execute("COMMIT TRANSACTION");
        }

        List<PromoChangeLogTestItem> actual = getAll();
        assertEquals(2, actual.size());

        PromoChangeLogTestItem first = actual.get(0);
        assertNotNull(first);
        assertNotNull(first.transactionId);
        assertNotNull(first.updatedAt);

        PromoChangeLogTestItem second = actual.get(1);
        assertNotNull(second);
        assertNotNull(second.transactionId);
        assertNotNull(second.updatedAt);

        assertTrue(first.transactionId.compareTo(second.transactionId) < 0);
        assertTrue(first.updatedAt.before(second.updatedAt));
    }
}

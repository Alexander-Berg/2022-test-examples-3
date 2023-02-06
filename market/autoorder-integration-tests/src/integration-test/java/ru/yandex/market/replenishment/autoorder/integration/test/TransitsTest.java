package ru.yandex.market.replenishment.autoorder.integration.test;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

public class TransitsTest extends AbstractIntegrationTest {

    private static final String RECOMMENDATION_QUERY =
            "SELECT r.msku as msku, r.warehouse_id as warehouse_id, d.supplier_id as supplier_id, " +
                    "d.order_date as order_date FROM public.recommendations_1p r " +
                    "inner join public.demand_1p d on r.demand_id = d.id " +
                    "inner join public.transits t on r.msku = t.msku and r.warehouse_id = t.warehouse_id " +
                    "and d.supplier_id = t.supplier_id and d.order_date = t.date " +
                    "Order By random() desc limit 1";

    private static final String TRANSIT_QUERY =
            "SELECT * FROM hahn.`//home/market/production/replenishment/order_planning/%s/outputs/transits` " +
                    "WHERE msku = %d and supplier_id = %d and warehouse_id = %d and `date` = '%s';";

    @Qualifier("jdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Qualifier("yqlJdbcTemplate")
    @Autowired
    private JdbcTemplate yqlJdbcTemplate;

    @Test
    public void test() {
        log.info("Executing query to get random recommendation...");
        final RecommendationDTO recommendationDTO = jdbcTemplate
                .queryForObject(RECOMMENDATION_QUERY, (rs, rowNum) -> new RecommendationDTO(
                        rs.getLong("msku"),
                        rs.getLong("supplier_id"),
                        rs.getLong("warehouse_id"),
                        rs.getString("order_date")));
        log.info("Executed query to get random recommendation");

        if (recommendationDTO == null) {
            log.warn("Random recommendation with transit has not been found");
            return;
        }

        final String yqlQuery = String.format(TRANSIT_QUERY,
                LocalDate.now(),
                recommendationDTO.getMsku(),
                recommendationDTO.getSupplierId(),
                recommendationDTO.getWarehouseId(),
                recommendationDTO.getDate());
        log.info("Executing query to get transit from YT: {}...", yqlQuery);
        final List<String> transits = yqlJdbcTemplate.query(yqlQuery,
                (rs, rowNum) -> rs.wasNull() ? null : rs.getString("in_transit"));

        Assertions.assertNotNull(transits, "Transits not found");
        Assertions.assertFalse(transits.isEmpty(), "Transits not found");
    }

    private static class RecommendationDTO {
        private final long msku;
        private final long supplierId;
        private final long warehouseId;
        private final String date;

        private RecommendationDTO(long msku, long supplierId, long warehouseId, String date) {
            this.msku = msku;
            this.supplierId = supplierId;
            this.warehouseId = warehouseId;
            this.date = date;
        }

        public long getMsku() {
            return msku;
        }

        public long getSupplierId() {
            return supplierId;
        }

        public long getWarehouseId() {
            return warehouseId;
        }

        public String getDate() {
            return date;
        }
    }

}

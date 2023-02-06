package ru.yandex.market.logistics.nesu.jobs;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.YtUpdateDropoffSegmentsInfoExecutor;

import static org.mockito.Mockito.doReturn;

@DatabaseSetup("/jobs/executors/update_dropoff_cargo_types/before/availabilities.xml")
@DisplayName("Обновление карго-типов дропоффов в конфигурациях доступности из YT")
class YtUpdateDropoffSegmentsInfoExecutorTest extends AbstractContextualTest {

    private static final String YQL_REQUEST = ""
        + "$data_with_return_sorting_center_partner_id = "
        + "SELECT "
        + "     segments.logistics_point_lms_id AS logistics_point_id, "
        + "     services.code AS service_code, "
        + "     ListConcat(CAST(Yson::ConvertToDoubleList(services.cargo_types) AS List<String>), ' ')"
        + " AS cargo_types, "
        + "     CAST(Yson::ConvertToString(DictLookup(Yson::LookupDict(`tags`, 'meta'), "
        + "'RETURN_SORTING_CENTER_ID'))          AS Int64 ) AS return_sorting_center_partner_id "
        + "FROM `logistics_segments_path` AS segments "
        + "INNER JOIN `logistics_point_path` AS logistics_point "
        + "    ON segments.logistics_point_lms_id = logistics_point.id "
        + "INNER JOIN `logistics_services_path` AS services "
        + "    ON segments.lms_id = services.segment_lms_id "
        + "WHERE segments.type == 'warehouse' "
        + "    AND segments.partner_type == 'DELIVERY' "
        + "    AND services.status == 'active' "
        + "    AND logistics_point.type == 'PICKUP_POINT' "
        + "    AND logistics_point.active == true"
        + "    AND ("
        + "         cargo_types is not null "
        + "    OR   "
        + "         DictLookup(Yson::LookupDict(`tags`, 'meta'), 'RETURN_SORTING_CENTER_ID') is not null"
        + "    ); "
        + "SELECT "
        + "     return_sorting_center.logistics_point_id AS logistics_point_id, "
        + "     return_sorting_center.service_code AS service_code, "
        + "     return_sorting_center.cargo_types AS cargo_types, "
        + "     return_sorting_center_lp.id AS return_sorting_center_column "
        + "FROM `logistics_point_path` AS return_sorting_center_lp "
        + "INNER JOIN $data_with_return_sorting_center_partner_id as return_sorting_center "
        + "    ON return_sorting_center_lp.partner_id = return_sorting_center.return_sorting_center_partner_id "
        + "WHERE return_sorting_center_lp.active == true "
        + "    AND return_sorting_center_lp.type == 'WAREHOUSE'"
        + " UNION ALL "
        + "SELECT "
        + "     return_sorting_center.logistics_point_id AS logistics_point_id, "
        + "     return_sorting_center.service_code AS service_code, "
        + "     return_sorting_center.cargo_types AS cargo_types, "
        + "     return_sorting_center.return_sorting_center_partner_id AS return_sorting_center_column "
        + "FROM $data_with_return_sorting_center_partner_id as return_sorting_center "
        + "WHERE return_sorting_center.return_sorting_center_partner_id is null;";

    @Autowired
    private JdbcTemplate yqlJdbcTemplate;

    @Autowired
    private YtUpdateDropoffSegmentsInfoExecutor executor;

    @Test
    @DisplayName("Успешное обновление карго-типов")
    @DatabaseSetup(
        value = "/jobs/executors/update_dropoff_cargo_types/before/cargo_types.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/jobs/executors/update_dropoff_cargo_types/after/cargo_type_availabilities.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateCargoTypes() {
        doReturn(
            List.of(
                Map.of(
                    "logistics_point_id", 1000L,
                    "cargo_types", ("101 102 103").getBytes(),
                    "service_code", "PROCESSING"
                ),
                Map.of(
                    "logistics_point_id", 2000L,
                    "cargo_types", ("300").getBytes(),
                    "service_code", "PROCESSING"
                ),
                Map.of(
                    "logistics_point_id", 3000L,
                    "service_code", "PROCESSING"
                ),
                Map.of(
                    "logistics_point_id", 4000L,
                    "cargo_types", ("").getBytes(),
                    "service_code", "PROCESSING"
                )
            )
        ).when(yqlJdbcTemplate).queryForList(YQL_REQUEST);

        executor.doJob(null);
    }

    @Test
    @DisplayName("Успешное обновление возвратных СЦ")
    @DatabaseSetup(
        value = "/jobs/executors/update_dropoff_cargo_types/before/return_sc.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/jobs/executors/update_dropoff_cargo_types/after/return_sc_availabilities.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateReturnSc() {
        doReturn(
            List.of(
                Map.of(
                    "logistics_point_id", 1000L,
                    "service_code", "SHIPMENT",
                    "return_sorting_center_column", "4321"
                ),
                Map.of(
                    "logistics_point_id", 1000L,
                    "service_code", "PROCESSING"
                ),
                Map.of(
                    "logistics_point_id", 2000L,
                    "service_code", "PROCESSING",
                    "return_sorting_center_column", "8765"
                ),
                Map.of(
                    "logistics_point_id", 3000L,
                    "service_code", "SHIPMENT"
                ),
                Map.of(
                    "logistics_point_id", 4000L,
                    "service_code", "SHIPMENT",
                    "return_sorting_center_column", "121314"
                ),
                Map.of(
                    "logistics_point_id", 5000L,
                    "service_code", "SHIPMENT",
                    "return_sorting_center_column", "0"
                )
            )
        ).when(yqlJdbcTemplate).queryForList(YQL_REQUEST);

        executor.doJob(null);
    }
}

package ru.yandex.market.logistics.nesu.jobs;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.YtUpdateDropoffOrderCapacityExecutor;

import static org.mockito.Mockito.doReturn;

@DisplayName("Обновление информации о капасити дропоффов по заказам данными из YT")
@DatabaseSetup("/jobs/executors/update_dropoff_order_capacity/before/availabilities.xml")
public class YtUpdateDropoffOrderCapacityExecutorTest extends AbstractContextualTest {
    private static final String YQL_QUERY = "SELECT dropoff_id, capacity, max_orders FROM `dropoff_capacity`";
    private static final String DROPOFF_ID_COLUMN = "dropoff_id";
    private static final String CAPACITY_COLUMN = "capacity";
    private static final String MAX_ORDERS_COLUMN = "max_orders";

    @Autowired
    private JdbcTemplate yqlJdbcTemplate;

    @Autowired
    private YtUpdateDropoffOrderCapacityExecutor ytUpdateDropoffOrderCapacityExecutor;

    @Test
    @DisplayName("Таблица в YT пуста")
    @ExpectedDatabase(value = "/jobs/executors/update_dropoff_order_capacity/before/availabilities.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void emptyYtTable() {
        doReturn(List.of())
            .when(yqlJdbcTemplate)
            .queryForList(YQL_QUERY);
        ytUpdateDropoffOrderCapacityExecutor.doJob(null);
    }

    @Test
    @DisplayName("Успешное обновление информации о капасити по заказам")
    @ExpectedDatabase(value = "/jobs/executors/update_dropoff_order_capacity/after/success_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void successUpdate() {
        List<Map<String, Object>> yqlResult = List.of(
            Map.of(DROPOFF_ID_COLUMN, 1000L, CAPACITY_COLUMN, 200, MAX_ORDERS_COLUMN, 194),
            Map.of(DROPOFF_ID_COLUMN, 2000L, CAPACITY_COLUMN, 900, MAX_ORDERS_COLUMN, 0),
            Map.of(DROPOFF_ID_COLUMN, 999L, CAPACITY_COLUMN, 600, MAX_ORDERS_COLUMN, 10)
        );
        doReturn(yqlResult)
            .when(yqlJdbcTemplate)
            .queryForList(YQL_QUERY);
        ytUpdateDropoffOrderCapacityExecutor.doJob(null);
    }
}

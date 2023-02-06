package ru.yandex.market.logistics.lom.controller.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.jdbc.core.JdbcTemplate;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.assertJson;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public final class OrderHistoryTestUtil {

    private OrderHistoryTestUtil() {
        throw new UnsupportedOperationException();
    }

    public static void assertOrderHistoryEventCount(JdbcTemplate jdbcTemplate, long orderId, int eventCount) {
        Integer actualCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM order_history_event WHERE order_id = ?",
            new Object[]{orderId},
            Integer.class
        );
        Assertions.assertEquals(eventCount, actualCount, "Неправильное количество изменений истории заказа");
    }

    public static void assertOrderHistoryEvent(
        JdbcTemplate jdbcTemplate,
        long orderId,
        @Nullable BigDecimal yandexUid,
        @Nullable Long serviceId
    ) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT * FROM order_history_event WHERE order_id = ?",
            orderId
        );
        Assertions.assertTrue(list.size() > 0, "Отсутствует история заказа");
        for (Map<String, Object> orderHistoryEvent : list) {
            Assertions.assertEquals(
                yandexUid,
                orderHistoryEvent.get("yandex_uid"),
                "Неправильный uid пользователя изменений истории заказа"
            );
            Assertions.assertEquals(
                serviceId,
                orderHistoryEvent.get("abc_service_id"),
                "Неправильный id сервиса изменений истории заказа"
            );
        }
    }

    static void assertOrderDiff(JdbcTemplate jdbcTemplate, long eventId, String relativePath) throws JSONException {
        assertOrderDiff(jdbcTemplate, eventId, relativePath, JSONCompareMode.STRICT);
    }

    public static void assertOrderDiff(
        JdbcTemplate jdbcTemplate,
        long eventId,
        String relativePath,
        JSONCompareMode mode
    )
        throws JSONException {
        String actual = jdbcTemplate.queryForObject(
            "SELECT diff FROM order_history_event WHERE id = ?",
            new Object[]{eventId},
            String.class
        );
        String expected = extractFileContent(relativePath);
        JSONAssert.assertEquals(expected, actual, mode);
    }

    public static void assertOrderSnapshot(
        JdbcTemplate jdbcTemplate,
        long eventId,
        String contentPath,
        String... ignoringFields
    ) {
        String actual = jdbcTemplate.queryForObject(
            "SELECT snapshot FROM order_history_event WHERE id = ?",
            new Object[]{eventId},
            String.class
        );
        assertJson(contentPath, Objects.requireNonNull(actual), ignoringFields);
    }
}

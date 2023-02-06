package ru.yandex.market.ff.util;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class YqlTablesInPgUtils {

    private YqlTablesInPgUtils() {
        throw new AssertionError();
    }

    public static final void recreateTables(NamedParameterJdbcTemplate yqlJdbcTemplate) {
        yqlJdbcTemplate.getJdbcTemplate().execute("" +
                "CREATE TABLE IF NOT EXISTS tmp_yt_delivery_tracker_delivery_track (" +
                " order_id text, " +
                " track_code text" +
                ")"
        );
        yqlJdbcTemplate.getJdbcTemplate().execute("" +
                "CREATE TABLE IF NOT EXISTS tmp_yt_return (" +
                " id bigint, " +
                " order_id bigint, " +
                " return_delivery_id bigint" +
                ")"
        );
        yqlJdbcTemplate.getJdbcTemplate().execute("" +
                "CREATE TABLE IF NOT EXISTS tmp_yt_return_delivery (" +
                " id bigint, " +
                " delivery_service_id bigint" +
                ")"
        );
        yqlJdbcTemplate.getJdbcTemplate().execute("" +
                "CREATE TABLE IF NOT EXISTS tmp_yt_return_item (" +
                " id bigint, " +
                " order_id bigint, " +
                " return_id bigint, " +
                " item_id bigint, " +
                " count int, " +
                " return_reason text, " +
                " reason_type bigint" +
                ")"
        );

        yqlJdbcTemplate.update("delete from tmp_yt_delivery_tracker_delivery_track", Map.of());
        yqlJdbcTemplate.update("delete from tmp_yt_return", Map.of());
        yqlJdbcTemplate.update("delete from tmp_yt_return_delivery", Map.of());
        yqlJdbcTemplate.update("delete from tmp_yt_return_item", Map.of());
    }

    public static void insertIntoTrack(NamedParameterJdbcTemplate yqlJdbcTemplate, String orderId, String boxId) {
        yqlJdbcTemplate.update("insert into tmp_yt_delivery_tracker_delivery_track (order_id, track_code) " +
                "values (:orderId, :boxId)", Map.of("orderId", orderId, "boxId", boxId));
    }

    public static void insertIntoReturn(NamedParameterJdbcTemplate yqlJdbcTemplate,
                                        long id,
                                        long orderId,
                                        long returnDeliveryId) {
        yqlJdbcTemplate.update("insert into tmp_yt_return (id, order_id, return_delivery_id) " +
                "values (:id, :orderId, :returnDeliveryId)", Map.of("id", id, "orderId", orderId,
                "returnDeliveryId", returnDeliveryId));
    }

    public static void insertIntoReturnDelivery(NamedParameterJdbcTemplate yqlJdbcTemplate,
                                                long id,
                                                long deliveryServiceId) {
        yqlJdbcTemplate.update("insert into tmp_yt_return_delivery (id, delivery_service_id) " +
                "values (:id, :deliveryServiceId)", Map.of("id", id, "deliveryServiceId", deliveryServiceId));
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static void insertIntoReturnItem(NamedParameterJdbcTemplate yqlJdbcTemplate,
                                            long returnItemId,
                                            long orderId,
                                            long returnId,
                                            long itemId,
                                            int count,
                                            String returnReason,
                                            long reasonType) {
        yqlJdbcTemplate.update("insert into tmp_yt_return_item " +
                        "(id, order_id, return_id, item_id, count, return_reason, reason_type) " +
                        "values (:returnItemId, :orderId, :returnId, :itemId, :count, :returnReason, :reasonType)",
                Map.of("returnItemId", returnItemId, "orderId", orderId, "returnId", returnId, "itemId", itemId,
                        "count", count, "returnReason", returnReason, "reasonType", reasonType));
    }
}

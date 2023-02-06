package ru.yandex.market.sc.core.domain.archive.schrodingerbox;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.location.repository.Location;
import ru.yandex.market.sc.core.domain.measurements.repository.Measurements;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;

import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ARCHIVE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_CELL;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDERS;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDER_FF_STATUS_HISTORY;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDER_FF_STATUS_HISTORY_SIMPLE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDER_ITEM;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDER_ITEM_SIMPLE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDER_SCAN_LOG;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDER_TICKET;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDER_TICKET_SIMPLE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDER_UPDATE_HISTORY;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ORDER_UPDATE_HISTORY_SIMPLE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_PLACE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_PLACE_HISTORY;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_PLACE_PARTNER_CODE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_PLACE_SIMPLE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ROUTE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ROUTE_CELL;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ROUTE_FINISH;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ROUTE_FINISH_ORDER;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ROUTE_FINISH_ORDER_SIMPLE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ROUTE_FINISH_PLACE;
import static ru.yandex.market.sc.core.domain.archive.schrodingerbox.ArchivingTestJdbcEntityManager.Inserts.INSERT_INTO_ROUTE_FINISH_PLACE_SIMPLE;

@Service
public class ArchivingTestJdbcEntityManager {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public enum TestEntity {
        CELL,
        ORDERS,
        ORDER_SCAN_LOG,
        ORDER_FF_STATUS_HISTORY,
        ORDER_ITEM,
        ORDER_TICKET,
        ORDER_UPDATE_HISTORY,
        PLACE,
        PLACE_HISTORY,
        PLACE_PARTNER_CODE,
        ROUTE,
        ROUTE_CELL,
        ROUTE_FINISH_ORDER,
        ROUTE_FINISH_PLACE,
        //        SORTABLE, //temporary disabled
        ROUTE_FINISH;


        public static List<TestEntity> orderRelated() {
            return List.of(
                    ORDER_SCAN_LOG,
                    ORDER_FF_STATUS_HISTORY,
                    ORDER_ITEM,
                    ORDER_TICKET,
                    ORDER_UPDATE_HISTORY,
                    PLACE,
                    ROUTE_FINISH_ORDER,
                    ROUTE_FINISH_PLACE
//                    SORTABLE //temporary disabled
            );

        }
    }

    private static final String SELECT_ARCHIVABLE_BY_ID = """
                    SELECT
                        id, archive_id
                    FROM
                        ${table}
                    WHERE
                        id = ${id};
            """;


    public Optional<Map<String, Object>> findRawById(TestEntity testEntity, long recordId) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", recordId);
        data.put("table", testEntity.name());

        String readyInsert = StrSubstitutor.replace(SELECT_ARCHIVABLE_BY_ID, data);

        List<Map<String, Object>> archivables = jdbcTemplate.queryForList(readyInsert);

        return archivables.stream().findFirst();
    }

    public long createScanLog(SortingCenter sortingCenter, String orderExternalId, long daysInPast,
                              @Nullable Long archiveId) {
        Map<String, Object> data = new HashMap<>();
        data.put("days", daysInPast);
        data.put("sortingCenterId", sortingCenter.getId());
        data.put("orderExternalId", orderExternalId);
        data.put("archiveId", archiveId == null ? "null" : archiveId);

        String readyInsert = StrSubstitutor.replace(INSERT_INTO_ORDER_SCAN_LOG, data);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(readyInsert,
                    Statement.RETURN_GENERATED_KEYS);

            return ps;
        }, keyHolder);

        long scanLogId = keyHolder.getKey().longValue();
        return scanLogId;
    }


    public long createRecord(TestEntity relatedEntity,
                             Long sortingCenterId,
                             Long orderId,
                             String orderExternalId,
                             @Nullable Long routeFinishId,
                             @Nullable Long placeId,
                             @Nullable Long archiveId
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("sortingCenterId", sortingCenterId);
        data.put("orderExternalId", orderExternalId);
        data.put("orderId", orderId);
        data.put("routeFinishId", routeFinishId);
        data.put("placeId", placeId);
        data.put("archiveId", archiveId == null ? "null" : archiveId);

        String readyInsert = switch (relatedEntity) {
            case ORDER_SCAN_LOG -> throw new IllegalArgumentException("Unsupported");
            case ORDER_FF_STATUS_HISTORY -> StrSubstitutor.replace(INSERT_INTO_ORDER_FF_STATUS_HISTORY_SIMPLE, data);
            case ORDER_ITEM -> StrSubstitutor.replace(INSERT_INTO_ORDER_ITEM_SIMPLE, data);
            case ORDER_TICKET -> StrSubstitutor.replace(INSERT_INTO_ORDER_TICKET_SIMPLE, data);
            case ORDER_UPDATE_HISTORY -> StrSubstitutor.replace(INSERT_INTO_ORDER_UPDATE_HISTORY_SIMPLE, data);
            case PLACE -> StrSubstitutor.replace(INSERT_INTO_PLACE_SIMPLE, data);
            case ROUTE_FINISH_ORDER -> StrSubstitutor.replace(INSERT_INTO_ROUTE_FINISH_ORDER_SIMPLE, data);
            case ROUTE_FINISH_PLACE -> StrSubstitutor.replace(INSERT_INTO_ROUTE_FINISH_PLACE_SIMPLE, data);
//            case SORTABLE -> StrSubstitutor.replace(INSERT_INTO_SORTABLE_SIMPLE, data);

            default -> throw new RuntimeException();
        };

        System.out.println(readyInsert);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(readyInsert,
                    Statement.RETURN_GENERATED_KEYS);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public long createRecord(TestEntity relatedEntity,
                             Long sortingCenterId,
                             Long orderId,
                             String orderExternalId,
                             @Nullable Long routeFinishId,
                             @Nullable Long placeId,
                             @Nullable Long archiveId,
                             long daysInPast
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("days", daysInPast);
        data.put("sortingCenterId", sortingCenterId);
        data.put("orderExternalId", orderExternalId);
        data.put("orderId", orderId);
        data.put("routeFinishId", routeFinishId);
        data.put("placeId", placeId);
        data.put("archiveId", archiveId == null ? "null" : archiveId);

        String readyInsert = switch (relatedEntity) {
            case ORDERS -> StrSubstitutor.replace(INSERT_INTO_ORDER_SCAN_LOG, data);
            case ORDER_SCAN_LOG -> StrSubstitutor.replace(INSERT_INTO_ORDER_SCAN_LOG, data);
            case ORDER_FF_STATUS_HISTORY -> StrSubstitutor.replace(INSERT_INTO_ORDER_FF_STATUS_HISTORY, data);
            case ORDER_ITEM -> StrSubstitutor.replace(INSERT_INTO_ORDER_ITEM, data);
            case ORDER_TICKET -> StrSubstitutor.replace(INSERT_INTO_ORDER_TICKET, data);
            case ORDER_UPDATE_HISTORY -> StrSubstitutor.replace(INSERT_INTO_ORDER_UPDATE_HISTORY, data);
            case PLACE -> StrSubstitutor.replace(INSERT_INTO_PLACE, data);
            case ROUTE -> StrSubstitutor.replace(INSERT_INTO_ROUTE, data);
            case ROUTE_FINISH_ORDER -> StrSubstitutor.replace(INSERT_INTO_ROUTE_FINISH_ORDER, data);
            case ROUTE_FINISH_PLACE -> StrSubstitutor.replace(INSERT_INTO_ROUTE_FINISH_PLACE, data);
            case ROUTE_FINISH -> StrSubstitutor.replace(INSERT_INTO_ROUTE_FINISH, data);
//            case SORTABLE -> StrSubstitutor.replace(INSERT_INTO_SORTABLE, data);

            default -> throw new RuntimeException();
        };

        System.out.println(readyInsert);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(readyInsert,
                    Statement.RETURN_GENERATED_KEYS);

            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * Версия для Гаргантюа теста
     */
    public long createRecord(TestEntity relatedEntity,
                             EntityCreationContext context,
                             long daysInPast,
                             @Nullable Long archiveId

    ) {
        Map<String, Object> data = new HashMap<>();
        //common fields
        data.put("days", daysInPast);
        data.put("sortingCenterId", context.sortingCenterId);
        data.put("orderExternalId", context.orderExternalId);
        data.put("orderId", context.ids.get(TestEntity.ORDERS) == null ? "null" : context.ids.get(TestEntity.ORDERS));
        data.put("routeId", context.ids.get(TestEntity.ROUTE));
        data.put("routeFinishId", context.ids.get(TestEntity.ROUTE_FINISH));
        data.put("placeId", context.ids.get(TestEntity.PLACE));
        data.put("cellId", context.ids.get(TestEntity.CELL));
        data.put("userId", context.userId);
        data.put("archiveId", archiveId == null ? "null" : archiveId);

        //order fields
        data.put("orderFfStatus", context.orderFfStatus);
        data.put("warehouseId", context.warehouseId);
        data.put("deliveryServiceId", context.deliveryServiceId);
        data.put("locationId", context.locationId);
        data.put("measurementId", context.measurementId);


        String readyInsert = switch (relatedEntity) {
            case CELL -> StrSubstitutor.replace(INSERT_INTO_CELL, data);
            case ORDERS -> StrSubstitutor.replace(INSERT_INTO_ORDERS, data);
            case ORDER_SCAN_LOG -> StrSubstitutor.replace(INSERT_INTO_ORDER_SCAN_LOG, data);
            case ORDER_FF_STATUS_HISTORY -> StrSubstitutor.replace(INSERT_INTO_ORDER_FF_STATUS_HISTORY, data);
            case ORDER_ITEM -> StrSubstitutor.replace(INSERT_INTO_ORDER_ITEM, data);
            case ORDER_TICKET -> StrSubstitutor.replace(INSERT_INTO_ORDER_TICKET, data);
            case ORDER_UPDATE_HISTORY -> StrSubstitutor.replace(INSERT_INTO_ORDER_UPDATE_HISTORY, data);
            case PLACE -> StrSubstitutor.replace(INSERT_INTO_PLACE, data);
            case PLACE_HISTORY -> StrSubstitutor.replace(INSERT_INTO_PLACE_HISTORY, data);
            case PLACE_PARTNER_CODE -> StrSubstitutor.replace(INSERT_INTO_PLACE_PARTNER_CODE, data);
            case ROUTE -> StrSubstitutor.replace(INSERT_INTO_ROUTE, data);
            case ROUTE_CELL -> StrSubstitutor.replace(INSERT_INTO_ROUTE_CELL, data);
            case ROUTE_FINISH -> StrSubstitutor.replace(INSERT_INTO_ROUTE_FINISH, data);
            case ROUTE_FINISH_ORDER -> StrSubstitutor.replace(INSERT_INTO_ROUTE_FINISH_ORDER, data);
            case ROUTE_FINISH_PLACE -> StrSubstitutor.replace(INSERT_INTO_ROUTE_FINISH_PLACE, data);
//            case SORTABLE -> StrSubstitutor.replace(INSERT_INTO_SORTABLE, data);
        };

        System.out.println(readyInsert);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(readyInsert,
                    Statement.RETURN_GENERATED_KEYS);

            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }


    public long createArchive(long id, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("status", status);

        String readyInsert = StrSubstitutor.replace(INSERT_INTO_ARCHIVE, data);

        System.out.println(readyInsert);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(readyInsert,
                    Statement.RETURN_GENERATED_KEYS);

            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public long createOrder(
            SortingCenter sortingCenter,
            String orderExternalId,
            String orderFfStatus,
            Warehouse warehouse,
            DeliveryService deliveryService,
            Location location,
            Measurements measurement,
            long daysInPast,
            @Nullable Long archiveId
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("days", daysInPast);
        data.put("sortingCenterId", sortingCenter.getId());
        data.put("orderExternalId", orderExternalId);
        data.put("orderFfStatus", orderFfStatus);
        data.put("warehouseId", warehouse.getId());
        data.put("deliveryServiceId", deliveryService.getId());
        data.put("locationId", location.getId());
        data.put("measurementId", measurement.getId());
        data.put("archiveId", archiveId == null ? "null" : archiveId);

        String readyInsert = StrSubstitutor.replace(INSERT_INTO_ORDERS, data);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        System.out.println(readyInsert);

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(readyInsert,
                    Statement.RETURN_GENERATED_KEYS);

            return ps;
        }, keyHolder);

        long orderId = keyHolder.getKey().longValue();
        return orderId;
    }


    public long createOrder(EntityCreationContext context, long daysInPast, Long archiveId) {
        Map<String, Object> data = new HashMap<>();
        data.put("days", daysInPast);
        data.put("sortingCenterId", context.sortingCenterId);
        data.put("orderExternalId", context.orderExternalId);
        data.put("orderFfStatus", context.orderFfStatus);
        data.put("warehouseId", context.warehouseId);
        data.put("deliveryServiceId", context.deliveryServiceId);
        data.put("locationId", context.locationId);
        data.put("measurementId", context.measurementId);
        data.put("archiveId", archiveId == null ? "null" : archiveId);

        String readyInsert = StrSubstitutor.replace(INSERT_INTO_ORDERS, data);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(readyInsert,
                    Statement.RETURN_GENERATED_KEYS);

            return ps;
        }, keyHolder);

        long orderId = keyHolder.getKey().longValue();
        return orderId;
    }

    public static class EntityCreationContext {
        Map<TestEntity, Long> ids = new HashMap<>();

        long sortingCenterId;
        @Nullable
        String orderExternalId;
        @Nullable
        String orderFfStatus;
        @Nullable
        Long warehouseId;
        @Nullable
        Long deliveryServiceId;
        @Nullable
        Long locationId;
        @Nullable
        Long measurementId;
        @Nullable
        Long userId;
    }

    static class Inserts {

        static final String INSERT_INTO_ARCHIVE = """
                                INSERT INTO schrodinger_box.archive(id, created_at, updated_at, archive_status)
                                VALUES (${id}, now(), now(), '${status}')
                                RETURNING id;
                """;

        static final String INSERT_INTO_ORDERS = """
                        INSERT INTO ORDERS
                        (
                            created_at, updated_at, sorting_center_id,
                            external_id, ff_status, warehouse_from_id, warehouse_return_id,
                            delivery_service_id, payment_method, delivery_type, location_to_id,
                            measurements_id, cargo_cost, assessed_cost, recipient_name, recipient_phones,
                            total, amount_prepaid, is_damaged,
                            order_history_updated, is_middle_mile, is_package_required, archive_id
                        ) VALUES (
                           now() - ${days} * interval '1 days', now() - ${days} * interval '1 days', ${sortingCenterId},
                           '${orderExternalId}', '${orderFfStatus}', ${warehouseId}, ${warehouseId},
                            ${deliveryServiceId}, 'PREPAID', 'COURIER', ${locationId},
                            ${measurementId}, 18, -5, 'recipient_name', '{"recipient_phones", "consulting"}',
                            '1000', '-5', false,
                            now() - ${days} * interval '1 days', false, false, ${archiveId}
                        ) RETURNING id;
                """;

        static final String INSERT_INTO_ORDER_SCAN_LOG = """
                        INSERT INTO order_scan_log (
                            created_at, updated_at, scanned_at,
                            sorting_center_id, external_order_id,
                            operation, context, result, archive_id
                        ) VALUES (
                            now() - ${days} * interval '1 days',
                            now() - ${days} * interval '1 days',
                            now() - ${days} * interval '1 days',
                            ${sortingCenterId}, '${orderExternalId}',
                             'SCAN', 'SORT', 'OK', ${archiveId}
                        ) RETURNING id;
                """;


        static final String INSERT_INTO_ORDER_FF_STATUS_HISTORY = """
                        INSERT INTO order_ff_status_history (
                            created_at, updated_at,
                            order_update_time,
                            order_id, ff_status, archive_id
                        ) VALUES (
                            now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                            now() - ${days} * interval '1 days',
                            ${orderId}, 'ORDER_CREATED_FF', ${archiveId}
                            ) RETURNING id;
                """;

        static final String INSERT_INTO_ORDER_FF_STATUS_HISTORY_SIMPLE = """
                        INSERT INTO order_ff_status_history (
                            created_at, updated_at, order_update_time,
                            order_id, ff_status, archive_id
                        ) VALUES (
                            now(), now(), now(),
                            ${orderId}, 'ORDER_CREATED_FF', ${archiveId}
                        ) RETURNING id;
                """;

        static final String INSERT_INTO_ORDER_ITEM_SIMPLE = """
                        INSERT INTO order_item (created_at, updated_at, order_id, name, count, price, archive_id)
                        VALUES (now(), now(), ${orderId}, 'name', -10, 20, ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_ORDER_ITEM = """
                        INSERT INTO order_item (created_at, updated_at, order_id, name, count, price, archive_id)
                        VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                             ${orderId}, 'name', -10, 20, ${archiveId})
                        RETURNING id;
                """;


        static final String INSERT_INTO_ORDER_TICKET_SIMPLE = """
                        INSERT INTO order_ticket (created_at, order_id, key, reason, archive_id) VALUES
                        (now(), ${orderId}, 'key-${orderId}', 'reason', ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_ORDER_TICKET = """
                        INSERT INTO order_ticket (created_at, order_id, key, reason, archive_id) VALUES
                        (now() - ${days} * interval '1 days', ${orderId}, 'key-${orderId}', 'reason', ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_ORDER_UPDATE_HISTORY_SIMPLE = """
                        INSERT INTO order_update_history ( created_at, updated_at, order_update_time,
                                                           order_id, event, archive_id)
                        VALUES ( now(), now(), now(), ${orderId}, 'event', ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_ORDER_UPDATE_HISTORY = """
                        INSERT INTO order_update_history ( created_at, updated_at, order_update_time,
                                                           order_id, event, archive_id)
                        VALUES ( now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                        now() - ${days} * interval '1 days', ${orderId}, 'event', ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_PLACE_SIMPLE = """
                        INSERT INTO place ( created_at, updated_at, order_id, yandex_id,
                                            main_partner_code, place_status, archive_id, sorting_center_id,
                                            sortable_flow_stage, is_middle_mile, sortable_status,
                                            utilization_flow)
                        VALUES ( now(), now(), ${orderId}, 'yandex_id', 'partner_code',
                                'SHIPPED',  ${archiveId}, ${sortingCenterId},
                                'STAGE_2_3', false, 'SHIPPED_RETURN', false)
                        RETURNING id;
                """;

        static final String INSERT_INTO_PLACE = """
                        INSERT INTO place ( created_at, updated_at, order_id, yandex_id, main_partner_code,
                                            place_status, archive_id, sorting_center_id, sortable_flow_stage,
                                            is_middle_mile, sortable_status, utilization_flow, stage_id)
                        VALUES ( now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                        ${orderId}, 'yandex_id', 'partner_code', 'SHIPPED',  ${archiveId}, ${sortingCenterId},
                        'STAGE_2_3', false, 'SHIPPED_RETURN', false, 1)
                        RETURNING id;
                """;

        static final String INSERT_INTO_ROUTE_FINISH_ORDER_SIMPLE = """
                        INSERT INTO route_finish_order (created_at, updated_at, route_finish_id,
                                                        order_id, external_order_id, finished_order_ff_status,
                                                        archive_id)
                        VALUES (now(), now(), ${routeFinishId},
                                ${orderId}, '${orderExternalId}', 'finished_order_ff_status', ${archiveId})
                        RETURNING id;
                """;
        static final String INSERT_INTO_ROUTE_FINISH_ORDER = """
                        INSERT INTO route_finish_order (created_at, updated_at, route_finish_id,
                                                        order_id, external_order_id, finished_order_ff_status,
                                                        archive_id)

                        VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days', ${routeFinishId},
                                ${orderId}, '${orderExternalId}', 'finished_order_ff_status', ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_ROUTE_FINISH_PLACE_SIMPLE = """
                        INSERT INTO route_finish_place (created_at, updated_at, route_finish_id, place_id,
                                                        external_place_id, order_id, finished_place_status,
                                                        sortable_status,
                                                        archive_id)
                        VALUES (now(), now(), ${routeFinishId}, ${placeId},
                                'external_place_id', ${orderId}, 'SHIPPED', 'SHIPPED_RETURN', ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_ROUTE_FINISH_PLACE = """
                        INSERT INTO route_finish_place (created_at, updated_at, route_finish_id, place_id,
                                                        external_place_id, order_id, finished_place_status,
                                                        sortable_status, archive_id)
                        VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                                ${routeFinishId}, ${placeId}, 'external_place_id', ${orderId},
                                 'SHIPPED', 'SHIPPED_RETURN', ${archiveId} ) RETURNING id;
                """;


        static final String INSERT_INTO_SORTABLE_SIMPLE = """
                        INSERT INTO sortable (created_at, updated_at, sorting_center_id, barcode, type,
                                              status, order_id, archive_id)
                        VALUES (now(), now(), ${sortingCenterId}, 'barcode-${orderId}', 'type',
                                'status', ${orderId}, ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_SORTABLE = """
                        INSERT INTO sortable (created_at, updated_at, sorting_center_id, barcode,
                                              type, status, order_id, archive_id)
                        VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                         ${sortingCenterId}, 'barcode-${orderId}', 'type', 'status', ${orderId}, ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_ROUTE_FINISH = """
                        INSERT INTO route_finish (
                                created_at, updated_at, finished_at, route_id,  dispatch_person_id, archive_id)
                        VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                                now() - ${days} * interval '1 days', ${routeId}, ${userId}, ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_ROUTE = """
                        INSERT INTO route (created_at, updated_at, expected_date, sorting_center_id, type, archive_id)
                        VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                                (now() - ${days} * interval '1 days')::date, ${sortingCenterId}, 'OUTGOING_COURIER', ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_ROUTE_CELL = """
                        INSERT INTO route_cell (created_at, updated_at, route_id, cell_id, expected_date_sort, archive_id)
                        VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                                ${routeId}, ${cellId}, (now() - ${days} * interval '1 days')::date, ${archiveId})
                        RETURNING id;
                """;

        static final String INSERT_INTO_CELL = """
                        INSERT INTO cell (
                            created_at, updated_at, sorting_center_id, status, type, deleted, subtype, archive_id, is_full)
                        VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                                ${sortingCenterId}, 'NOT_ACTIVE', 'COURIER', false, 'DEFAULT', ${archiveId}, 'false')
                        RETURNING id;
                """;


        static final String INSERT_INTO_PLACE_HISTORY = """
                      INSERT INTO place_history (created_at, updated_at, place_id, place_status, archive_id)
                      VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                              ${placeId}, 'CREATED', ${archiveId})
                      RETURNING id;
                """;

        static final String INSERT_INTO_PLACE_PARTNER_CODE = """
                     INSERT INTO place_partner_code (created_at, updated_at, place_id, partner_id,
                                                     partner_code, archive_id)
                     VALUES (now() - ${days} * interval '1 days', now() - ${days} * interval '1 days',
                             ${placeId},'partner-${placeId}', 'code-${placeId}', ${archiveId})
                     RETURNING id;
                """;

    }
}

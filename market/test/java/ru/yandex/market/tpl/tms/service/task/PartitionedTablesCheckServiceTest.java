package ru.yandex.market.tpl.tms.service.task;

import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.partition.AdditionalIndex;
import ru.yandex.market.tpl.core.domain.partition.PartitionedTable;
import ru.yandex.market.tpl.core.domain.partition.PartitionedTablesRepository;
import ru.yandex.market.tpl.core.domain.partition.StorageUnit;
import ru.yandex.market.tpl.tms.service.task.partition.CreatePartitionService;
import ru.yandex.market.tpl.tms.service.task.partition.DeletePartitionService;
import ru.yandex.market.tpl.tms.service.task.partition.PartitionedTableService;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor
@DirtiesContext
class PartitionedTablesCheckServiceTest extends TplTmsAbstractTest {
    private static final String sqlCreationTime = "creation";
    private final DeletePartitionService deletePartitionService;
    private final CreatePartitionService createPartitionService;
    private final PartitionedTableService partitionedTableService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PartitionedTablesRepository partitionedTablesRepository;
    private final Clock clock;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String sqlPartitionedTableName = "partitionedTableName";

    private final String sqlCreateArchivePartition = "CREATE TABLE IF NOT EXISTS :" + sqlPartitionedTableName +
            "_archive PARTITION OF :" + sqlPartitionedTableName + " DEFAULT";

    private final String sqlTruncateTale = "TRUNCATE :" + sqlPartitionedTableName + " CASCADE;";

    private static Stream<Arguments> getPartitionedTables() {

        PartitionedTable taskStatusHistoryEventTable = new PartitionedTable();
        taskStatusHistoryEventTable.setName("task_status_history_event");
        taskStatusHistoryEventTable.setStorageUnitCount(1);
        taskStatusHistoryEventTable.setDeleteOldPartition(true);
        taskStatusHistoryEventTable.setStorageUnit(StorageUnit.MONTH);
        String insertSqlTaskStatusHistoryEvent = "insert into task_status_history_event(id, task_id, timestamp, " +
                "status_from, status_to, source) VALUES (2430219, 1, ':" + sqlCreationTime + "', 'NOT STARTED', 'NOT " +
                "STARTED', null);";

        PartitionedTable orderHistoryEventTable = new PartitionedTable();
        orderHistoryEventTable.setName("order_history_event");
        orderHistoryEventTable.setStorageUnitCount(3);
        orderHistoryEventTable.setDeleteOldPartition(true);
        orderHistoryEventTable.setStorageUnit(StorageUnit.MONTH);
        String insertSqlOrder = "INSERT INTO orders " +
                "(id, external_order_id, payment_type, payment_status, created_at, updated_at, " +
                "delivery_status, delivery_service_id, order_flow_status, order_flow_status_updated_at, " +
                "pickup, sender_id, warehouse_id, weight, length, width, height, warehouse_return_id, " +
                "ds_api_checkpoint, pickup_point_id, buyer_yandex_uid) " +
                "VALUES (1, 34539, 'PREPAID', 'PAID', now(), " +
                "now(), 'NOT_DELIVERED', -1, 'SORTING_CENTER_ARRIVED', " +
                "now(), true, null, null, 1.2, 10, 20, 30,null,30, null, null); ";
        String insertSqlOrderHistoryEvent = "insert into order_history_event(id, order_id, date, context, source, " +
                "type, created_at, updated_at) VALUES (1434643, 1, ':" + sqlCreationTime + "', 'context', 'TEST', " +
                "'CREATED', ':" + sqlCreationTime + "', ':" + sqlCreationTime + "')";

        PartitionedTable userLocationTable = new PartitionedTable();
        userLocationTable.setName("user_location");
        userLocationTable.setStorageUnitCount(1);
        userLocationTable.setDeleteOldPartition(true);
        userLocationTable.setStorageUnit(StorageUnit.MONTH);
        String sqlDropNotNullUserLocationUserId = "ALTER TABLE user_location ALTER COLUMN user_id DROP NOT NULL; ";
        String insertSqlUserLocation = "INSERT INTO user_location (longitude, latitude, device_id, " +
                "route_point_id, route_point_status_after, created_at, updated_at, posted_at) VALUES " +
                "(39.8271984, 47.1046228, null, null, null, ':" + sqlCreationTime + "', '2021-02-18" +
                " 23:41:26.177220', '2021-02-18 23:41:26.177288')";

        AdditionalIndex additionalIndex = new AdditionalIndex();
        additionalIndex.setUnique(true);
        additionalIndex.setColumns(List.of("user_id", "posted_at"));
        additionalIndex.setTable(userLocationTable);
        userLocationTable.setAdditionalIndices(List.of(additionalIndex));

        PartitionedTable partnerkaCommandLogTable = new PartitionedTable();
        partnerkaCommandLogTable.setName("partnerka_command_log");
        partnerkaCommandLogTable.setStorageUnitCount(1);
        partnerkaCommandLogTable.setDeleteOldPartition(true);
        partnerkaCommandLogTable.setStorageUnit(StorageUnit.MONTH);
        String insertSqlPartnerkaCommandLog = "insert into partnerka_command_log (name, context, created_at, updated_at) " +
                "values ('TEST', 'TEST', ':" + sqlCreationTime + "', ':" + sqlCreationTime + "');";

        PartitionedTable eventLogTable = new PartitionedTable();
        eventLogTable.setName("event_log");
        eventLogTable.setStorageUnitCount(7);
        eventLogTable.setDeleteOldPartition(true);
        eventLogTable.setStorageUnit(StorageUnit.DAY);
        String insertSqlEventLog = "insert into event_log(id, event_type, event_date, " +
                "entity, entity_id, created_at, rollback) " +
                "values ('8643e09d-15e8-47aa-8d90-08164d42d343', 'CmdOk', '2022-02-08 21:50:36.315000 +00:00', " +
                "'Order', 41584039, ':" + sqlCreationTime + "', false)";

        PartitionedTable orderFlowStatusHistoryTable = new PartitionedTable();
        orderFlowStatusHistoryTable.setName("order_flow_status_history");
        orderFlowStatusHistoryTable.setStorageUnitCount(3);
        orderFlowStatusHistoryTable.setDeleteOldPartition(true);
        orderFlowStatusHistoryTable.setStorageUnit(StorageUnit.MONTH);
        String sqlDropNotNullOrderFlowStatusHistoryOrderId = "ALTER TABLE order_flow_status_history ALTER COLUMN " +
                "order_id DROP NOT NULL; ";
        String insertSqlOrderFlowStatusHistory = "insert into order_flow_status_history" +
                "(external_order_id, order_flow_status_before, order_flow_status_after, order_flow_status_updated_at," +
                " delivery_service_id, ds_api_checkpoint) values (79646592, 'SORTING_CENTER_CREATED', " +
                "'SORTING_CENTER_ARRIVED', ':" + sqlCreationTime + "', 1005429, 10);";

        PartitionedTable movementHistoryEventTable = new PartitionedTable();
        movementHistoryEventTable.setName("movement_history_event");
        movementHistoryEventTable.setStorageUnitCount(3);
        movementHistoryEventTable.setDeleteOldPartition(true);
        movementHistoryEventTable.setStorageUnit(StorageUnit.MONTH);
        String sqlDropNotNullMovementHistoryEventMovementId = "ALTER TABLE movement_history_event ALTER COLUMN movement_id DROP NOT NULL; ";
        String insertSqlMovementHistoryEvent =
                "insert into movement_history_event(date, source, type, created_at, updated_at) VALUES ('2020-10-23 18:00:11.183000 +00:00', 'DELIVERY', 'MOVEMENT_CREATED', ':" + sqlCreationTime + "', '2020-10-23 18:00:11.201358 +00:00');";

        PartitionedTable clientReturnHistoryEventTable = new PartitionedTable();
        clientReturnHistoryEventTable.setName("client_return_history_event");
        clientReturnHistoryEventTable.setStorageUnitCount(3);
        clientReturnHistoryEventTable.setDeleteOldPartition(true);
        clientReturnHistoryEventTable.setStorageUnit(StorageUnit.MONTH);
        String sqlDropNotNullClientReturnHistoryEventClientReturnId = "ALTER TABLE client_return_history_event ALTER " +
                "COLUMN client_return_id DROP NOT NULL; ";
        String insertSqlClientReturnHistoryEvent = "insert into client_return_history_event" +
                "(date, source, type, context, created_at, updated_at) values ('2021-04-07 17:26:48.870000 " +
                "+00:00', 'COURIER', 'CLIENT_RETURN_CREATED', 'Клиентский возврат создан', ':" + sqlCreationTime +
                "', '2022-08-07 17:26:48.890695 +00:00')";

        PartitionedTable routePointPostponementHistoryEventTable = new PartitionedTable();
        routePointPostponementHistoryEventTable.setName("route_point_postponement_history_event");
        routePointPostponementHistoryEventTable.setStorageUnitCount(3);
        routePointPostponementHistoryEventTable.setDeleteOldPartition(true);
        routePointPostponementHistoryEventTable.setStorageUnit(StorageUnit.MONTH);
        String sqlRoutePointPostponementHistoryEventUserShiftIdAndRoutePointId = "ALTER TABLE " +
                "route_point_postponement_history_event ALTER COLUMN " +
                "user_shift_id DROP NOT NULL; ALTER TABLE route_point_postponement_history_event ALTER COLUMN " +
                "route_point_id DROP NOT NULL;";
        String insertSqlRoutePointPostponementHistoryEvent = "insert into " +
                "route_point_postponement_history_event(active, duration, created_at, updated_at) values (false, '0 " +
                "years 0 mons 0 days 3 hours 0 mins 0.0 secs', ':" + sqlCreationTime + "', ':" + sqlCreationTime +
                "');";

        PartitionedTable scOrderHistoryTable = new PartitionedTable();
        scOrderHistoryTable.setName("sc_order_history");
        scOrderHistoryTable.setStorageUnitCount(3);
        scOrderHistoryTable.setDeleteOldPartition(true);
        scOrderHistoryTable.setStorageUnit(StorageUnit.MONTH);
        String sqlDropNotNullScOrderHistoryTableOrderId = "ALTER TABLE sc_order_history ALTER COLUMN order_id DROP NOT NULL; ";
        String insertSqlScOrderHistoryTable =
                "insert into sc_order_history(yandex_id, partner_id, status, status_updated_at, created_at, updated_at) values(10387039, 47951, 'ORDER_CREATED_BUT_NOT_APPROVED_FF', '2019-11-15 16:45:53.032450 +00:00', ':" + sqlCreationTime + "', '2019-11-15 16:45:53.032450 +00:00');";

        PartitionedTable courseHistoryTable = new PartitionedTable();
        courseHistoryTable.setName("course_history_event");
        courseHistoryTable.setStorageUnitCount(3);
        courseHistoryTable.setDeleteOldPartition(true);
        courseHistoryTable.setStorageUnit(StorageUnit.MONTH);
        String sqlDropNotNullCourseHistoryTableCourseId = "ALTER TABLE course_history_event ALTER COLUMN course_id DROP NOT NULL; ";
        String insertSqlCourseHistory = "insert into course_history_event(date, " +
                "entity_type, entity_id, event_type) values(':" + sqlCreationTime + "', 'CourseAssignment', " +
                "11166, 'ASSIGNMENT_CREATED');";

        PartitionedTable queueLogTable = new PartitionedTable();
        queueLogTable.setName("queue_log");
        queueLogTable.setStorageUnitCount(7);
        queueLogTable.setDeleteOldPartition(true);
        queueLogTable.setStorageUnit(StorageUnit.DAY);
        String insertSqlQueueLog =
                "insert into queue_log(queue_name, event, task_id, created_at, updated_at) values ('TEST', 'TEST', 1," +
                        " ':" + sqlCreationTime + "', ':" + sqlCreationTime + "');";

        return Stream.of(
                Arguments.of(taskStatusHistoryEventTable, insertSqlTaskStatusHistoryEvent),
                Arguments.of(orderHistoryEventTable, insertSqlOrder + insertSqlOrderHistoryEvent),
                Arguments.of(userLocationTable, sqlDropNotNullUserLocationUserId + insertSqlUserLocation),
                Arguments.of(partnerkaCommandLogTable, insertSqlPartnerkaCommandLog),
                Arguments.of(eventLogTable, insertSqlEventLog),
                Arguments.of(orderFlowStatusHistoryTable, sqlDropNotNullOrderFlowStatusHistoryOrderId
                        + insertSqlOrderFlowStatusHistory),
                Arguments.of(movementHistoryEventTable, sqlDropNotNullMovementHistoryEventMovementId + insertSqlMovementHistoryEvent),
                Arguments.of(clientReturnHistoryEventTable, sqlDropNotNullClientReturnHistoryEventClientReturnId + insertSqlClientReturnHistoryEvent),
                Arguments.of(queueLogTable, insertSqlQueueLog),
                Arguments.of(routePointPostponementHistoryEventTable,
                        sqlRoutePointPostponementHistoryEventUserShiftIdAndRoutePointId + insertSqlRoutePointPostponementHistoryEvent),
                Arguments.of(scOrderHistoryTable, sqlDropNotNullScOrderHistoryTableOrderId + insertSqlScOrderHistoryTable),
                Arguments.of(courseHistoryTable, sqlDropNotNullCourseHistoryTableCourseId + insertSqlCourseHistory)
        );
    }

    @ParameterizedTest(name = "Проверка автоматического создания партиций на следующую единицу партицирования ддя " +
            "таблицы: {0}")
    @MethodSource("getPartitionedTables")
    void checkAndModifyPartitions_createsRequired(PartitionedTable partitionedTable) {
        List<String> existedPartitionsNames = partitionedTableService.getExistedPartitions(partitionedTable);
        existedPartitionsNames.forEach(this::dropTable);

        truncateTable("partitioned_tables");
        partitionedTablesRepository.save(partitionedTable);

        createPartitionService.createNewPartition();
        deletePartitionService.deleteOldPartition();

        assertExistsOnlyRequiredPartitions(partitionedTable);

        createArchivePartition(partitionedTable);
    }

    @ParameterizedTest(name = "Проверка автоматического удаления партиций за пределами заданного диапозона для " +
            "таблицы: {0}")
    @MethodSource("getPartitionedTables")
    void checkAndModifyPartitions_removesRedundant(PartitionedTable partitionedTable) {
        List<String> existedPartitionsNames = partitionedTableService.getExistedPartitions(partitionedTable);
        existedPartitionsNames.forEach(this::dropTable);

        truncateTable("partitioned_tables");
        partitionedTablesRepository.save(partitionedTable);

        var today = LocalDateTime.now(clock);
        String partitionedTableName = partitionedTable.getName();
        switch (partitionedTable.getStorageUnit()) {
            case MONTH: {
                LocalDateTime partitionDateBefore = today.minusMonths(partitionedTable.getStorageUnitCount() + 1);
                createPartition(getPartitionNameForMonth(partitionedTableName, partitionDateBefore), partitionedTable,
                        partitionDateBefore);
                LocalDateTime partitionDateAfter = today.plusMonths(2);
                createPartition(getPartitionNameForMonth(partitionedTableName, partitionDateAfter), partitionedTable,
                        partitionDateAfter);
                break;
            }
            case DAY: {
                LocalDateTime partitionDateBefore = today.minusDays(partitionedTable.getStorageUnitCount() + 1);
                createPartition(getPartitionNameForDay(partitionedTableName, partitionDateBefore), partitionedTable,
                        partitionDateBefore);
                LocalDateTime partitionDateAfter = today.plusDays(2);
                createPartition(getPartitionNameForDay(partitionedTableName, partitionDateAfter), partitionedTable,
                        partitionDateAfter);
                break;
            }
            default:
                throw new TplInvalidParameterException("Ошибка");
        }

        createPartitionService.createNewPartition();
        deletePartitionService.deleteOldPartition();

        assertExistsOnlyRequiredPartitions(partitionedTable);

        createArchivePartition(partitionedTable);
    }

    @ParameterizedTest(name = "Проверка вставки данных в партиции для таблицы: {0}")
    @MethodSource("getPartitionedTables")
    void checkAndModifyPartitions_checkRanges(PartitionedTable partitionedTable, String insertSql) {
        List<String> existedPartitionsNames = partitionedTableService.getExistedPartitions(partitionedTable);
        existedPartitionsNames.forEach(this::dropTable);

        truncateTable("partitioned_tables");
        partitionedTablesRepository.save(partitionedTable);

        LocalDateTime today = LocalDateTime.now(clock);
        createPartitionService.createNewPartition();
        deletePartitionService.deleteOldPartition();

        String sqlInsertGood;
        String sqlInsertBadPrevious;
        String sqlInsertBadFuture;
        switch (partitionedTable.getStorageUnit()) {
            case DAY: {
                sqlInsertGood = insertSql.replaceAll(":" + sqlCreationTime, dateFormatter.format(today.plusDays(1)));
                sqlInsertBadPrevious = insertSql.replaceAll(":" + sqlCreationTime,
                        dateFormatter.format(today.minusDays(partitionedTable.getStorageUnitCount() + 1)));
                sqlInsertBadFuture = insertSql.replaceAll(":" + sqlCreationTime,
                        dateFormatter.format(today.plusDays(PartitionedTableService.COUNT_OF_NEXT_DAY_PARTITIONS + 1)));
                break;
            }
            case MONTH: {
                sqlInsertGood = insertSql.replaceAll(":" + sqlCreationTime, dateFormatter.format(today.plusMonths(1)));
                sqlInsertBadPrevious = insertSql.replaceAll(":" + sqlCreationTime,
                        dateFormatter.format(today.minusMonths(partitionedTable.getStorageUnitCount() + 1)));
                sqlInsertBadFuture = insertSql.replaceAll(":" + sqlCreationTime,
                        dateFormatter.format(today.plusMonths(2)));
                break;
            }
            default:
                throw new TplInvalidParameterException("Ошибка");
        }

        System.out.println(sqlInsertGood);
        jdbcTemplate.execute(sqlInsertGood, PreparedStatement::execute);

        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute(sqlInsertBadFuture, PreparedStatement::execute);
        });
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute(sqlInsertBadPrevious, PreparedStatement::execute);
        });

        createArchivePartition(partitionedTable);
    }

    private void assertExistsOnlyRequiredPartitions(PartitionedTable partitionedTable) {
        List<String> resultExistedPartitionsNames = partitionedTableService.getExistedPartitions(partitionedTable);
        int requiredCountOfPartitions = partitionedTable.getStorageUnit() == StorageUnit.MONTH ? 1 :
                PartitionedTableService.COUNT_OF_NEXT_DAY_PARTITIONS;

        assertThat(resultExistedPartitionsNames.size()).isEqualTo(requiredCountOfPartitions);

        List<String> requiredPartitionsNames = getRequiredPartitionsNames(partitionedTable);
        assertThat(
                resultExistedPartitionsNames.stream()
                        .filter(requiredPartitionsNames::contains)
                        .count()
        ).isEqualTo((long) requiredCountOfPartitions);
    }

    private void createPartition(
            String partitionName,
            PartitionedTable partitionedTable,
            LocalDateTime from
    ) {
        String tableName = partitionedTable.getName();
        LocalDateTime to;
        switch (partitionedTable.getStorageUnit()) {
            case MONTH:
                from = LocalDateTime.of(
                        from.getYear(), from.getMonthValue(), 1, 0, 0, 0
                );
                to = from.plusMonths(1);
                break;
            case DAY:
                from = LocalDateTime.of(
                        from.getYear(), from.getMonthValue(), from.getDayOfMonth(), 0, 0, 0
                );
                to = from.plusDays(1);
                break;
            default:
                throw new TplInvalidParameterException("Ошибка");
        }
        String sql = "" +
                "CREATE TABLE IF NOT EXISTS " + partitionName + " " +
                "PARTITION OF " + tableName + " " +
                "FOR VALUES FROM ('" + dateFormatter.format(from) + "') " +
                "TO ('" + dateFormatter.format(to) + "');";
        jdbcTemplate.execute(sql, PreparedStatement::execute);
    }

    private void dropTable(String table) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + table + ";", PreparedStatement::execute);
    }

    private void createArchivePartition(PartitionedTable partitionedTable) {
        String realSqlCreateArchivePartition = sqlCreateArchivePartition.replaceAll(":" + sqlPartitionedTableName,
                partitionedTable.getName());
        jdbcTemplate.execute(realSqlCreateArchivePartition, PreparedStatement::execute);
    }

    private void truncateTable(String tableName) {
        String realSqlTruncateTable = sqlTruncateTale.replaceAll(":" + sqlPartitionedTableName,
                tableName);
        jdbcTemplate.execute(realSqlTruncateTable, PreparedStatement::execute);
    }

    private List<String> getRequiredPartitionsNames(PartitionedTable partitionedTable) {
        var todayDate = LocalDateTime.now(clock);
        switch (partitionedTable.getStorageUnit()) {
            case DAY: {
                List<String> result = new ArrayList<>();
                for (int i = 1; i <= PartitionedTableService.COUNT_OF_NEXT_DAY_PARTITIONS; i++) {
                    LocalDateTime dateNext = todayDate.plusDays(i);
                    result.add(getPartitionNameForDay(partitionedTable.getName(), dateNext));
                }
                return result;
            }
            case MONTH: {
                LocalDateTime dateNext = todayDate.plusMonths(1);
                return List.of(getPartitionNameForMonth(partitionedTable.getName(), dateNext));
            }
            default:
                throw new TplInvalidParameterException("Ошибка");
        }
    }

    private String getPartitionNameForMonth(String partitionedTableName, LocalDateTime date) {
        return partitionedTableName + "_" +
                date.getYear() + "_" + String.format("%02d", date.getMonthValue());
    }

    private String getPartitionNameForDay(String partitionedTableName, LocalDateTime date) {
        return partitionedTableName + "_" +
                date.getYear() + "_" + String.format("%02d", date.getMonthValue()) + "_" + String.format("%02d",
                date.getDayOfMonth());
    }
}

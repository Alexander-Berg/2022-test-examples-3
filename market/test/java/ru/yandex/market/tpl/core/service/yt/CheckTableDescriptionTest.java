package ru.yandex.market.tpl.core.service.yt;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.order.OrderChequeStatus;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.VatType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.LeavingAtReceptionStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
@Slf4j
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CheckTableDescriptionTest {

    private static final Set<String> TABLES = Set.of(
            "order_cheque",
            "order_delivery",
            "order_flow_status_history",
            "order_item",
            "orders",
            "route_point",
            "sc_order",
            "sc_order_history",
            "shift",
            "task",
            "task_call_log",
            "task_call_to_recipient",
            "task_order_delivery",
            "task_order_pickup",
            "task_order_return",
            "user_shift",
            "users",
            "routing_log",
            "company",
            "sort_center_company_mapping",
            "order_places",
            "order_place_items",
            "movement",
            "task_locker_delivery",
            "subtask_locker_delivery",
            "tariff"
    );
    private static final Map<String, String> DICT_2_PATH = StreamEx.of(OrderFlowStatus.class,
            UserShiftStatus.class,
            VatType.class,
            ShiftStatus.class,
            OrderPaymentType.class,
            OrderDeliveryStatus.class,
            OrderStatusType.class,
            LeavingAtReceptionStatus.class,
            OrderPaymentStatus.class,
            RoutePointStatus.class,
            OrderChequeType.class,
            OrderChequeStatus.class,
            CourierVehicleType.class,
            OrderDeliveryTaskStatus.class,
            CallToRecipientTaskStatus.class
    ).toMap(Class::getSimpleName, CheckTableDescriptionTest::getPath);

    private static final Set<String> EXCLUDE_COLUMNS = Set.of("id", "created_at", "updated_at",
            "cmax", "xmax", "cmin", "xmin", "ctid", "name", "tableoid");

    private final JdbcTemplate jdbcTemplate;

    private static String getPath(Class<? extends Enum<? extends Enum<?>>> c) {
        URL resource = c.getResource(c.getSimpleName() + ".class");
        if (resource == null) {
            return "";
        }
        return resource.toString().replaceFirst(".*/production/(.*)/(ru/.*).class",
                "https://a.yandex-team.ru/arc/trunk/arcadia/market/market-tpl/main/$1/src/main/java/$2.java");
    }

    @Test
    void checkTableComments() {
        List<ColumnDescription> columns = jdbcTemplate.query("" +
                        "SELECT pn.nspname                             AS schemaName, " +
                        "       pc.relname                             AS tableName, " +
                        "       pa.attname                             AS columnName, " +
                        "       format_type(pa.atttypid, pa.atttypmod) AS columnType, " +
                        "       obj_description(pc.oid)                AS tableComment, " +
                        "       col_description(pc.oid, pa.attnum)     AS columnComment " +
                        "FROM pg_class pc " +
                        "     JOIN pg_attribute pa ON pa.attrelid = pc.oid " +
                        "     JOIN pg_namespace pn ON pn.oid = pc.relnamespace " +
                        "WHERE pc.relname IN (" +
                        TABLES.stream().map(t -> "'" + t + "'").collect(Collectors.joining(", ")) +
                        ") " +
                        "ORDER BY relname, attnum",
                BeanPropertyRowMapper.newInstance(ColumnDescription.class)
        );

        printWiki(columns);
        assertThat(columns.stream()
                .filter(c -> StringUtils.isEmpty(c.getTableComment()))
                .map(ColumnDescription::getTableName)
                .distinct()
        ).describedAs("Все таблицы для выгрузок должны иметь комментарии").isEmpty();

        assertThat(columns.stream()
                .filter(c -> StringUtils.isEmpty(c.getColumnComment()))
                .filter(c -> !EXCLUDE_COLUMNS.contains(c.getColumnName()))
                .filter(c -> !c.getColumnName().contains("pg.dropped"))
                .map(c -> c.getTableName() + "." + c.getColumnName())
        ).describedAs("Все колонки таблиц для выгрузок должны иметь комментарии").isEmpty();
    }

    /**
     * https://wiki.yandex-team.ru/Market/3pl/development/backend/analytics/tables/desc/
     */
    private void printWiki(List<ColumnDescription> columns) {
        Map<String, List<ColumnDescription>> tables =
                EntryStream.of(columns.stream().collect(Collectors.groupingBy(ColumnDescription::getTableName)))
                        .toSortedMap();
        for (String table : tables.keySet()) {
            List<ColumnDescription> columnsForTable = tables.get(table);

            System.out.println("===" + table);
            System.out.println(columnsForTable.stream()
                    .filter(c -> !EXCLUDE_COLUMNS.contains(c.getColumnName()))
                    .map(c -> String.format("|| %s | %s | %s||",
                            c.getColumnName(),
                            c.getColumnType(),
                            addLinkToEnum(c.getColumnComment())))
                    .collect(Collectors.joining("\n", "#|\n", "\n|#"))
            );
        }
    }

    private String addLinkToEnum(String columnComment) {
        if (columnComment == null) {
            return "";
        }
        if (!columnComment.contains("справочник ")) {
            return columnComment;
        }
        int index = columnComment.indexOf("справочник ");
        if (index == -1) {
            return columnComment;
        }
        String className = columnComment.substring(index + "справочник ".length());
        String link = DICT_2_PATH.get(className);
        if (link == null) {
            return columnComment;
        }
        return columnComment + " ((" + link + " link))";
    }

    @Data
    static class ColumnDescription {

        String schemaName;
        String tableName;
        String columnName;
        String columnType;
        String tableComment;
        String columnComment;

    }

}

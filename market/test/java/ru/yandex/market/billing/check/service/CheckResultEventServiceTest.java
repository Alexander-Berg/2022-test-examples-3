package ru.yandex.market.billing.check.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.juggler2.Status;
import ru.yandex.inside.juggler2.event.JugglerEvent;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.billing.check.CheckResultYtDao;
import ru.yandex.market.billing.check.model.CheckResultItem;
import ru.yandex.market.billing.check.model.CheckResultType;
import ru.yandex.market.billing.check.model.MismatchType;
import ru.yandex.market.billing.monitor.model.MonitorJobsGroups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree.stringNode;

/**
 * Тест для {@link CheckResultEventService}.
 */
class CheckResultEventServiceTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2021, 12, 7);

    private static final Clock TEST_NIGHT_CLOCK = Clock.fixed(
            LocalDateTime.of(TEST_DATE, LocalTime.of(3, 14)).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    private static final Clock TEST_DAYLIGHT_CLOCK = Clock.fixed(
            LocalDateTime.of(TEST_DATE, LocalTime.of(15, 28)).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String TEST_JUGGLER_HOST_NAME = "market-billing-test";

    private final Yt yt = mock(Yt.class);

    private final YtTables tables = mock(YtTables.class);

    private final Cypress cypress = mock(Cypress.class);

    private CheckResultService checkResultService;

    private static YTreeMapNode toYTreeMapNode(CheckResultItem checkResultItem) {
        var node = new YTreeMapNodeImpl(new HashMap<>());
        node.put("first_component_name", stringNode(checkResultItem.getFirstComponentName()));
        node.put("first_component_value", stringNode(checkResultItem.getFirstComponentValue()));
        node.put("second_component_name", stringNode(checkResultItem.getSecondComponentName()));
        node.put("second_component_value", stringNode(checkResultItem.getSecondComponentValue()));
        node.put("mismatch_field_name", stringNode(checkResultItem.getMismatchFieldName()));
        node.put("mismatch_type", stringNode(checkResultItem.getMismatchType().getId()));
        node.put("mismatch_value", stringNode(checkResultItem.getMismatchValue()));
        node.put("primary_key", stringNode(checkResultItem.getPrimaryKey()));
        node.put("sort_parameters", stringNode(checkResultItem.getSortParameters()));
        return node;
    }

    private static List<YTreeMapNode> getCheckResultRows() {
        return Stream.of(
                        CheckResultItem.builder()
                                .setFirstComponentName("Checkouter")
                                .setFirstComponentValue("1310")
                                .setSecondComponentName("MBI-billing")
                                .setSecondComponentValue(null)
                                .setMismatchFieldName("payout_amount")
                                .setMismatchType(MismatchType.SECOND_ABSENT)
                                .setMismatchValue("1310")
                                .setPrimaryKey(
                                        "" +
                                                "{\n" +
                                                "    \"order_id\": \"61537687\",\n" +
                                                "    \"payment_id\": \"72208014\"\n" +
                                                "}"
                                )
                                .setSortParameters(
                                        "" +
                                                "{\n" +
                                                "    \"order_id\": \"61537687\",\n" +
                                                "    \"payment_goal\": \"ORDER_PREPAY\",\n" +
                                                "    \"payment_id\": \"72208014\"\n" +
                                                "}"
                                )
                                .build(),
                        CheckResultItem.builder()
                                .setFirstComponentName("Checkouter")
                                .setFirstComponentValue("779")
                                .setSecondComponentName("MBI-billing")
                                .setSecondComponentValue(null)
                                .setMismatchFieldName("payout_amount")
                                .setMismatchType(MismatchType.SECOND_ABSENT)
                                .setMismatchValue("779")
                                .setPrimaryKey(
                                        "" +
                                                "{\n" +
                                                "    \"order_id\": \"60970522\",\n" +
                                                "    \"payment_id\": \"72385254\"\n" +
                                                "}"
                                )
                                .setSortParameters(
                                        "" +
                                                "{\n" +
                                                "    \"order_id\": \"60970522\",\n" +
                                                "    \"payment_goal\": \"SUBSIDY\",\n" +
                                                "    \"payment_id\": \"72385254\"\n" +
                                                "}"
                                )
                                .build(),
                        CheckResultItem.builder()
                                .setFirstComponentName("Checkouter")
                                .setFirstComponentValue("1129")
                                .setSecondComponentName("MBI-billing")
                                .setSecondComponentValue("924")
                                .setMismatchFieldName("payout_amount")
                                .setMismatchType(MismatchType.VALUE_MISMATCH)
                                .setMismatchValue("205")
                                .setPrimaryKey(
                                        "" +
                                                "{\n" +
                                                "    \"order_id\": \"60211876\",\n" +
                                                "    \"payment_id\": \"72388458\"\n" +
                                                "}"
                                )
                                .setSortParameters(
                                        "" +
                                                "{\n" +
                                                "    \"order_id\": \"60211876\",\n" +
                                                "    \"payment_goal\": \"ORDER_POSTPAY\",\n" +
                                                "    \"payment_id\": \"72388458\"\n" +
                                                "}"
                                )
                                .build()
                )
                .map(CheckResultEventServiceTest::toYTreeMapNode)
                .collect(Collectors.toList());
    }

    private static JugglerEvent buildOkEvent(String serviceName) {
        return buildEvent("OK", serviceName, Status.OK);
    }

    private static JugglerEvent buildWarnEvent(String description, String serviceName) {
        return buildEvent(description, serviceName, Status.WARN);
    }

    private static JugglerEvent buildCritEvent(String description, String serviceName) {
        return buildEvent(description, serviceName, Status.CRIT);
    }

    private static JugglerEvent buildEvent(String description, String serviceName, Status status) {
        return new JugglerEvent(
                description,
                TEST_JUGGLER_HOST_NAME,
                "",
                serviceName,
                status,
                List.of("market-billing-tms", MonitorJobsGroups.CHECK_RESULTS.getTag())
        );
    }

    private void addTableToYt(String tablePath, List<YTreeMapNode> rows) {
        doAnswer(invocation -> {
            rows.forEach(invocation.getArgument(2));
            return null;
        })
                .when(tables).read(
                        argThat(ypath -> ypath.toString().equals(tablePath)),
                        eq(YTableEntryTypes.YSON),
                        any(Consumer.class)
                );

        doReturn(true).when(cypress).exists(
                (YPath) argThat(ypath -> ypath.toString().equals(tablePath))
        );
    }

    @BeforeEach
    void setUp() {
        when(yt.tables()).thenReturn(tables);
        when(yt.cypress()).thenReturn(cypress);
        checkResultService = new CheckResultService(new CheckResultYtDao(yt));
    }

    @Test
    @DisplayName("Событие CRIT, если расхождения найдены")
    void getCritMismatchEvent() {
        var testing = new CheckResultEventService(checkResultService, TEST_DAYLIGHT_CLOCK, TEST_JUGGLER_HOST_NAME);
        var check = CheckResultType.DAILY_CHECKOUTER_PAYMENTS_VS_BILLING_PAYOUTS;
        addTableToYt(check.getResultYtFolder() + "/" + DATE_FORMATTER.format(TEST_DATE), getCheckResultRows());
        var description = "3 mismatches were found in \"" + check.getCheckName() + "\" check at "
                + TEST_DATE.format(DATE_FORMATTER) + ". Quantity by types: "
                + MismatchType.SECOND_ABSENT.getId() + " - 2, "
                + MismatchType.VALUE_MISMATCH.getId() + " - 1.";
        var excepted = buildCritEvent(description, check.getServiceName());
        var actual = testing.getEventsForDate(TEST_DATE);
        assertThat(actual).usingRecursiveFieldByFieldElementComparator().contains(excepted);
    }

    @Test
    @DisplayName("Событие WARN, если расхождения найдены")
    void getWarnMismatchEvent() {
        var testing = new CheckResultEventService(checkResultService, TEST_DAYLIGHT_CLOCK, TEST_JUGGLER_HOST_NAME);
        var check = CheckResultType.DAILY_CHECKOUTER_PAYMENTS_VS_BILLING_PAYOUTS;
        addTableToYt(check.getResultYtFolder() + "/" + DATE_FORMATTER.format(TEST_DATE), getCheckResultRows());
        var description = "3 mismatches were found in \"" + check.getCheckName() + "\" check at "
                + TEST_DATE.format(DATE_FORMATTER) + ". Quantity by types: "
                + MismatchType.SECOND_ABSENT.getId() + " - 2, "
                + MismatchType.VALUE_MISMATCH.getId() + " - 1.";
        var excepted = buildCritEvent(description, check.getServiceName());
        var actual = testing.getEventsForDate(TEST_DATE);
        assertThat(actual).usingRecursiveFieldByFieldElementComparator().contains(excepted);
    }

    @Test
    @DisplayName("Событие, если таблица сверки пустая")
    void getEventWhenTableIsEmpty() {
        var testing = new CheckResultEventService(checkResultService, TEST_DAYLIGHT_CLOCK, TEST_JUGGLER_HOST_NAME);
        var check = CheckResultType.DAILY_CHECKOUTER_REFUNDS_VS_BILLING_ACCRUALS;
        addTableToYt(check.getResultYtFolder() + "/" + DATE_FORMATTER.format(TEST_DATE), List.of());
        var excepted = buildOkEvent(check.getServiceName());
        var actual = testing.getEventsForDate(TEST_DATE);
        assertThat(actual).usingRecursiveFieldByFieldElementComparator().contains(excepted);
    }

    @Test
    @DisplayName("Событие CRIT с ошибкой, если таблицы не существует")
    void getCritEventWhenNoTableExists() {
        addTableToYt(
                CheckResultType.DAILY_CHECKOUTER_REFUNDS_VS_BILLING_PAYOUTS.getResultYtFolder()
                        + "/" + DATE_FORMATTER.format(TEST_DATE),
                List.of()
        );
        addTableToYt(
                CheckResultType.DAILY_CHECKOUTER_PAYMENTS_VS_BILLING_ACCRUALS.getResultYtFolder()
                        + "/" + DATE_FORMATTER.format(TEST_DATE),
                List.of()
        );
        var testing = new CheckResultEventService(checkResultService, TEST_DAYLIGHT_CLOCK, TEST_JUGGLER_HOST_NAME);
        var check = CheckResultType.DAILY_CHECKOUTER_PAYMENTS_VS_BILLING_PAYOUTS;
        var description = String.format(
                "YT table %s not found",
                check.getResultYtFolder() + "/" + TEST_DATE.format(DATE_FORMATTER)
        );
        var expected = buildCritEvent(description, check.getServiceName());
        var actual = testing.getEventsForDate(TEST_DATE);
        assertThat(actual).usingRecursiveFieldByFieldElementComparator().contains(expected);
    }

    @Test
    @DisplayName("Событие WARN с ошибкой, если таблицы не существует")
    void getWarnEventWhenNoTableExists() {
        addTableToYt(
                CheckResultType.DAILY_CHECKOUTER_REFUNDS_VS_BILLING_PAYOUTS.getResultYtFolder()
                        + "/" + DATE_FORMATTER.format(TEST_DATE),
                List.of()
        );
        addTableToYt(
                CheckResultType.DAILY_CHECKOUTER_PAYMENTS_VS_BILLING_ACCRUALS.getResultYtFolder()
                        + "/" + DATE_FORMATTER.format(TEST_DATE),
                List.of()
        );
        var testing = new CheckResultEventService(checkResultService, TEST_NIGHT_CLOCK, TEST_JUGGLER_HOST_NAME);
        var check = CheckResultType.DAILY_CHECKOUTER_REFUNDS_VS_BILLING_ACCRUALS;
        var description = String.format(
                "YT table %s not found",
                check.getResultYtFolder() + "/" + TEST_DATE.format(DATE_FORMATTER)
        );
        var expected = buildWarnEvent(description, check.getServiceName());
        var actual = testing.getEventsForDate(TEST_DATE);
        assertThat(actual).usingRecursiveFieldByFieldElementComparator().contains(expected);
    }

}

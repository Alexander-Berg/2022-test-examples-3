package ru.yandex.market.rg.asyncreport.orders.cis;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.io.ByteOrderMark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.asyncreport.exception.EmptyReportException;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.rg.asyncreport.orders.cis.service.OrderCISService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link OrdersCISReportGenerator}.
 *
 * @author don-dron Zvorygin Andrey don-dron@yandex-team.ru
 */
@ExtendWith(MockitoExtension.class)
class OrderCISReportGeneratorTest {
    private static final DateTimeFormatter XML_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            .withZone(DateTimes.MOSCOW_TIME_ZONE);
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(DateTimes.MOSCOW_TIME_ZONE);
    private static final LocalDate DATE = LocalDate.of(2007, 12, 3);
    private OrdersCISReportGenerator reportGenerator;

    @Mock
    private OrderCISService orderCISService;

    static Stream<Arguments> csvArgs() {
        return Stream.of(
                simpleCsvTest(),
                escapeCsvTest()
        );
    }

    static Stream<Arguments> xmlArgs() {
        return Stream.of(
                simpleXmlTest(),
                escapeXmlTest()
        );
    }

    @BeforeEach
    void beforeEach() {
        reportGenerator = new OrdersCISReportGenerator(orderCISService);
    }

    @DisplayName("Получение ошибки пустого отчета  CSV")
    @Test
    void testEmptyCsv() {
        assertThrows(EmptyReportException.class, () -> {
            try (var writer = new StringWriter()) {
                var reportParams =
                        new OrderCISReportParams(774L,
                                DATE.atStartOfDay().toInstant(ZoneOffset.UTC),
                                DATE.plusDays(1L).atStartOfDay().toInstant(ZoneOffset.UTC),
                                false);
                when(orderCISService.getOrderItemCISBySupplier(
                        reportParams.getPartnerId(),
                        reportParams.getDateTimeFrom(),
                        reportParams.getDateTimeTo(),
                        reportParams.isDelivered())).thenReturn(Collections.emptyList());

                reportGenerator.generateCsv(reportParams, writer);
            }
        });
    }

    @DisplayName("Получение ошибки пустого отчета  XML")
    @Test
    void testEmptyXml() {
        assertThrows(EmptyReportException.class, () -> {
            try (var writer = new StringWriter()) {
                var reportParams =
                        new OrderCISReportParams(774L,
                                DATE.atStartOfDay().toInstant(ZoneOffset.UTC),
                                DATE.plusDays(1L).atStartOfDay().toInstant(ZoneOffset.UTC),
                                true);
                when(orderCISService.getOrderItemCISBySupplier(
                        reportParams.getPartnerId(),
                        reportParams.getDateTimeFrom(),
                        reportParams.getDateTimeTo(),
                        reportParams.isDelivered())).thenReturn(Collections.emptyList());

                reportGenerator.generateXml(reportParams, writer);
            }
        });
    }

    /**
     * Тестирование генерации отчета о кодах маркировки в заказах в формате CSV.
     *
     * @param reportParams - параметры отчета
     * @param items - элементы отчета мок
     * @param result - ожидаемый результат
     * @throws IOException - ошибка генерации
     */
    @MethodSource("csvArgs")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Тестирование CSV")
    void testCsv(
            String description,
            OrderCISReportParams reportParams,
            List<OrderItemCIS> items,
            String result
    ) throws IOException {
        try (var writer = new StringWriter()) {
            when(orderCISService.getOrderItemCISBySupplier(
                    reportParams.getPartnerId(),
                    reportParams.getDateTimeFrom(),
                    reportParams.getDateTimeTo(),
                    reportParams.isDelivered())).thenReturn(items);

            reportGenerator.generateCsv(reportParams, writer);
            assertEquals(result, writer.getBuffer().toString());
        }
    }

    /**
     * Тестирование генерации отчета о кодах маркировки в заказах в формате XML.
     *
     * @param reportParams - параметры отчета
     * @param items - элементы отчета мок
     * @param result - ожидаемый результат
     * @throws IOException - ошибка генерации
     */
    @MethodSource("xmlArgs")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Тестирование XML")
    void testXml(
            String description,
            OrderCISReportParams reportParams,
            List<OrderItemCIS> items,
            String result
    ) throws IOException {
        try (var writer = new StringWriter()) {
            when(orderCISService.getOrderItemCISBySupplier(
                    reportParams.getPartnerId(),
                    reportParams.getDateTimeFrom(),
                    reportParams.getDateTimeTo(),
                    reportParams.isDelivered())).thenReturn(items);

            reportGenerator.generateXml(reportParams, writer);
            MbiAsserts.assertXmlEquals(result, writer.getBuffer().toString());
        }
    }

    private static OrderItemCIS createItem(String inn,
                                           String reason,
                                           String name,
                                           String type,
                                           long id,
                                           LocalDate date,
                                           String cis) {
        return OrderItemCIS.Builder.builder()
                .setInn(inn)
                .setReason(reason)
                .setName(name)
                .setType(type)
                .setId(id)
                .setDate(date)
                .setCis(cis)
                .build();
    }

    private static String getCsvHeaders() {
        return ByteOrderMark.UTF_BOM + String.join(",",
                "ИНН поставщика",
                "Причина вывода из оборота",
                "Вид первичного документа",
                "Наименование первичного документа",
                "Номер первичного документа",
                "Дата первичного документа",
                "Код маркировки");
    }

    private static String csvResult(String items) {
        return getCsvHeaders() +
                System.lineSeparator() +
                items +
                System.lineSeparator();
    }

    private static Arguments simpleCsvTest() {
        var orderItemCIS = List.of(
                createItem("1", "2", "3", "4", 5L, DATE, "0101")
        );
        return Arguments.of(
                "Тест на заполнение отчета в формате CSV",
                new OrderCISReportParams(
                        774L,
                        DATE.atStartOfDay().toInstant(ZoneOffset.UTC),
                        DATE.plusDays(1L).atStartOfDay().toInstant(ZoneOffset.UTC),
                        true),
                orderItemCIS,
                csvResult("1,2,3,4,5," + CSV_DATE_FORMAT.format(DATE) + "," + "\"0101\""));
    }

    private static Arguments escapeCsvTest() {
        var orderItemCIS = List.of(
                createItem("1", "2", "3", "4", 5L, DATE, "0101"),
                createItem("8", "7", "6", "5", 4L, DATE, "0\"10\"1")
        );
        return Arguments.of(
                "Тест на экранирование в отчете в формате CSV",
                new OrderCISReportParams(774L,
                        DATE.atStartOfDay().toInstant(ZoneOffset.UTC),
                        DATE.plusDays(1L).atStartOfDay().toInstant(ZoneOffset.UTC),
                        true),
                orderItemCIS,
                csvResult("1,2,3,4,5," + CSV_DATE_FORMAT.format(DATE) + "," + "\"0101\"" + "\n" +
                        "8,7,6,5,4," + CSV_DATE_FORMAT.format(DATE) + "," + "\"0\"\"10\"\"1\""));
    }

    private static Arguments simpleXmlTest() {
        var orderItemCIS = List.of(
                createItem("1", "2", "3", "4", 5L, DATE, "0101")
        );
        return Arguments.of(
                "Тест на заполнение в отчета в формате XML",
                new OrderCISReportParams(774L,
                        DATE.atStartOfDay().toInstant(ZoneOffset.UTC),
                        DATE.plusDays(1L).atStartOfDay().toInstant(ZoneOffset.UTC),
                        true),
                orderItemCIS,
                String.format(StringTestUtil.getString(OrderCISReportGeneratorTest.class, "simpleOrderCISReportGeneratorTest.xml"),
                        XML_DATE_FORMAT.format(DATE), XML_DATE_FORMAT.format(DATE), XML_DATE_FORMAT.format(DATE)));
    }

    private static Arguments escapeXmlTest() {
        var orderItemCIS = List.of(
                createItem("1", "2", "3", "4", 5L, DATE, "0101"),
                createItem("8", "7", "6", "5", 4L, DATE, "0\"10\"1")
        );
        return Arguments.of(
                "Тест на экранирование в отчете в формате XML",
                new OrderCISReportParams(774L,
                        DATE.atStartOfDay().toInstant(ZoneOffset.UTC),
                        DATE.plusDays(1L).atStartOfDay().toInstant(ZoneOffset.UTC),
                        true),
                orderItemCIS,
                String.format(StringTestUtil.getString(OrderCISReportGeneratorTest.class, "escapeOrderCISReportGeneratorTest.xml"),
                        XML_DATE_FORMAT.format(DATE),
                        XML_DATE_FORMAT.format(DATE),
                        XML_DATE_FORMAT.format(DATE),
                        XML_DATE_FORMAT.format(DATE)));
    }
}

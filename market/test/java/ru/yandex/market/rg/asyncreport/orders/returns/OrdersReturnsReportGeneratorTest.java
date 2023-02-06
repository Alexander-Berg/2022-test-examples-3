package ru.yandex.market.rg.asyncreport.orders.returns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.asyncreport.exception.EmptyReportException;
import ru.yandex.market.core.billing.OrderCheckpointDao;
import ru.yandex.market.core.billing.returns.OrderReturnStorageBilledReportDao;
import ru.yandex.market.core.billing.returns.ReturnItemBilledReportDao;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.delivery.LogisticPointInfoYtDao;
import ru.yandex.market.core.delivery.model.LogisticPointInfo;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.core.order.OrderItemTransactionService;
import ru.yandex.market.core.order.OrdersInfoService;
import ru.yandex.market.core.order.ReceiptItemDao;
import ru.yandex.market.core.order.returns.OrderReturnsService;
import ru.yandex.market.core.order.returns.os.OrderServiceReturnDao;
import ru.yandex.market.core.order.returns.os.model.LogisticReturnLine;
import ru.yandex.market.core.order.returns.os.model.ReturnLine;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.orderservice.client.model.ReturnDTO;
import ru.yandex.market.rg.client.orderservice.RgOrderServiceClient;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "OrdersReturnsReportGeneratorTest.basicTest.csv")
class OrdersReturnsReportGeneratorTest extends FunctionalTest {

    private static final Date DATE_FROM = DateUtil.asDate(LocalDate.of(2000, 8, 26));
    private static final Date DATE_TO = DateUtil.asDate(LocalDate.of(2024, 11, 27));
    private static final Date LONG_AGO_DATE = DateUtil.asDate(LocalDate.of(2012, 11, 1));
    private static final long SUPPLIER_ID = 1099239;
    private static final long DBS_PARTNER_ID = 1099240;

    @Autowired
    OrderItemTransactionService orderItemTransactionService;
    @Autowired
    private OrdersInfoService ordersInfoService;
    @Autowired
    private ReceiptItemDao receiptItemDao;
    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;
    @Autowired
    private OrderCheckpointDao orderCheckpointDao;
    @Autowired
    private OrderReturnsService orderReturnsService;
    @Autowired
    private TestableClock clock;

    @Autowired
    private DeliveryInfoService deliveryInfoService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ReturnItemBilledReportDao returnItemBilledReportDao;

    @Autowired
    private RgOrderServiceClient rgOrderServiceClient;

    private OrderReturnStorageBilledReportDao orderReturnStorageBilledReportDao;

    private LogisticPointInfoYtDao logisticPointInfoYtDao;

    private OrdersReturnsReportFactory ordersReturnsReportFactory;

    private OrderServiceReturnDao orderServiceReturnDao;

    private ObjectMapper mapper;

    @BeforeEach
    void init() throws IOException {
        mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);

        orderServiceReturnDao = mock(OrderServiceReturnDao.class);
        logisticPointInfoYtDao = mock(LogisticPointInfoYtDao.class);
        orderReturnStorageBilledReportDao = mock(OrderReturnStorageBilledReportDao.class);

        when(logisticPointInfoYtDao.lookupLogisticPointsInfo(anyCollection()))
                .thenReturn(List.of(LogisticPointInfo.builder()
                        .setLogisticPointId(10001700279L)
                        .setLocalityName("Местность")
                        .setPointName("Точка")
                        .setPremiseNumber("Номер")
                        .setStreetName("Улица")
                        .setShipperName("Название отгрузчика")
                        .build()));

        ordersReturnsReportFactory = new OrdersReturnsReportFactory(
                new OrdersReturnsDateRangeReportGenerator(
                        ordersInfoService,
                        orderItemTransactionService,
                        receiptItemDao,
                        partnerTypeAwareService,
                        orderServiceReturnDao,
                        orderCheckpointDao,
                        orderReturnsService,
                        clock,
                        deliveryInfoService,
                        logisticPointInfoYtDao,
                        returnItemBilledReportDao,
                        orderReturnStorageBilledReportDao,
                        environmentService,
                        rgOrderServiceClient
                ),
                new OrdersReturnsReadyForPickupGenerator(
                        ordersInfoService,
                        orderItemTransactionService,
                        receiptItemDao,
                        partnerTypeAwareService,
                        orderServiceReturnDao,
                        orderCheckpointDao,
                        orderReturnsService,
                        clock,
                        deliveryInfoService,
                        logisticPointInfoYtDao,
                        returnItemBilledReportDao,
                        orderReturnStorageBilledReportDao,
                        environmentService,
                        rgOrderServiceClient
                ));

        clock.setFixed(Instant.parse("2021-09-10T15:15:30.00Z"), ZoneOffset.UTC);
    }

    @AfterEach
    void tearDown() {
        clock.clearFixed();
    }

    @Test
    @Disabled
    @DisplayName("Проверка генерации отчета по невыкупленным и возвращенным товарам из Order Service")
    void reportFromOS() throws IOException, InvalidFormatException {
        environmentService.setValue("os.client.experiment.enabled", "true");
        List<ReturnDTO> returns = testReturns("orderServiceReturns.json");

        doAnswer(it -> returns.stream())
                .when(rgOrderServiceClient)
                .streamReturns(any());
        when(orderReturnStorageBilledReportDao.selectForReportByOrderIds(anyLong(), any()))
                .thenReturn(Map.of(59385402L, 1116L));
        assertFilled(SUPPLIER_ID, "returnFromOS.xlsx", true);
    }

    @Test
    @DisplayName("Проверка генерации отчета по невыкупленным и возвращенным товарам")
    void orderByDateRange() throws IOException, InvalidFormatException {
        List<ReturnLine> returnLines = testReturnLines("in_range.json");
        Map<Long, List<LogisticReturnLine>> logisticReturnLines = testLogisticReturnLines("logistic_lines.json")
                .stream()
                .collect(Collectors.groupingBy(LogisticReturnLine::getReturnLineId));

        when(orderServiceReturnDao.getReturnLines(anyLong(), any(Instant.class), any(Instant.class)))
                .thenReturn(returnLines);
        when(orderServiceReturnDao.getLogisticReturnLinesByIds(anyList()))
                .thenReturn(logisticReturnLines);
        when(orderReturnStorageBilledReportDao.selectForReportByOrderIds(anyLong(), any()))
                .thenReturn(Map.of(59385402L, 1116L));
        assertFilled(SUPPLIER_ID, "orderByDateRange.xlsx", true);
    }

    @Test
    @DisplayName("[DBS] Проверка генерации отчета по невыкупленным и возвращенным товарам")
    void orderByDateRangeDbs() throws IOException, InvalidFormatException {
        List<ReturnLine> returnLines = testReturnLines("dbs_in_range.json");
        Map<Long, List<LogisticReturnLine>> logisticReturnLines = testLogisticReturnLines("logistic_lines.json")
                .stream()
                .collect(Collectors.groupingBy(LogisticReturnLine::getReturnLineId));

        when(orderServiceReturnDao.getReturnLines(anyLong(), any(Instant.class), any(Instant.class)))
                .thenReturn(returnLines);
        when(orderServiceReturnDao.getLogisticReturnLinesByIds(anyList()))
                .thenReturn(logisticReturnLines);
        when(orderReturnStorageBilledReportDao.selectForReportByOrderIds(anyLong(), any()))
                .thenReturn(Map.of(59385403L, 1116L));
        assertFilled(DBS_PARTNER_ID, "dbsOrderByDateRange.xlsx", true);
    }

    @Test
    @DisplayName("Проверка генерации отчета по невыкупленным товарам, готовым к возврату или переданным поставщику")
    public void ordersReadyForPickupReportTest() throws IOException, InvalidFormatException {
        List<ReturnLine> returnLines = testReturnLines("ready_for_pickup.json");
        Map<Long, List<LogisticReturnLine>> logisticReturnLines = testLogisticReturnLines("logistic_lines.json")
                .stream()
                .collect(Collectors.groupingBy(LogisticReturnLine::getReturnLineId));

        when(orderServiceReturnDao.getReturnLinesReadyForPickup(anyLong()))
                .thenReturn(returnLines);
        when(orderServiceReturnDao.getLogisticReturnLinesByIds(anyList()))
                .thenReturn(logisticReturnLines);
        when(orderReturnStorageBilledReportDao.selectForReportByOrderIds(anyLong(), any()))
                .thenReturn(Map.of(59891913L, 1116L));

        assertFilled(SUPPLIER_ID, "orderReadyForPickup.xlsx", false);
    }

    @Test
    @DisplayName("[DBS] Проверка генерации отчета по невыкупленным товарам, готовым к возврату или переданным " +
            "поставщику")
    public void ordersReadyForPickupReportTestDbs() throws IOException, InvalidFormatException {
        List<ReturnLine> returnLines = testReturnLines("dbs_ready_for_pickup.json");
        Map<Long, List<LogisticReturnLine>> logisticReturnLines = testLogisticReturnLines("logistic_lines.json")
                .stream()
                .collect(Collectors.groupingBy(LogisticReturnLine::getReturnLineId));

        when(orderServiceReturnDao.getReturnLinesReadyForPickup(anyLong()))
                .thenReturn(returnLines);
        when(orderServiceReturnDao.getLogisticReturnLinesByIds(anyList()))
                .thenReturn(logisticReturnLines);
        when(orderReturnStorageBilledReportDao.selectForReportByOrderIds(anyLong(), any()))
                .thenReturn(Map.of(59385403L, 1481L));

        assertFilled(DBS_PARTNER_ID, "dbsOrderReadyForPickup.xlsx", false);
    }

    @Test
    @DisplayName("Пустой отчет не должен загружаться в MDS")
    public void testEmptyReport() {
        assertThatExceptionOfType(EmptyReportException.class).isThrownBy(() -> {
            OrdersReturnsParams reportParams = getReportParams(SUPPLIER_ID, LONG_AGO_DATE.toInstant(),
                    LONG_AGO_DATE.toInstant().plus(Duration.ofDays(1)));
            ordersReturnsReportFactory.getReportGenerator(reportParams).generateReport(reportParams, null);
        });
    }

    private void assertFilled(long supplierId,
                              String expectedPath,
                              boolean isDateRangeReport) throws IOException, InvalidFormatException {
        File reportFile = TempFileUtils.createTempFile("tmpReport", ".xlsx");
        try (OutputStream output = new FileOutputStream(reportFile)) {
            OrdersReturnsParams reportParams;
            if (isDateRangeReport) {
                reportParams = getReportParams(supplierId, DATE_FROM.toInstant(), DATE_TO.toInstant());
            } else {
                reportParams = getReportParams(supplierId);
            }
            ordersReturnsReportFactory.getReportGenerator(reportParams).generateReport(reportParams, output);
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expectedXls = new XSSFWorkbook(getClass().getResourceAsStream(expectedPath));

        ExcelTestUtils.assertEquals(
                expectedXls,
                actual,
                new LinkedHashSet<>(Set.of(0))
        );
    }

    private OrdersReturnsParams getReportParams(long supplierId) {
        return getReportParams(supplierId, null, null);
    }

    private OrdersReturnsParams getReportParams(long supplierId, Instant dateTimeFrom, Instant dateTimeTo) {
        return new OrdersReturnsParams(supplierId, dateTimeFrom, dateTimeTo, false);
    }

    private List<ReturnLine> testReturnLines(String path) throws IOException {
        var s = StringTestUtil.getString(this.getClass(), path);
        var typeRef = mapper
                .getTypeFactory()
                .constructCollectionType(ArrayList.class, ReturnLine.class);
        List<ReturnLine> returnLines = mapper.readValue(s, typeRef);
        return returnLines;
    }

    private List<ReturnDTO> testReturns(String path) throws IOException {
        var s = StringTestUtil.getString(this.getClass(), path);
        var typeRef = mapper
                .getTypeFactory()
                .constructCollectionType(ArrayList.class, ReturnDTO.class);
        List<ReturnDTO> returns = mapper.readValue(s, typeRef);
        return returns;
    }

    private List<LogisticReturnLine> testLogisticReturnLines(String path) throws IOException {
        var s = StringTestUtil.getString(this.getClass(), path);
        var typeRef = mapper
                .getTypeFactory()
                .constructCollectionType(ArrayList.class, LogisticReturnLine.class);
        List<LogisticReturnLine> logisticReturnLines = mapper.readValue(s, typeRef);
        return logisticReturnLines;
    }
}

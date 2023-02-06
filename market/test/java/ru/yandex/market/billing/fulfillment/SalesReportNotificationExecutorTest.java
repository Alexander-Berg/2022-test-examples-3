package ru.yandex.market.billing.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.rating.operational.PartnerRatingDTO;
import ru.yandex.market.abo.api.entity.rating.operational.RatingPartnerType;
import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDao;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.fulfillment.SalesReportNotificationExecutor.WarehouseStock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.dao.SalesReportHistoryDao;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.delivery.DeliveryServiceInfo;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsFilter;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsRow;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsRowWarehouse;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtServiceImpl;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtStorage;
import ru.yandex.market.core.replenishment.supplier.PilotSupplierYtDao;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.core.warehouse.service.WarehouseCapabilityService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.billing.fulfillment.SalesReportNotificationExecutor.LOW_STOCK_NOTIFICATION_TEMPLATE;

/**
 * Тесты для {@link SalesReportNotificationExecutor}.
 */
@DbUnitDataSet(before = "SalesReportNotificationExecutorTest.before.csv")
@ExtendWith(MockitoExtension.class)
class SalesReportNotificationExecutorTest extends FunctionalTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private PrepayRequestDao prepayRequestDao;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    private SalesReportHistoryDao salesReportHistoryDao;

    @Autowired
    private DeliveryInfoService deliveryInfoService;

    @Autowired
    private WarehouseCapabilityService warehouseCapabilityService;

    @Autowired
    private PilotSupplierYtDao pilotSupplierYtDao;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AboPublicRestClient aboPublicRestClient;

    @Autowired
    private EnvironmentService environmentService;

    @Spy
    @Autowired
    private SalesDynamicsYtStorage salesDynamicsYtStorage;

    private SalesReportNotificationExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new SalesReportNotificationExecutor(
                supplierService,
                notificationService,
                campaignService,
                prepayRequestDao,
                new SalesDynamicsYtServiceImpl(warehouseCapabilityService, salesDynamicsYtStorage),
                partnerTypeAwareService,
                salesReportHistoryDao,
                environmentService,
                Collections.emptyList(),
                aboPublicRestClient,
                pilotSupplierYtDao,
                deliveryInfoService
        );
        mockYtStorage();
        environmentService.setValue(SalesReportNotificationExecutor.LAST_SUCCESS_RUN_DATE_ENV,
                LocalDate.now().minusDays(7).toString());
    }

    @Test
    void testNotWorkingInTime() {
        String strDate = "\\d{4}-" + DateTimeFormatter.ofPattern("MM-dd").format(LocalDate.now());
        SalesReportNotificationExecutor executor = new SalesReportNotificationExecutor(
                supplierService,
                notificationService,
                campaignService,
                prepayRequestDao,
                new SalesDynamicsYtServiceImpl(warehouseCapabilityService, salesDynamicsYtStorage),
                partnerTypeAwareService,
                salesReportHistoryDao,
                environmentService,
                Collections.singletonList(strDate),
                aboPublicRestClient,
                pilotSupplierYtDao,
                deliveryInfoService
        );
        executor.doJob(null);
        Mockito.verify(notificationService, Mockito.never()).send(any(NotificationSendContext.class));
    }

    @DbUnitDataSet(after = "SalesReportNotificationExecutorTest.after.csv")
    @Test
    void testDoJob_withNoPilotSuppliers() {
        when(pilotSupplierYtDao.getPilotSupplierIds()).thenReturn(List.of());
        SalesReportNotificationExecutor.SalesReportMailInfo supplierMailInfo = prepareSalesMailInfo();
        SalesReportNotificationExecutor.SalesReportMailInfo dropshipMailInfo = prepareDropshipSalesMailInfo();
        mockFFRatingData();

        executor.doJob(null);

        ArgumentCaptor<NotificationSendContext> contextCaptor = ArgumentCaptor.forClass(NotificationSendContext.class);

        NotificationSendContext sendContext =
                new NotificationSendContext.Builder()
                        .setTypeId(LOW_STOCK_NOTIFICATION_TEMPLATE)
                        .setShopId(1000)
                        .setData(Collections.singletonList(supplierMailInfo))
                        .build();

        Mockito.verify(notificationService, Mockito.times(5)).send(contextCaptor.capture());

        NotificationSendContext expectedSendContext = contextCaptor.getAllValues()
                .stream().filter(context -> 1000L == context.getShopId()).findFirst().orElseThrow();

        assertEquals(sendContext.getShopId(), expectedSendContext.getShopId());
        assertEquals(sendContext.getTypeId(), expectedSendContext.getTypeId());

        assertEqualsMailInfo(
                (SalesReportNotificationExecutor.SalesReportMailInfo) expectedSendContext.getData().get(0),
                supplierMailInfo);

        NotificationSendContext lowRatedFFExpectedSendContext = contextCaptor.getAllValues().stream()
                .filter(context -> 1007L == context.getShopId()).findFirst().orElseThrow();

        // в случае низкого рейтинга ФФ и наличия продаж за последние 28 дней отправляется сообщение,
        // что товаров на стоке достаточно, пополнение не требуется
        assertThat(lowRatedFFExpectedSendContext.getTypeId(),
                Is.is(SalesReportNotificationExecutor.FULL_STOCK_NOTIFICATION_TEMPLATE));

        NotificationSendContext dropshipSendContext = contextCaptor.getAllValues().stream()
                .filter(context -> 1003L == context.getShopId()).findFirst().orElseThrow();

        assertEqualsMailInfo(
                (SalesReportNotificationExecutor.SalesReportMailInfo) dropshipSendContext.getData().get(0),
                dropshipMailInfo);

        Mockito.verifyZeroInteractions(notificationService);
    }

    @DbUnitDataSet(after = "SalesReportNotificationExecutorTest.after.csv")
    @Test
    void testDoJob_filteredByPilotSuppliers() {
        when(pilotSupplierYtDao.getPilotSupplierIds()).thenReturn(List.of(1001L, 1004L));
        SalesReportNotificationExecutor.SalesReportMailInfo supplierMailInfo = prepareSalesMailInfo();
        SalesReportNotificationExecutor.SalesReportMailInfo dropshipMailInfo = prepareDropshipSalesMailInfo();
        mockFFRatingData();

        executor.doJob(null);

        ArgumentCaptor<NotificationSendContext> contextCaptor = ArgumentCaptor.forClass(NotificationSendContext.class);

        NotificationSendContext sendContext =
                new NotificationSendContext.Builder()
                        .setTypeId(LOW_STOCK_NOTIFICATION_TEMPLATE)
                        .setShopId(1000)
                        .setData(Collections.singletonList(supplierMailInfo))
                        .build();

        Mockito.verify(notificationService, Mockito.times(3)).send(contextCaptor.capture());

        NotificationSendContext expectedSendContext = contextCaptor.getAllValues().stream()
                .filter(context -> 1000L == context.getShopId()).findFirst().orElseThrow();

        assertEquals(sendContext.getShopId(), expectedSendContext.getShopId());
        assertEquals(sendContext.getTypeId(), expectedSendContext.getTypeId());

        assertEqualsMailInfo(
                (SalesReportNotificationExecutor.SalesReportMailInfo) expectedSendContext.getData().get(0),
                supplierMailInfo);

        NotificationSendContext lowRatedFFExpectedSendContext = contextCaptor.getAllValues().stream()
                .filter(context -> 1007L == context.getShopId()).findFirst().orElseThrow();

        // в случае низкого рейтинга ФФ и наличия продаж за последние 28 дней отправляется сообщение,
        // что товаров на стоке достаточно, пополнение не требуется
        assertThat(lowRatedFFExpectedSendContext.getTypeId(),
                Is.is(SalesReportNotificationExecutor.FULL_STOCK_NOTIFICATION_TEMPLATE));

        NotificationSendContext dropshipSendContext = contextCaptor.getAllValues().stream()
                .filter(context -> 1003L == context.getShopId()).findFirst().orElseThrow();

        assertEqualsMailInfo(
                (SalesReportNotificationExecutor.SalesReportMailInfo) dropshipSendContext.getData().get(0),
                dropshipMailInfo);

        Mockito.verifyZeroInteractions(notificationService);
    }

    /**
     * Нотификации не отправляются партнёрам, которые сегодня уже их получили.
     */
    @Test
    void dontNotifyAlreadyNotifiedPartners() {
        mockFFRatingData();
        executor.doJob(null);

        ArgumentCaptor<NotificationSendContext> contextCaptor = ArgumentCaptor.forClass(NotificationSendContext.class);

        Mockito.verify(notificationService, Mockito.times(5)).send(contextCaptor.capture());

        List<NotificationSendContext> sendContexts = contextCaptor.getAllValues();

        assertThat(
                sendContexts.stream()
                        .map(NotificationSendContext::getShopId)
                        .collect(Collectors.toSet()),
                not(contains(1005L)));
    }

    /**
     * После падения джобы она повторно отрабатывает с нормальными результатами.
     */
    @DbUnitDataSet(after = "SalesReportNotificationExecutorTest.after.csv")
    @Test
    void doJobAfterFailService() {
        SalesDynamicsFilter filter = new SalesDynamicsFilter(1000L, LocalDate.now().minusDays(1));
        Mockito.doThrow(new RuntimeException()).when(salesDynamicsYtStorage).getSalesDynamicsReport(eq(filter), any()
                , any());
        Assertions.assertThrows(RuntimeException.class, () -> executor.doJob(null));
        mockYtStorage();
        mockFFRatingData();
        executor.doJob(null);
    }

    @Test
    void chooseTemplateId() {
        SalesReportNotificationExecutor.SalesReportMailInfo lowStockInfo =
                new SalesReportNotificationExecutor.SalesReportMailInfo();
        lowStockInfo.setSkuCountToSupply(10);
        lowStockInfo.setSkuCountInStock(10);
        mockFFRatingData();

        assertThat(executor.chooseTemplateId(1001L, lowStockInfo),
                Is.is(SalesReportNotificationExecutor.LOW_STOCK_NOTIFICATION_TEMPLATE));

        // в случае низкого рейтинга ФФ и отсутствия продаж за последние 28 дней ничего не отправляем
        assertNull(executor.chooseTemplateId(1007L, lowStockInfo));

        SalesReportNotificationExecutor.SalesReportMailInfo fullStockInfo =
                new SalesReportNotificationExecutor.SalesReportMailInfo();
        fullStockInfo.setOrderedSkus(10);

        assertThat(executor.chooseTemplateId(1002L, fullStockInfo),
                Is.is(SalesReportNotificationExecutor.FULL_STOCK_NOTIFICATION_TEMPLATE));

        SalesReportNotificationExecutor.SalesReportMailInfo dropshipInfo =
                new SalesReportNotificationExecutor.SalesReportMailInfo();
        dropshipInfo.setOrderedSkus(10);
        dropshipInfo.setSkuCountInStock(20);

        assertThat(executor.chooseTemplateId(1003L, dropshipInfo),
                Is.is(SalesReportNotificationExecutor.DROPSHIP_NOTIFICATION_TEMPLATE));
    }

    private void assertEqualsMailInfo(
            SalesReportNotificationExecutor.SalesReportMailInfo captured,
            SalesReportNotificationExecutor.SalesReportMailInfo expected
    ) {
        assertEquals(expected.getOrderedSkus(), captured.getOrderedSkus());
        assertEquals(expected.getOrderedItems(), captured.getOrderedItems());
        assertEquals(expected.getOrderedItemsCost(), captured.getOrderedItemsCost());
        assertEquals(expected.getSkuCountInStock(), captured.getSkuCountInStock());
        assertEquals(expected.getSkuCountToSupply(), captured.getSkuCountToSupply());
        assertEquals(expected.getItemCountInStock(), captured.getItemCountInStock());
        assertEquals(expected.getItemCountToSupply(), captured.getItemCountToSupply());

        verifyWarehousesEquality(expected.getWarehouseStocks(), captured.getWarehouseStocks());
    }

    private SalesReportNotificationExecutor.SalesReportMailInfo prepareSalesMailInfo() {
        SalesReportNotificationExecutor.SalesReportMailInfo expectedMailInfo2000 =
                new SalesReportNotificationExecutor.SalesReportMailInfo();
        expectedMailInfo2000.setCampaignId(2000);
        expectedMailInfo2000.setSupplierName("OOO Ромашка");
        expectedMailInfo2000.setOrderedSkus(3);
        expectedMailInfo2000.setOrderedItems(40);
        expectedMailInfo2000.setSkuCountInStock(3);
        expectedMailInfo2000.setItemCountInStock(33);
        expectedMailInfo2000.setSkuCountToSupply(3);
        expectedMailInfo2000.setItemCountToSupply(23);
        expectedMailInfo2000.setOrderedItemsCost(new BigDecimal("4050.0"));

        WarehouseStock ekbStock = new WarehouseStock(createFFWarehouse(300, "Яндекс.Маркет (Екатеринбург)"));
        ekbStock.addOrderedItems(10, new BigDecimal("1000.0"), "someSku");
        ekbStock.addOrderedItems(3, new BigDecimal("600.0"), "otherSku");
        ekbStock.addItemsInStock(6, new BigDecimal("100.0"), "someSku");
        ekbStock.addItemsInStock(6, new BigDecimal("200.0"), "otherSku");
        ekbStock.addSuggestItems(2, new BigDecimal("100.0"), "someSku");
        ekbStock.addSuggestItems(5, new BigDecimal("200.0"), "otherSku");

        WarehouseStock rostovStock = new WarehouseStock(createFFWarehouse(147, "Яндекс.Маркет (Ростов-на-Дону)"));
        rostovStock.addOrderedItems(5, new BigDecimal("250.0"), "moreSku");
        rostovStock.addItemsInStock(6, new BigDecimal("50.0"), "moreSku");
        rostovStock.addSuggestItems(10, new BigDecimal("50.0"), "moreSku");

        WarehouseStock tomilinoStock = new WarehouseStock(createFFWarehouse(171, "Яндекс.Маркет (Томилино)"));
        tomilinoStock.addOrderedItems(7, new BigDecimal("700.0"), "someSku");
        tomilinoStock.addItemsInStock(6, new BigDecimal("100.0"), "someSku");
        tomilinoStock.addSuggestItems(3, new BigDecimal("100.0"), "someSku");

        WarehouseStock sofinoStock = new WarehouseStock(createFFWarehouse(172, "Яндекс.Маркет (Софьино)"));
        sofinoStock.addOrderedItems(15, new BigDecimal("1500.0"), "someSku");
        sofinoStock.addItemsInStock(9, new BigDecimal("300.0"), "someSku");
        sofinoStock.addSuggestItems(3, new BigDecimal("300.0"), "someSku");

        expectedMailInfo2000.setWarehouseStocks(List.of(ekbStock, rostovStock, tomilinoStock, sofinoStock));

        return expectedMailInfo2000;
    }

    private DeliveryServiceInfo createFFWarehouse(long id, String name) {
        DeliveryServiceInfo warehouse = new DeliveryServiceInfo(id, name);
        warehouse.setType(DeliveryServiceType.FULFILLMENT);
        return warehouse;
    }

    private SalesReportNotificationExecutor.SalesReportMailInfo prepareDropshipSalesMailInfo() {
        SalesReportNotificationExecutor.SalesReportMailInfo expectedDropshipMailInfo =
                new SalesReportNotificationExecutor.SalesReportMailInfo();
        expectedDropshipMailInfo.setCampaignId(2003);
        expectedDropshipMailInfo.setSupplierName("Дропшип");
        expectedDropshipMailInfo.setOrderedSkus(1);
        expectedDropshipMailInfo.setOrderedItems(5);
        expectedDropshipMailInfo.setSkuCountInStock(1);
        expectedDropshipMailInfo.setItemCountInStock(10);
        expectedDropshipMailInfo.setOrderedItemsCost(new BigDecimal("1000.0"));

        return expectedDropshipMailInfo;
    }

    private void verifyWarehousesEquality(
            List<WarehouseStock> first,
            List<WarehouseStock> other
    ) {
        if (first == other) {
            return;
        }

        assertEquals(first.size(), other.size());
        first.forEach(stock -> {
            WarehouseStock otherStock =
                    other.stream()
                            .filter(stock2 -> stock2.getWarehouseName().equals(stock.getWarehouseName()))
                            .findFirst()
                            .orElse(null);
            assertNotNull(otherStock);
            assertEquals(stock, otherStock);
        });
    }

    private void mockYtStorage() {
        List<SalesDynamicsRow> rows = List.of(
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("someSku")
                        .addWarehouse(300, SalesDynamicsRowWarehouse.builder()
                                .withGmvWeeks4(1000.0)
                                .withOrdersWeeks4(10.0)
                                .withStockSsku(6.0)
                                .withDemandDynamicRecWeek4(2.0)
                                .build())
                        .withPrice(100.0)
                        .build(),
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("otherSku")
                        .addWarehouse(300, SalesDynamicsRowWarehouse.builder()
                                .withGmvWeeks4(600.0)
                                .withOrdersWeeks4(3.0)
                                .withStockSsku(6.0)
                                .withDemandDynamicRecWeek4(5.0)
                                .build())
                        .withPrice(200.0)
                        .build(),
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("moreSku")
                        .addWarehouse(147, SalesDynamicsRowWarehouse.builder()
                                .withGmvWeeks4(250.0)
                                .withOrdersWeeks4(5.0)
                                .withStockSsku(6.0)
                                .withDemandDynamicRecWeek4(10.0)
                                .build())
                        .withPrice(50.0)
                        .build(),
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("someSku")
                        .addWarehouse(171, SalesDynamicsRowWarehouse.builder()
                                .withGmvWeeks4(700.0)
                                .withOrdersWeeks4(7.0)
                                .withStockSsku(6.0)
                                .withDemandDynamicRecWeek4(3.0)
                                .build())
                        .withPrice(100.0)
                        .build(),
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("skuDropship")
                        .withGmv4weeks(1000.0)
                        .withOrders4weeks(5.0)
                        .withStockTotalSsku(10.0)
                        .withPrice(2000.0)
                        .build(),
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("someSku")
                        .addWarehouse(172, SalesDynamicsRowWarehouse.builder()
                                .withGmvWeeks4(1500.0)
                                .withOrdersWeeks4(15.0)
                                .withStockSsku(9.0)
                                .withDemandDynamicRecWeek4(3.0)
                                .build())
                        .withPrice(300.0)
                        .build()
        );
        doAnswer(invocation -> {
            Consumer<Iterator<SalesDynamicsRow>> consumer = invocation.getArgument(2);
            consumer.accept(rows.iterator());
            return null;
        }).when(salesDynamicsYtStorage).getSalesDynamicsReport(any(), any(), any());
        SalesDynamicsFilter filter = new SalesDynamicsFilter(1006L, LocalDate.now().minusDays(1));
        doNothing().when(salesDynamicsYtStorage).getSalesDynamicsReport(eq(filter), any(), any());
    }

    private void mockFFRatingData() {
        PartnerRatingDTO goodFFRatingDTO =
                new PartnerRatingDTO(1000L, 55.0, 5, Collections.emptyList(),
                        true, true);
        PartnerRatingDTO lowFFRatingDTO =
                new PartnerRatingDTO(1007L, 35.5, 1, Collections.emptyList(),
                        true, true);
        doReturn(goodFFRatingDTO).when(aboPublicRestClient)
                .getPartnerRating(anyLong(), any(RatingPartnerType.class));
        doReturn(lowFFRatingDTO).when(aboPublicRestClient)
                .getPartnerRating(eq(1007L), any(RatingPartnerType.class));
    }
}

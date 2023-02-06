package ru.yandex.market.replenishment.autoorder.service.recommendations_grouping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.Recommendation;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.SupplyRouteType;
import ru.yandex.market.replenishment.autoorder.model.WarehouseType;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.LogisticsParamKey;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Supplier;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Warehouse;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.WarehouseRegion;
import ru.yandex.market.replenishment.autoorder.repository.postgres.AutoprocessingValidationLogRepository;
import ru.yandex.market.replenishment.autoorder.service.DbDemandService;
import ru.yandex.market.replenishment.autoorder.service.EbpCalculatorService;
import ru.yandex.market.replenishment.autoorder.service.WarehouseService;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.environment.FeatureFlagService;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.DemandAutoprocessingFlagHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class DemandAutoprocessingFlagHandlerTest extends FunctionalTest {

    private static final LocalDate NOW_DATE = LocalDate.of(2021, 7, 10);
    private static final LocalDateTime NOW_DATE_TIME = LocalDateTime.of(2021, 7, 10, 0, 0, 0);
    private static final LocalDate BEFORE_NOW_DATE = LocalDate.of(2021, 7, 9);
    private static final LocalDate AFTER_NOW_DATE = LocalDate.of(2021, 7, 11);
    private static final BigDecimal DEMAND_SUM = BigDecimal.valueOf(100);
    private static final String ULTRA_PILOT_USER = "aleksasarmina";
    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private AutoprocessingValidationLogRepository autoprocessingValidationLogRepository;

    private FeatureFlagService featureFlagService;
    private DemandAutoprocessingFlagHandler demandAutoprocessingFlagHandler;
    private DbDemandService dbDemandService;

    private static void setMskWarehouse(DemandDTO demand) {
        if (demand == null) {
            throw new IllegalArgumentException("Demand is null");
        }
        WarehouseRegion region = new WarehouseRegion(WarehouseRegion.Companion.getMoscowId(), "Moscow",
            WarehouseRegion.Companion.getMoscowId());
        Warehouse warehouse = new Warehouse(145L, "Москва 145", WarehouseType.FULFILLMENT, region,
            10000927726L);
        demand.setWarehouse(warehouse);
    }

    private static void setRostovWarehouse(DemandDTO demand) {
        if (demand == null) {
            throw new IllegalArgumentException("Demand is null");
        }
        WarehouseRegion region = new WarehouseRegion(WarehouseRegion.Companion.getRostovId(), "Rostov",
            WarehouseRegion.Companion.getMoscowId());
        Warehouse warehouse = new Warehouse(147L, "Ростов 147", WarehouseType.FULFILLMENT, region,
            10000985804L);
        demand.setWarehouse(warehouse);
    }

    private static DemandDTO createMskDemand(DemandType type, long id, long supplierId, BigDecimal sum,
                                             LocalDate orderDate) {
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        supplier.setName("Supplier #" + supplierId);
        supplier.setRsId(Objects.toString(supplierId));

        DemandDTO demand = new DemandDTO();
        demand.setDemandType(type);
        demand.setId(id);
        demand.setSupplier(supplier);
        demand.setSum(sum);
        demand.setOrderDate(orderDate);
        demand.setCatman("boris");
        demand.setSupplyRoute(SupplyRouteType.DIRECT.getInternalName());

        setMskWarehouse(demand);

        return demand;
    }

    private static RecommendationNew createRecommendation(DemandDTO demand, int qty, Double price, boolean isEBP,
                                                          String reason, Recommendation.Filter filter) {
        RecommendationNew r = new RecommendationNew();
        r.setDemandId(demand.getId() == null ? -1L : demand.getId());
        r.setAdjustedPurchQty(qty);
        r.setPurchQty(qty);
        r.setImportPrice(price);
        r.setFilter(filter);
        r.setNeedsManualReview(isEBP);
        r.setNeedsManualReviewCause(isEBP ? reason : null);
        return r;
    }

    private static RecommendationNew createRecommendationWithoutPrice(DemandDTO demand,
                                                                      EbpCalculatorService.Reasons reason) {
        RecommendationNew r = new RecommendationNew();
        r.setDemandId(demand.getId() == null ? -1L : demand.getId());
        r.setAdjustedPurchQty(1);
        r.setPurchQty(1);
        r.setImportPrice(0d);
        r.setNeedsManualReview(true);
        r.setNeedsManualReviewCause(reason.getLabel());
        return r;
    }

    @Before
    public void mockBeforeTest() {
        setTestTime(NOW_DATE_TIME);
        featureFlagService = Mockito.mock(FeatureFlagService.class);
        dbDemandService = Mockito.mock(DbDemandService.class);
        warehouseService = Mockito.mock(WarehouseService.class);

        when(featureFlagService.consolidatedSupplyForAutoExportEnabled()).thenReturn(false);

        when(dbDemandService.getAutoprocessingLogParams(true))
            .thenReturn(Set.of(
                new LogisticsParamKey(4L, 145, "boris", SupplyRouteType.DIRECT),
                new LogisticsParamKey(4L, 145, "catman2", SupplyRouteType.DIRECT),
                new LogisticsParamKey(5L, 145, "catman1", SupplyRouteType.DIRECT),
                new LogisticsParamKey(5L, 145, "catman2", SupplyRouteType.DIRECT),
                new LogisticsParamKey(5L, 147, "catman2", SupplyRouteType.DIRECT),
                new LogisticsParamKey(5L, 147, null, SupplyRouteType.DIRECT),

                new LogisticsParamKey(1L, 145, null, SupplyRouteType.DIRECT),
                new LogisticsParamKey(2L, 145, null, SupplyRouteType.DIRECT),
                new LogisticsParamKey(2L, 147, null, SupplyRouteType.DIRECT),
                new LogisticsParamKey(3L, 145, null, SupplyRouteType.DIRECT),

                new LogisticsParamKey(2L, 145, ULTRA_PILOT_USER, SupplyRouteType.XDOC),
                new LogisticsParamKey(2L, 147, ULTRA_PILOT_USER, SupplyRouteType.XDOC)
            ));
        when(dbDemandService.getAutoprocessSpecialIgnoreSupplierIdsWithCatman())
            .thenReturn(Map.of(
                4L, Set.of("boris", "catman1")
            ));
        when(dbDemandService.getAutoprocessSpecialIgnoreSupplierIds())
            .thenReturn(Set.of(2L));

        var regionMsk = new WarehouseRegion();
        regionMsk.setId(WarehouseRegion.Companion.getMoscowId());
        var regionRostov = new WarehouseRegion();
        regionRostov.setId(WarehouseRegion.Companion.getRostovId());
        when(warehouseService.getAllWarehouses())
            .thenReturn(List.of(
                new Warehouse(145L, "msk", WarehouseType.FULFILLMENT, regionMsk, 0L),
                new Warehouse(147L, "rostov", WarehouseType.FULFILLMENT, regionRostov, 0L)
            ));
        demandAutoprocessingFlagHandler = new DemandAutoprocessingFlagHandler(dbDemandService, timeService,
            warehouseService, environmentService, featureFlagService, autoprocessingValidationLogRepository);
    }

    @Test
    public void testSetAutoprocessing() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAsNotAutoprocessingByEbpScVeryHigh() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        RecommendationNew veryHighScRec =
            createRecommendationWithoutPrice(demand, EbpCalculatorService.Reasons.VERY_HIGH_SC);
        demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, veryHighScRec);
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetAutoprocessingByEbpScHigh() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        RecommendationNew veryHighScRec =
            createRecommendationWithoutPrice(demand, EbpCalculatorService.Reasons.HIGH_NO_COREFIX_SC);
        demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, veryHighScRec);
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAsNotAutoprocessingByEbpSumGreaterThresholdOfDemandSum() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), true)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAutoprocessingByEbpSumGreaterThresholdOfDemandSumButTransits() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            EbpCalculatorService.Reasons.TRANSIT_WARNINGS.getLabel(), Recommendation.Filter.TRANSIT_WARNING)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAutoprocessingByEbpSumGreaterThresholdOfDemandSumButSpecialOrder() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            "special order test", Recommendation.Filter.SPECIAL_ORDER)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAutoprocessingBySpecialOrder() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        demand.setSpecial(true);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            "special order test", Recommendation.Filter.SPECIAL_ORDER)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetNoAutoprocessingXdock() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        demand.setSupplyRoute(SupplyRouteType.XDOC.getInternalName());
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            "special order test", Recommendation.Filter.SPECIAL_ORDER)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetAutoprocessingXdockForSpecialLogins() {
        var date = LocalDate.of(2022, 5, 21);
        var xdoc = new Warehouse(407L, "XDOC", WarehouseType.XDOC, new WarehouseRegion(145L, "", 145L), 42L);
        demandAutoprocessingFlagHandler.init();

        var d1 = createMskDemand(DemandType.TYPE_1P, 1L, 2L, DEMAND_SUM, NOW_DATE);
        d1.setSupplyRoute(SupplyRouteType.XDOC.getInternalName());
        d1.setXdocDate(date);
        d1.setCatman(ULTRA_PILOT_USER);
        d1.setWarehouseFrom(xdoc);
        createRecommendations(d1, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            "special order test", Recommendation.Filter.SPECIAL_ORDER)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(d1, r));

        var d2 = createMskDemand(DemandType.TYPE_1P, 2L, 2L, DEMAND_SUM, NOW_DATE);
        d2.setSupplyRoute(SupplyRouteType.XDOC.getInternalName());
        d2.setXdocDate(date);
        d2.setCatman(ULTRA_PILOT_USER);
        d2.setWarehouse(new Warehouse(147L, "Ростов", WarehouseType.FULFILLMENT, new WarehouseRegion(147L, "", 147L),
            43L));
        d2.setWarehouseFrom(xdoc);
        createRecommendations(d2, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            "special order test", Recommendation.Filter.SPECIAL_ORDER)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(d2, r));

        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();

        assertEquals("220521_2_407", d1.getConsolidatedSupply());
        assertTrue(d1.isAutoProcessing());
        assertEquals("220521_2_407", d2.getConsolidatedSupply());
        assertTrue(d2.isAutoProcessing());
    }

    @Test
    public void testSetAutoprocessingBySpecialOrderRostov() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        setRostovWarehouse(demand);
        demand.setSpecial(true);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            "special order test", Recommendation.Filter.SPECIAL_ORDER)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetNoAutoprocessingBySpecialOrderIgnoreList() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 2L, DEMAND_SUM, NOW_DATE);
        demand.setSpecial(true);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            "special order test", Recommendation.Filter.SPECIAL_ORDER)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetNoAutoprocessingBySpecialOrderIgnoreListWithCatman() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 4L, DEMAND_SUM, NOW_DATE);
        demand.setCatman("catman1");
        demand.setSpecial(true);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            "special order test", Recommendation.Filter.SPECIAL_ORDER)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetNoAutoprocessingBySpecialOrderIgnoreListWithoutCatman() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 4L, DEMAND_SUM, NOW_DATE);
        demand.setCatman("catman2");
        demand.setSpecial(true);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), true,
            "special order test", Recommendation.Filter.SPECIAL_ORDER)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAsNotAutoprocessingWithCatmanByNotAvailableRegion() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 4L, DEMAND_SUM, NOW_DATE);
        demand.setCatman("catman2");
        setRostovWarehouse(demand);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetAsAutoprocessingWithCatmanByAvailableRegion() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 5L, DEMAND_SUM, NOW_DATE);
        demand.setCatman("catman2");
        setRostovWarehouse(demand);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAsAutoprocessingWithNullCatmanByAvailableRegion() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 5L, DEMAND_SUM, NOW_DATE);
        demand.setCatman(null);
        setRostovWarehouse(demand);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAsNotAutoprocessingByNotAvailableRegion() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        demand.setCatman(null);
        setRostovWarehouse(demand);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetAsAutoprocessingByAvailableRegion() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 2L, DEMAND_SUM, NOW_DATE);
        setRostovWarehouse(demand);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAsAutoprocessingByNullCatman() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, NOW_DATE);
        demand.setCatman(null);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAsNotAutoprocessingBySupplierId() {
        final long notExistsSupplierId = 745L;
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, notExistsSupplierId, DEMAND_SUM, NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetNotAutoprocessingForLaterDate() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, BEFORE_NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetNotAutoprocessingForFutureDate() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 1L, DEMAND_SUM, AFTER_NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetAutoprocessingWithCatman_isOk() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 4L, DEMAND_SUM, NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testSetAutoprocessingNotToday() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 5L, DEMAND_SUM, AFTER_NOW_DATE);
        demand.setSpecial(true);
        demand.setDeliveryDate(NOW_DATE.plusDays(3));
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> {
                r.setDeliveryTime(5);
                demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r);
            });
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertTrue(demand.isAutoProcessing());
    }

    @Test
    public void testNotSetAutoprocessingNotToday() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 5L, DEMAND_SUM, AFTER_NOW_DATE);
        demand.setDeliveryDate(NOW_DATE.plusDays(3));
        demand.setSpecial(true);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> {
                r.setDeliveryTime(10);
                demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r);
            });
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    @Test
    public void testSetAutoprocessingWithCatman_isForbidden() {
        demandAutoprocessingFlagHandler.init();
        DemandDTO demand = createMskDemand(DemandType.TYPE_1P, 1L, 5L, DEMAND_SUM, NOW_DATE);
        createRecommendations(demand, splitSumOnQtyAndPrice(DEMAND_SUM), false)
            .forEach(r -> demandAutoprocessingFlagHandler.storeRecommendationForDemand(demand, r));
        demandAutoprocessingFlagHandler.setAutoprocessingFlagAndClear();
        assertFalse(demand.isAutoProcessing());
    }

    private List<RecommendationNew> createRecommendations(DemandDTO demand, Map<Integer, Double> qtyAndPrice,
                                                          boolean isEBP) {
        return qtyAndPrice.entrySet().stream()
            .map(e -> createRecommendation(demand, e.getKey(), e.getValue(), isEBP, "for test",
                Recommendation.Filter.NEW))
            .collect(Collectors.toList());
    }

    private List<RecommendationNew> createRecommendations(DemandDTO demand, Map<Integer, Double> qtyAndPrice,
                                                          boolean isEBP, String reason, Recommendation.Filter filter) {
        return qtyAndPrice.entrySet().stream()
            .map(e -> createRecommendation(demand, e.getKey(), e.getValue(), isEBP, reason, filter))
            .collect(Collectors.toList());
    }

    private Map<Integer, Double> splitSumOnQtyAndPrice(BigDecimal totalSum) {
        return splitSumOnQtyAndPrice(totalSum, new int[]{2, 3});
    }

    private Map<Integer, Double> splitSumOnQtyAndPrice(BigDecimal totalSum, int[] quantities) {
        final BigDecimal sumOfQuantities = BigDecimal.valueOf(Arrays.stream(quantities).sum());
        final double k = totalSum.divide(sumOfQuantities).doubleValue();
        return Arrays.stream(quantities).mapToObj(Integer::valueOf).collect(
            Collectors.groupingBy(Function.identity(), Collectors.summingDouble(qty -> k)));
    }
}

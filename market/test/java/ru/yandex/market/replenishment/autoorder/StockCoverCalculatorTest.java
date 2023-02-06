package ru.yandex.market.replenishment.autoorder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.dto.PartyInfoType;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.StockCoverType;
import ru.yandex.market.replenishment.autoorder.service.info_extractor.RecommendationExtendedInfoService;
import ru.yandex.market.replenishment.autoorder.utils.StockCoverCalculator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.new1pRecommendation;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.newRecommendation;

@ActiveProfiles("unittest")
public class StockCoverCalculatorTest extends FunctionalTest {

    @Autowired
    RecommendationExtendedInfoService recommendationExtendedInfoService;

    private static final int VERY_LONG_EXPIRY = 65535;

    @Test
    public void testNormalStockCover28() {
        final RecommendationNew recommendation = newRecommendation28(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(8, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(30, stockCoverForward.longValue());
    }

    @Test
    public void testNormalStockCover281p() {
        final RecommendationNew recommendation = newRecommendation281p(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward(PartyInfoType.ALL);
        assertNotNull(stockCoverBackward);
        assertEquals(8, stockCoverBackward.longValue());

        final Long stockCoverBackward1p = calc.backward(PartyInfoType.FIRST);
        assertNotNull(stockCoverBackward1p);
        assertEquals(9, stockCoverBackward1p.longValue());

        final Long stockCoverBackward3p = calc.backward(PartyInfoType.THIRD);
        assertNotNull(stockCoverBackward3p);
        assertEquals(22, stockCoverBackward3p.longValue());
    }

    @Test
    public void testStockCoverForward281p() {
        final RecommendationNew recommendation = newRecommendation281p(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverForward = calc.forward(PartyInfoType.ALL);
        assertNotNull(stockCoverForward);
        assertEquals(80, stockCoverForward.longValue());

        final Long stockCoverForward1p = calc.forward(PartyInfoType.FIRST);
        assertNotNull(stockCoverForward1p);
        assertEquals(123, stockCoverForward1p.longValue());

        final Long stockCoverForward3p = calc.forward(PartyInfoType.THIRD);
        assertNotNull(stockCoverForward3p);
        assertEquals(123, stockCoverForward3p.longValue());
    }

    @Test
    public void testStockCover28_respectPastTransits() {
        final RecommendationNew recommendation = newRecommendation28(DemandType.TYPE_1P);
        final LocalDate today = LocalDate.now();

        final Map<LocalDate, Long> transitsMaps = new HashMap<>();
        transitsMaps.put(today.minusDays(1), 10L);
        transitsMaps.put(today.plusDays(1), 10L);
        recommendation.setTransits(transitsMaps);

        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(8, stockCoverBackward.longValue());
        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(30, stockCoverForward.longValue());
    }

    @Test
    public void testTenderStockCover28() {
        final RecommendationNew recommendation = newRecommendation28(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(11, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(34, stockCoverForward.longValue());
    }

    @Test
    public void testStockCover28_emptyStocks_willUseOnlyAdjustedPurchQty() {
        final RecommendationNew recommendation = newRecommendation28(DemandType.TYPE_1P);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;

        recommendation.getStocksWithLifetimes().setStocksList(null);
        recommendation.getStocksWithLifetimes().setLifetimesList(null);
        recommendation.setDeliveryDate(null);
        recommendation.getWarehouseInfo().setTransit(0);
        recommendation.setTransits(Collections.emptyMap());

        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(5L, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(16L, stockCoverForward.longValue());
    }

    @Test
    public void testStockCover28_emptyStocks_hasFallbackTransit() {
        final RecommendationNew recommendation = newRecommendation28(DemandType.TYPE_1P);
        assert recommendation.getStocksWithLifetimes() != null;

        recommendation.getStocksWithLifetimes().setStocksList(null);
        recommendation.getStocksWithLifetimes().setLifetimesList(null);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(1));
        recommendation.setTransits(Collections.emptyMap());

        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(9, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(28, stockCoverForward.longValue());
    }

    private RecommendationNew newRecommendation28(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        final int[] salesAll = new int[] {0, 0, 0, 150, 150, 150, 150, 150};
        final double salesForecast = 50.0;
        final int transit = 20;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {10L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {VERY_LONG_EXPIRY});
        recommendation.getRegionInfo().setSalesForecast28days(salesForecast);
        recommendation.getRegionInfo().setSalesAll(salesAll);
        recommendation.getWarehouseInfo().setTransit(transit);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(10L);
        recommendation.getCountryInfo().setSalesForecast28days(salesForecast);
        recommendation.getCountryInfo().setSalesAll(salesAll);
        recommendation.getRegionInfo().setTransit(transit);

        recommendation.getRegionInfo().setMissedOrders28d(50.0);
        recommendation.setAdjustedPurchQty(30);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        Map<LocalDate, Long> transitsMaps = new HashMap<>();
        transitsMaps.put(LocalDate.now(), 10L);
        transitsMaps.put(LocalDate.now().plusDays(1), 10L);
        recommendation.setTransits(transitsMaps);

        return recommendation;
    }

    private RecommendationNew newRecommendation281p(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        final int[] salesAll = new int[] {0, 0, 0, 75, 75, 75, 75, 75};
        final int[] sales1p = new int[] {0, 0, 0, 50, 50, 50, 50, 50};

        final int transit = 20;
        final int transit1p = 10;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {10L});
        recommendation.getStocksWithLifetimes().setStocksList1p(new Long[] {5L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {VERY_LONG_EXPIRY});
        recommendation.getStocksWithLifetimes().setLifetimesList1p(new Integer[] {VERY_LONG_EXPIRY});

        recommendation.getRegionInfo().setSalesAll(salesAll);
        recommendation.getRegionInfo().setSales1p(sales1p);
        recommendation.getWarehouseInfo().setTransit(transit);
        recommendation.getWarehouseInfo().setTransit1p(transit1p);

        recommendation.getRegionInfo().setMissedOrders28d(50.0);
        recommendation.getRegionInfo().setMissedOrders28d1p(50.0);
        recommendation.setAdjustedPurchQty(30);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        recommendation.getRegionInfo().setOos28Days(14L);
        recommendation.getRegionInfo().setOosDays(14L);

        recommendation.getRegionInfo().setSalesForecast28days1p(10.0);
        recommendation.getRegionInfo().setSalesForecast28days(20.0);


        Map<LocalDate, Long> transitsMaps = new HashMap<>();
        transitsMaps.put(LocalDate.now(), 10L);
        transitsMaps.put(LocalDate.now().plusDays(1), 10L);
        recommendation.setTransits(transitsMaps);

        Map<LocalDate, Long> transitsMaps1p = new HashMap<>();
        transitsMaps1p.put(LocalDate.now(), 5L);
        transitsMaps1p.put(LocalDate.now().plusDays(1), 5L);
        recommendation.setTransits1p(transitsMaps1p);

        return recommendation;
    }

    @Test
    public void testNormalStockCover56() {
        final RecommendationNew recommendation = newRecommendation56(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(19, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(64, stockCoverForward.longValue());
    }

    @Test
    public void testTenderStockCover56() {
        final RecommendationNew recommendation = newRecommendation56(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(22, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(67, stockCoverForward.longValue());
    }

    private RecommendationNew newRecommendation56(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        final int[] salesAll = new int[] {0, 0, 0, 0, 150, 150, 150, 150};
        final double salesForecast = 50.0;
        final int transit = 20;

        final Map<LocalDate, Long> transitsMaps = new HashMap<>();
        transitsMaps.put(LocalDate.now(), (long) transit);
        recommendation.setTransits(transitsMaps);

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {10L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {VERY_LONG_EXPIRY});
        recommendation.getRegionInfo().setSalesForecast56days(salesForecast);
        recommendation.getRegionInfo().setSalesAll(salesAll);
        recommendation.getWarehouseInfo().setTransit(transit);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(10L);
        recommendation.getCountryInfo().setSalesForecast56days(salesForecast);
        recommendation.getCountryInfo().setSalesAll(salesAll);
        recommendation.getRegionInfo().setTransit(transit);

        recommendation.getRegionInfo().setMissedOrders56d(50.0);
        recommendation.setAdjustedPurchQty(30);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        return recommendation;
    }

    @Test
    public void testNormalStockCoverNoForecast() {
        final RecommendationNew recommendation = newRecommendationNoForecast(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(13, stockCoverBackward.longValue());

        assertNull(calc.forward());
    }

    @Test
    public void testTenderStockCoverNoForecast() {
        final RecommendationNew recommendation = newRecommendationNoForecast(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(17, stockCoverBackward.longValue());

        assertNull(calc.forward());
    }

    private RecommendationNew newRecommendationNoForecast(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {10L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {VERY_LONG_EXPIRY});
        recommendation.getRegionInfo().setSalesAll(new int[] {0, 0, 0, 100, 100, 100, 100, 100});
        recommendation.getWarehouseInfo().setTransit(20);

        var transitsMaps = new HashMap<LocalDate, Long>();
        transitsMaps.put(LocalDate.now(), 20L);
        recommendation.setTransits(transitsMaps);

        // Для тендеров
        recommendation.getCountryInfo().setSalesAll(new int[] {0, 0, 0, 100, 100, 100, 100, 100});
        recommendation.getRegionInfo().setStockOverall(10L);
        recommendation.getRegionInfo().setTransit(20);

        recommendation.setAdjustedPurchQty(30);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));
        return recommendation;
    }

    @Test
    public void testStockCover28WithExpiringStocks() {
        final RecommendationNew recommendation = new1pRecommendation();
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getRegionInfo() != null;

        // 10 продаж в день
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {100000L, 20L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {9, VERY_LONG_EXPIRY});
        recommendation.getRegionInfo().setSalesAll(new int[] {0, 0, 0, 280, 200, 200, 200, 200});
        recommendation.getRegionInfo().setSalesForecast28days(280.0);
        recommendation.getRegionInfo().setMissedOrders28d(80.0);
        recommendation.getWarehouseInfo().setTransit(0);
        recommendation.setAdjustedPurchQty(10);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(9, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(9, stockCoverForward.longValue());
    }

    @Test
    public void testStockCover56WithExpiringStocks() {
        final RecommendationNew recommendation = new1pRecommendation();
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getRegionInfo() != null;

        // 10 продаж в день
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {100000L, 20L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {9, VERY_LONG_EXPIRY});
        recommendation.getRegionInfo().setSalesAll(new int[] {0, 0, 0, 0, 560, 500, 500, 500});
        recommendation.getRegionInfo().setSalesForecast56days(560.0);
        recommendation.getRegionInfo().setMissedOrders28d(60.0);
        recommendation.getWarehouseInfo().setTransit(0);
        recommendation.setAdjustedPurchQty(10);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(9, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(9, stockCoverForward.longValue());
    }

    @Test
    public void testNormalForwardZeroForecastAndOrders28() {
        final RecommendationNew recommendation = newRecommendationZeroForecastAndOrders28(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        assertNull(calc.backward());
        assertNull(calc.forward());
    }

    @Test
    public void testTenderForwardZeroForecastAndOrders28() {
        final RecommendationNew recommendation = newRecommendationZeroForecastAndOrders28(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        assertNull(calc.backward());
        assertNull(calc.forward());
    }

    private RecommendationNew newRecommendationZeroForecastAndOrders28(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {});
        recommendation.getRegionInfo().setSalesAll(new int[] {0, 0, 0, 0, 0, 0, 0, 0});
        recommendation.getRegionInfo().setSalesForecast28days(0.0);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(0);
        recommendation.getCountryInfo().setSalesAll(new int[] {0, 0, 0, 0, 0, 0, 0, 0});
        recommendation.getCountryInfo().setSalesForecast28days(0.0);

        return recommendation;
    }

    @Test
    public void testNormalForwardZeroForecastAndOrders56() {
        final RecommendationNew recommendation = newRecommendationZeroForecastAndOrders56(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        assertNull(calc.backward());
        assertNull(calc.forward());
    }

    @Test
    public void testTenderForwardZeroForecastAndOrders56() {
        final RecommendationNew recommendation = newRecommendationZeroForecastAndOrders56(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        assertNull(calc.backward());
        assertNull(calc.forward());
    }

    private RecommendationNew newRecommendationZeroForecastAndOrders56(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {});
        recommendation.getRegionInfo().setSalesAll(new int[] {0, 0, 0, 0, 0, 0, 0, 0});
        recommendation.getRegionInfo().setSalesForecast56days(0.0);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(0);
        recommendation.getCountryInfo().setSalesAll(new int[] {0, 0, 0, 0, 0, 0, 0, 0});
        recommendation.getCountryInfo().setSalesForecast56days(0.0);

        return recommendation;
    }

    @Test
    public void testNormalQuantityByStockCoverBackward() {
        final RecommendationNew recommendation = newRecommendationForQuantityByStockCover(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        assertEquals(6L, calc.quantityByStockCoverBackward(50));
    }

    @Test
    public void testTenderQuantityByStockCoverBackward() {
        final RecommendationNew recommendation = newRecommendationForQuantityByStockCover(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        assertEquals(4L, calc.quantityByStockCoverBackward(28));
    }

    @Test
    public void testNormalQuantityByStockCoverForward() {
        final RecommendationNew recommendation = newRecommendationForQuantityByStockCover(DemandType.TYPE_1P);
        recommendation.getRegionInfo().setSalesForecast56days(50.0);
        recommendation.getRegionInfo().setMissedOrders28d(10.0);

        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        assertEquals(34L, calc.quantityByStockCover(50, StockCoverType.FORWARD));
    }

    private RecommendationNew newRecommendationForQuantityByStockCover(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {10L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {VERY_LONG_EXPIRY});
        recommendation.getRegionInfo().setSalesAll(new int[] {0, 0, 0, 10, 10, 10, 10, 10});
        recommendation.getWarehouseInfo().setTransit(3);

        // Для тендеров
        recommendation.getCountryInfo().setSalesAll(new int[] {0, 0, 0, 10, 10, 10, 10, 10});
        recommendation.getRegionInfo().setStockOverall(5L);
        recommendation.getRegionInfo().setTransit(3);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        final Map<LocalDate, Long> transitsMaps = new HashMap<>();
        transitsMaps.put(LocalDate.now(), 3L);
        recommendation.setTransits(transitsMaps);

        recommendation.setShipmentQuantum(1L);
        recommendation.setDeliveryTime(7);
        return recommendation;
    }

    // @NOTE: этот тест нужен только для дебага RecommendationFilteringTest
    @Test
    public void recommendationFilteringDebugTest() {
        final RecommendationNew recommendation = newRecommendation(DemandType.TYPE_1P);
        assert recommendation.getStocksWithLifetimes() != null;
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {10L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {65535});

        assert recommendation.getRegionInfo() != null;
        recommendation.getRegionInfo().setSalesAll(new int[] {0, 0, 0, 150, 150, 150, 150, 150});
        recommendation.getRegionInfo().setSalesForecast28days(50.0);
        recommendation.getRegionInfo().setMissedOrders28d(50.0);
        recommendation.getRegionInfo().setStockOverall(10L);

        assert recommendation.getWarehouseInfo() != null;
        recommendation.getWarehouseInfo().setTransit(20);
        recommendation.setAdjustedPurchQty(30);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(2));
        recommendation.setGoalMsku(false);

        Map<LocalDate, Long> transitsMaps = new HashMap<>();
        transitsMaps.put(LocalDate.now(), 20L);
        recommendation.setTransits(transitsMaps);

        final StockCoverCalculator calc = new StockCoverCalculator(
            recommendationExtendedInfoService,
            timeService,
            recommendation,
            true
        );

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(9, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(31, stockCoverForward.longValue());
    }

    @Test
    public void testStockCoverWithoutTransit28() {
        final RecommendationNew recommendation = newRecommendationWithoutTransit28(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(6, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(19, stockCoverForward.longValue());
    }

    @Test
    public void testTenderStockCoverWithoutTransit28() {
        final RecommendationNew recommendation = newRecommendationWithoutTransit28(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(7, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(22, stockCoverForward.longValue());
    }

    private RecommendationNew newRecommendationWithoutTransit28(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        final int[] salesAll = new int[] {0, 0, 0, 150, 150, 150, 150, 150};
        final double salesForecast = 50.0;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {10L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {VERY_LONG_EXPIRY});
        recommendation.getRegionInfo().setSalesForecast28days(salesForecast);
        recommendation.getRegionInfo().setSalesAll(salesAll);
        recommendation.getWarehouseInfo().setTransit(0);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(10L);
        recommendation.getCountryInfo().setSalesForecast28days(salesForecast);
        recommendation.getCountryInfo().setSalesAll(salesAll);
        recommendation.getRegionInfo().setTransit(0);

        recommendation.getRegionInfo().setMissedOrders28d(50.0);
        recommendation.setAdjustedPurchQty(30);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        recommendation.setTransits(Collections.emptyMap());

        return recommendation;
    }

    @Test
    public void testStockCoverWithoutTransit56() {
        final RecommendationNew recommendation = newRecommendationWithoutTransit56(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, false);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(9, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(19, stockCoverForward.longValue());
    }

    @Test
    public void testTenderStockCoverWithoutTransit56() {
        final RecommendationNew recommendation = newRecommendationWithoutTransit56(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(11, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(22, stockCoverForward.longValue());
    }

    private RecommendationNew newRecommendationWithoutTransit56(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        final int[] salesAll = new int[] {0, 100, 100, 100, 100, 100, 100, 100};
        final double salesForecast = 100.0;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(new Long[] {10L});
        recommendation.getStocksWithLifetimes().setLifetimesList(new Integer[] {VERY_LONG_EXPIRY});
        recommendation.getRegionInfo().setSalesForecast56days(salesForecast);
        recommendation.getRegionInfo().setSalesAll(salesAll);
        recommendation.getWarehouseInfo().setTransit(0);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(10L);
        recommendation.getCountryInfo().setSalesForecast56days(salesForecast);
        recommendation.getCountryInfo().setSalesAll(salesAll);
        recommendation.getRegionInfo().setTransit(0);

        recommendation.getRegionInfo().setMissedOrders56d(50.0);
        recommendation.setAdjustedPurchQty(30);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        recommendation.setTransits(Collections.emptyMap());

        return recommendation;
    }

    @Test
    public void testStockCoverTransitOnly28() {
        final RecommendationNew recommendation = newRecommendationTransitOnly28(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(2, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(8, stockCoverForward.longValue());
    }

    @Test
    public void testStockCoverTransitAfterOOS() {
        final RecommendationNew recommendation = newRecommendationTransitOnly28(DemandType.TYPE_1P);
        recommendation.getTransits().put(LocalDate.now().plusDays(28), 100L);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(2, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(8, stockCoverForward.longValue());
    }

    @Test
    public void testTenderStockCoverTransitOnly28() {
        final RecommendationNew recommendation = newRecommendationTransitOnly28(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(10, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(22, stockCoverForward.longValue());
    }

    private RecommendationNew newRecommendationTransitOnly28(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        final int[] salesAll = new int[] {0, 0, 0, 110, 110, 110, 110, 110};
        final double salesForecast = 50.0;
        final int transit = 30;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(null);
        recommendation.getStocksWithLifetimes().setLifetimesList(null);
        recommendation.getRegionInfo().setSalesForecast28days(salesForecast);
        recommendation.getRegionInfo().setSalesAll(salesAll);
        recommendation.getWarehouseInfo().setTransit(transit);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(10L);
        recommendation.getCountryInfo().setSalesForecast28days(salesForecast);
        recommendation.getCountryInfo().setSalesAll(salesAll);
        recommendation.getRegionInfo().setTransit(transit);

        recommendation.getRegionInfo().setMissedOrders28d(10.0);
        recommendation.setAdjustedPurchQty(0);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        Map<LocalDate, Long> transitsMaps = new HashMap<>();
        transitsMaps.put(LocalDate.now(), 10L);
        transitsMaps.put(LocalDate.now().plusDays(1), 10L);
        recommendation.setTransits(transitsMaps);

        return recommendation;
    }

    @Test
    public void testStockCoverTransitOnly56() {
        final RecommendationNew recommendation = newRecommendationTransitOnly56(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(2, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(19, stockCoverForward.longValue());
    }

    @Test
    public void testTenderStockCoverTransitOnly56() {
        final RecommendationNew recommendation = newRecommendationTransitOnly56(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNotNull(stockCoverBackward);
        assertEquals(31, stockCoverBackward.longValue());

        final Long stockCoverForward = calc.forward();
        assertNotNull(stockCoverForward);
        assertEquals(123, stockCoverForward.longValue());
    }

    private RecommendationNew newRecommendationTransitOnly56(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        final int[] salesAll = new int[] {0, 0, 100, 100, 100, 100, 100, 100};
        final double salesForecast = 50.0;
        final int transit = 100;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(null);
        recommendation.getStocksWithLifetimes().setLifetimesList(null);
        recommendation.getRegionInfo().setSalesForecast56days(salesForecast);
        recommendation.getRegionInfo().setSalesAll(salesAll);
        recommendation.getWarehouseInfo().setTransit(transit);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(10L);
        recommendation.getCountryInfo().setSalesForecast56days(salesForecast);
        recommendation.getCountryInfo().setSalesAll(salesAll);
        recommendation.getRegionInfo().setTransit(transit);

        recommendation.getRegionInfo().setMissedOrders56d(10.0);
        recommendation.setAdjustedPurchQty(0);
        recommendation.setDeliveryDate(LocalDate.now().plusDays(3));

        Map<LocalDate, Long> transitsMaps = new HashMap<>();
        transitsMaps.put(LocalDate.now(), 10L);
        transitsMaps.put(LocalDate.now().plusDays(1), 10L);
        recommendation.setTransits(transitsMaps);

        return recommendation;
    }

    @Test
    public void testStockCoverDeadStock28() {
        final RecommendationNew recommendation = newRecommendationDeadStock28(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNull(stockCoverBackward);

        final Long stockCoverForward = calc.forward();
        assertNull(stockCoverForward);
    }

    @Test
    public void testTenderStockCoverDeadStock28() {
        final RecommendationNew recommendation = newRecommendationDeadStock28(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNull(stockCoverBackward);

        final Long stockCoverForward = calc.forward();
        assertNull(stockCoverForward);
    }

    private RecommendationNew newRecommendationDeadStock28(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        final int[] salesAll = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
        final double salesForecast = 0.0;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(null);
        recommendation.getStocksWithLifetimes().setLifetimesList(null);
        recommendation.getRegionInfo().setSalesForecast28days(salesForecast);
        recommendation.getRegionInfo().setSalesAll(salesAll);
        recommendation.getWarehouseInfo().setTransit(0);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(0L);
        recommendation.getCountryInfo().setSalesForecast28days(salesForecast);
        recommendation.getCountryInfo().setSalesAll(salesAll);
        recommendation.getRegionInfo().setTransit(0);

        recommendation.getRegionInfo().setMissedOrders28d(0.0);
        recommendation.setAdjustedPurchQty(0);
        recommendation.setTransits(Collections.emptyMap());

        return recommendation;
    }

    @Test
    public void testStockCoverDeadStock56() {
        final RecommendationNew recommendation = newRecommendationDeadStock56(DemandType.TYPE_1P);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNull(stockCoverBackward);

        final Long stockCoverForward = calc.forward();
        assertNull(stockCoverForward);
    }

    @Test
    public void testTenderStockCoverDeadStock56() {
        final RecommendationNew recommendation = newRecommendationDeadStock56(DemandType.TENDER);
        final StockCoverCalculator calc = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, recommendation, true);

        final Long stockCoverBackward = calc.backward();
        assertNull(stockCoverBackward);

        final Long stockCoverForward = calc.forward();
        assertNull(stockCoverForward);
    }

    private RecommendationNew newRecommendationDeadStock56(DemandType demandType) {
        final RecommendationNew recommendation = newRecommendation(demandType);
        assert recommendation.getStocksWithLifetimes() != null;
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getCountryInfo() != null;
        assert recommendation.getRegionInfo() != null;

        final int[] salesAll = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
        final double salesForecast = 0.0;

        // Для 1Р рекомендаций
        recommendation.getStocksWithLifetimes().setStocksList(null);
        recommendation.getStocksWithLifetimes().setLifetimesList(null);
        recommendation.getRegionInfo().setSalesForecast56days(salesForecast);
        recommendation.getRegionInfo().setSalesAll(salesAll);
        recommendation.getWarehouseInfo().setTransit(0);

        // Для тендеров
        recommendation.getRegionInfo().setStockOverall(0L);
        recommendation.getCountryInfo().setSalesForecast56days(salesForecast);
        recommendation.getCountryInfo().setSalesAll(salesAll);
        recommendation.getRegionInfo().setTransit(0);

        recommendation.getRegionInfo().setMissedOrders28d(0.0);
        recommendation.setAdjustedPurchQty(0);
        recommendation.setTransits(Collections.emptyMap());

        return recommendation;
    }

}

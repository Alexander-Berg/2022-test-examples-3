package ru.yandex.market.replenishment.autoorder;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.ABC;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.WarehouseRegion;
import ru.yandex.market.replenishment.autoorder.service.EbpCalculatorService;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.info_extractor.RecommendationExtendedInfoService;
import ru.yandex.market.replenishment.autoorder.utils.StockCoverCalculator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.new1pRecommendation;

@ActiveProfiles("unittest")
public class EbpCalculatorTest extends FunctionalTest {

    @Autowired
    EbpCalculatorService ebpCalculatorService;

    @Autowired
    RecommendationExtendedInfoService recommendationExtendedInfoService;

    @Autowired
    TimeService timeService;

    @Test
    public void categoryIsNew() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAbc(ABC.NEW);
        replenishment.getWarehouseInfo().setTransit(2);
        replenishment.setMinShipment(10);
        assertEquals("Новинка без пополнения", ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void promoSales() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAbc(ABC.B);
        replenishment.setSalePromoStart(LocalDate.of(2021, 12, 16));
        replenishment.setSalePromoEnd(LocalDate.of(2021, 12, 20));
        replenishment.getRegionInfo().setStockOverall(1);
        assertEquals("Промо и SC<28", ebpCalculatorService.needsManualReview(replenishment, 25L, 25L));
    }

    @Test
    public void promoPurchase() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAbc(ABC.A);
        replenishment.setPurchasePromoStart(LocalDate.of(2021, 12, 16));
        replenishment.setPurchasePromoEnd(LocalDate.of(2021, 12, 20));
        replenishment.getWarehouseInfo().setTransit(2);
        assertEquals("Промо и SC<28", ebpCalculatorService.needsManualReview(replenishment, 27L, 27L));
    }

    @Test
    public void goodSC() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.getRegionInfo().setOosDays(1000L);
        replenishment.getRegionInfo().setStockOverall(1);
        assertNull(ebpCalculatorService.needsManualReview(replenishment, 25L, 25L));
    }



    @Test
    public void regionNoGoalNoRec() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setGoalMsku(false);
        replenishment.setWarehouseRegionId(WarehouseRegion.Companion.getRostovId());
        replenishment.setAdjustedPurchQty(0);
        assertNull(ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void noRecLowSells() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(0);
        replenishment.setMinShipment(10);
        assertEquals("OOS", ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void noRecNoStockWithOos() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(0);
        replenishment.getRegionInfo().setStockOverall(0);
        replenishment.getRegionInfo().setOosDays(16L);
        assertEquals("OOS", ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void noRecNoStockNoTransitsWithOos() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(0);
        replenishment.getRegionInfo().setStockOverall(0);
        replenishment.getRegionInfo().setOosDays(16L);
        replenishment.getWarehouseInfo().setTransit(0);
        assertEquals("OOS", ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void noRecNoStockWithLowScb() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(0);
        replenishment.getRegionInfo().setOosDays(4L);
        replenishment.getRegionInfo().setStockOverall(1);
        assertEquals("SC<21", ebpCalculatorService.needsManualReview(replenishment, 0L, 20L));
    }

    @Test
    public void noRecWithLowScf() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(0);
        replenishment.getRegionInfo().setOosDays(4L);
        replenishment.getRegionInfo().setStockOverall(1);
        assertEquals("SC<21", ebpCalculatorService.needsManualReview(replenishment, 20L, 0L));
    }

    @Test
    public void noRecNoLowScf() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(0);
        replenishment.getRegionInfo().setOosDays(4L);
        replenishment.getRegionInfo().setStockOverall(4L);
        assertEquals("SC<21", ebpCalculatorService.needsManualReview(replenishment, 10L, 10L));
    }

    @Test
    public void lowSellsWithStockWithRec() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(11);
        replenishment.getRegionInfo().setStockOverall(4L);
        replenishment.getRegionInfo().setTransit(0);
        replenishment.setMinShipment(10);
        replenishment.getRegionInfo().setSalesAll(new int[]{0, 0, 0, 5, 5, 5, 5});
        assertEquals("Пополняем товар с низкими продажами", ebpCalculatorService.needsManualReview(replenishment, 20L,
            20L));
    }

    @Test
    public void lowSellsWithTransitWithRec() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(1);
        replenishment.getRegionInfo().setStockOverall(0L);
        replenishment.getRegionInfo().setSalesAll(new int[]{0, 0, 0, 5, 5, 5, 5, 5});
        replenishment.getWarehouseInfo().setTransit(40);
        replenishment.setMinShipment(10);
        assertEquals("Пополняем товар с низкими продажами", ebpCalculatorService.needsManualReview(replenishment, 20L,
            20L));
    }

    @Test
    public void highNoCorefixScbWithRec() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(1);
        assertEquals("Рекомендация с SC>52", ebpCalculatorService.needsManualReview(replenishment, 55L, 55L));
    }


    @Test
    public void highScbWithRecWithPromoSales() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(1);
        replenishment.setSalePromoStart(LocalDate.of(2021, 12, 16));
        replenishment.setSalePromoEnd(LocalDate.of(2021, 12, 20));
        assertNull(ebpCalculatorService.needsManualReview(replenishment, 40L, 10L));
    }

    @Test
    public void highScbWithRecWithPromoPurchase() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(1);
        replenishment.setPurchasePromoStart(LocalDate.of(2021, 12, 16));
        replenishment.setPurchasePromoEnd(LocalDate.of(2021, 12, 20));
        assertNull(ebpCalculatorService.needsManualReview(replenishment, 40L, 10L));
    }

    @Test
    public void highScbWithRecWithPrebild() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(1);
        replenishment.setMinItems(1L);
        assertNull(ebpCalculatorService.needsManualReview(replenishment, 40L, 10L));
    }

    @Test
    public void highNoCorefixScWithRec() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(1);
        assertEquals("Рекомендация с SC>52", ebpCalculatorService.needsManualReview(replenishment, 55L, 55L));
    }

    @Test
    public void lowCorefixScWithRec() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(1);
        replenishment.setCoreFixMatrix(true);
        replenishment.setSafetyStock(50d);
        assertEquals("Corefix и SC<SS", ebpCalculatorService.needsManualReview(replenishment, 45L, 45L));
    }

    @Test
    public void veryHighScWithRec() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(1);
        assertEquals("Рекомендация с SC>168", ebpCalculatorService.needsManualReview(replenishment, 170L, 170L));
    }

    @Test
    public void veryHighScMoq() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(10);
        replenishment.setMinShipment(10);
        assertNull(ebpCalculatorService.needsManualReview(replenishment, 170L, 170L));
    }

    @Test
    public void oosMore14dWithAdj() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getRegionInfo() != null;

        replenishment.setAbc(ABC.A);
        replenishment.setPurchQty(10);
        replenishment.setAdjustedPurchQty(0);
        replenishment.getRegionInfo().setOosDays(15L);

        assertEquals("OOS", ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void oosMore14dWithRec() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getRegionInfo() != null;

        replenishment.setAbc(ABC.A);
        replenishment.setPurchQty(10);
        replenishment.setAdjustedPurchQty(10);
        replenishment.getRegionInfo().setOosDays(15L);

        assertNull(ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void highSc() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAbc(ABC.A);
        replenishment.setPurchQty(10);
        replenishment.setAdjustedPurchQty(10);

        assertEquals("Рекомендация с SC>168", ebpCalculatorService.needsManualReview(replenishment, 300L, 300L));
    }

    @Test
    public void highScMultipleSc168() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getCountryInfo() != null;

        replenishment.setAbc(ABC.A);
        replenishment.setPurchQty(10);
        replenishment.setAdjustedPurchQty(10);
        replenishment.getCountryInfo().setSalesAll(new int[]{0, 0, 0, 50, 50});
        replenishment.setDeliveryDate(LocalDate.now());

        assertEquals("Рекомендация с SC>168", ebpCalculatorService.needsManualReview(replenishment, 300L, 300L));

        RecommendationNew replenishment2 = getMoscow1pRecommendation();
        assert replenishment2.getCountryInfo() != null;

        replenishment2.setAbc(ABC.A);
        replenishment2.getCountryInfo().setSalesAll(new int[]{0, 0, 0, 50, 50});
        replenishment2.setPurchQty(10);
        replenishment2.setAdjustedPurchQty(10);
        replenishment2.getStocksWithLifetimes().setStocksList(new Long[]{20L});
        replenishment2.getStocksWithLifetimes().setLifetimesList(new Integer[]{100});

        testDontNeedReview(replenishment, replenishment2);
    }

    @Test
    public void highScMultipleSc56() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getCountryInfo() != null;

        replenishment.setAbc(ABC.A);
        replenishment.setPurchQty(10);
        replenishment.setAdjustedPurchQty(10);
        replenishment.getCountryInfo().setSalesAll(new int[]{0, 0, 0, 50, 50});
        replenishment.setDeliveryDate(LocalDate.now());

        assertEquals("Рекомендация с SC>168", ebpCalculatorService.needsManualReview(replenishment, 300L, 300L));

        RecommendationNew replenishment2 = getMoscow1pRecommendation();
        assert replenishment2.getCountryInfo() != null;

        replenishment2.setAbc(ABC.A);
        replenishment2.getCountryInfo().setSalesAll(new int[]{0, 0, 0, 50, 50});
        replenishment2.setPurchQty(10);
        replenishment2.setAdjustedPurchQty(10);
        replenishment2.getStocksWithLifetimes().setStocksList(new Long[]{20L});
        replenishment2.getStocksWithLifetimes().setLifetimesList(new Integer[]{100});

        testDontNeedReview(replenishment, replenishment2);
    }

    private void testDontNeedReview(RecommendationNew replenishment, RecommendationNew replenishment2) {
        ebpCalculatorService.setEbpForMultiReplenishments(Arrays.asList(replenishment, replenishment2));
        assertNull(replenishment.getNeedsManualReviewCause());
        assertFalse(replenishment.isNeedsManualReview());
        assertNull(replenishment2.getNeedsManualReviewCause());
        assertFalse(replenishment2.isNeedsManualReview());
    }

    @Test
    public void noOrdersAndStockPositiveAndQtyPositive() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getWarehouseInfo() != null;

        replenishment.setAbc(ABC.A);
        replenishment.getRegionInfo().setStockOverall(2L);
        replenishment.setPurchQty(5);
        replenishment.setAdjustedPurchQty(5);
        replenishment.setMinShipment(1);
        var stockCoverCalculator = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, replenishment, true);
        assertEquals(
            "Пополняем товар с низкими продажами",
            ebpCalculatorService.needsManualReview(replenishment, stockCoverCalculator.backward(),
                stockCoverCalculator.forward())
        );
    }

    @Test
    public void noSales() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAbc(ABC.A);
        replenishment.setPurchQty(1);
        replenishment.setAdjustedPurchQty(1);
        replenishment.setMinShipment(1);
        replenishment.getWarehouseInfo().setReplenishmentResultTransit1p(2);
        var stockCoverCalculator = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, replenishment, true);

        assertEquals(
            "Пополняем товар с низкими продажами",
            ebpCalculatorService.needsManualReview(replenishment, stockCoverCalculator.backward(),
                stockCoverCalculator.forward())
        );
    }

    @Test
    public void noOrdersWithStock() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getRegionInfo() != null;

        replenishment.setAbc(ABC.A);
        replenishment.getRegionInfo().setStockOverall(1L);

        var stockCoverCalculator = new StockCoverCalculator(recommendationExtendedInfoService,
            timeService, replenishment, true);

        assertNull(ebpCalculatorService.needsManualReview(replenishment, stockCoverCalculator.backward(),
            stockCoverCalculator.forward()));
    }

    @Test
    public void lowSc() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAbc(ABC.A);
        replenishment.getRegionInfo().setStockOverall(1);
        assertEquals("SC<21", ebpCalculatorService.needsManualReview(replenishment, 3L, 3L));
    }

    @Test
    public void salesSuccess1() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getRegionInfo() != null;

        replenishment.setAbc(ABC.A);
        replenishment.setPurchQty(10);
        replenishment.setAdjustedPurchQty(10);
        replenishment.getRegionInfo().setActualSales(0L);

        assertNull(ebpCalculatorService.needsManualReview(replenishment, 10L, 10L));
    }

    @Test
    public void salesSuccess2() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getRegionInfo() != null;

        replenishment.setAbc(ABC.A);
        replenishment.setPurchQty(10);
        replenishment.setAdjustedPurchQty(10);
        replenishment.getRegionInfo().setActualSales(10L);

        assertNull(ebpCalculatorService.needsManualReview(replenishment, 10L, 10L));
    }

    @Test
    public void fitSuccess1() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getRegionInfo() != null;

        replenishment.setAbc(ABC.C);
        replenishment.setPurchQty(10);
        replenishment.setAdjustedPurchQty(10);
        replenishment.getRegionInfo().setActualFit(0L);

        assertNull(ebpCalculatorService.needsManualReview(replenishment, 10L, 10L));
    }

    @Test
    public void fitSuccess2() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        assert replenishment.getRegionInfo() != null;

        replenishment.setAbc(ABC.D);
        replenishment.setPurchQty(10);
        replenishment.setAdjustedPurchQty(10);
        replenishment.getRegionInfo().setActualFit(10L);

        assertNull(ebpCalculatorService.needsManualReview(replenishment, 10L, 10L));
    }

    @Test
    public void purchQtyIsZeroAndSCMoreWeek() {
        // recommendation = 0 and SC > 7
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setAdjustedPurchQty(0);
        replenishment.getRegionInfo().setStockOverall(1);
        replenishment.getWarehouseInfo().setTransit(2);
        assertNull(ebpCalculatorService.needsManualReview(replenishment, 10L, 10L));
    }

    @Test
    public void testNeedManualReview_goalIsTrueRegionIsMoscowGetsEbp() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setGoalMsku(true);
        replenishment.setWarehouseRegionId(WarehouseRegion.Companion.getMoscowId());
        replenishment.getRegionInfo().setStockOverall(1);

        assertNotNull(ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void testNeedManualReview_goalIsTrueRegionIsRostovGetsEbp() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setGoalMsku(true);
        replenishment.setWarehouseRegionId(WarehouseRegion.Companion.getRostovId());
        replenishment.getRegionInfo().setStockOverall(1);
        assertNotNull(ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void testNeedManualReview_goalIsFalseRegionIsMoscowGetsEbp() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setGoalMsku(false);
        replenishment.setWarehouseRegionId(WarehouseRegion.Companion.getMoscowId());
        replenishment.getRegionInfo().setStockOverall(1);
        assertNotNull(ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void testNeedManualReview_goalIsFalseRegionIsRostovGetsNotEbp() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setGoalMsku(false);
        replenishment.setWarehouseRegionId(WarehouseRegion.Companion.getRostovId());

        assertNull(ebpCalculatorService.needsManualReview(replenishment, 0L, 0L));
    }

    @Test
    public void testNotCorefixWithReqEqualsMOQAndSCMoreSS() {
        RecommendationNew replenishment = getMoscow1pRecommendation();
        replenishment.setCoreFixMatrix(false);
        replenishment.setAdjustedPurchQty(5);
        replenishment.setMinShipment(5);
        replenishment.setSafetyStock(10.0);

        assertNull(ebpCalculatorService.needsManualReview(replenishment, 15L, 11L));
    }

    private RecommendationNew getMoscow1pRecommendation() {
        final RecommendationNew recommendation = new1pRecommendation();
        recommendation.setWarehouseRegionId(WarehouseRegion.Companion.getMoscowId());
        return recommendation;
    }
}

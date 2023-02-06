package ru.yandex.market.replenishment.autoorder;

import java.time.LocalDate;
import java.util.HashMap;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.Recommendation;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.WeeksType;
import ru.yandex.market.replenishment.autoorder.service.ReplenishmentFilterCalculatorService;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.replenishment.autoorder.model.ABC.NEW;
@ActiveProfiles("unittest")
public class ReplenishmentFilterCalculatorTest extends FunctionalTest {

    @Autowired
    ReplenishmentFilterCalculatorService replenishmentFilterCalculatorService;

    @Test
    public void replenishmentIsNew() {
        var r = TestUtils.new1pRecommendation();
        r.setAbc(NEW);
        var filter = replenishmentFilterCalculatorService.resolve(r);
        assertNotNull(filter);
        assertThat(filter.first, equalTo(Recommendation.Filter.NEW));
    }

    @Test
    public void replenishmentIsSalesZeroZeroTwo() {
        assertSalesWhereStockTransitZero(new int[]{0, 1, 2, 3, 3, 3, 3}, Recommendation.Filter.SALES_ZERO,
                WeeksType.ZERO_TWO);
    }

    @Test
    public void replenishmentIsSalesZeroTwoThree() {
        assertSalesWhereStockTransitZero(new int[]{0, 0, 2, 3, 3, 3, 3}, Recommendation.Filter.SALES_ZERO,
                WeeksType.TWO_THREE);
    }

    @Test
    public void replenishmentIsSalesZeroThreeFour() {
        assertSalesWhereStockTransitZero(new int[]{0, 0, 0, 3, 3, 3, 3}, Recommendation.Filter.SALES_ZERO,
                WeeksType.THREE_FOUR);
    }

    @Test
    public void replenishmentIsSalesZeroFourEight() {
        assertSalesWhereStockTransitZero(new int[]{0, 0, 0, 0, 0, 0, 0}, Recommendation.Filter.SALES_ZERO,
                WeeksType.FOUR_EIGHT);
    }

    @Test
    public void replenishmentIsSalesZeroOtherPosZeroTwo() {
        assertSales(new int[]{0, 1, 2, 3, 3, 3, 3}, 5, 3,
                Recommendation.Filter.SALES_ZERO_AND_POSITIVE_STOCK_OR_TRANSIT, WeeksType.ZERO_TWO);
    }

    @Test
    public void replenishmentIsSalesZeroOtherPosTwoThree() {
        assertSales(new int[]{0, 0, 2, 3, 3, 3, 3}, 5, 0,
                Recommendation.Filter.SALES_ZERO_AND_POSITIVE_STOCK_OR_TRANSIT, WeeksType.TWO_THREE);
    }

    @Test
    public void replenishmentIsSalesZeroOtherPosThreeFour() {
        assertSales(new int[]{0, 0, 0, 3, 3, 3, 3}, 0, 3,
                Recommendation.Filter.SALES_ZERO_AND_POSITIVE_STOCK_OR_TRANSIT, WeeksType.THREE_FOUR);
    }

    @Test
    public void replenishmentIsSalesZeroOtherPosFourEight() {
        assertSales(new int[]{0, 0, 0, 0, 0, 0, 0}, 1, 1,
                Recommendation.Filter.SALES_ZERO_AND_POSITIVE_STOCK_OR_TRANSIT, WeeksType.FOUR_EIGHT);
    }

    @Test
    public void replenishmentIsSalesLtQuantumZeroTwo() {
        assertSalesWhereStockTransitZero(new int[]{9, 11, 12, 13, 13, 13, 13},
                Recommendation.Filter.SALES_LT_QUANTUM, WeeksType.ZERO_TWO);
    }

    @Test
    public void replenishmentIsSalesLtQuantumTwoThree() {
        assertSalesWhereStockTransitZero(new int[]{8, 9, 11, 12, 12, 12, 12},
                Recommendation.Filter.SALES_LT_QUANTUM, WeeksType.TWO_THREE);
    }

    @Test
    public void replenishmentIsSalesLtQuantumThreeFour() {
        assertSalesWhereStockTransitZero(new int[]{7, 8, 9, 11, 11, 11, 11},
                Recommendation.Filter.SALES_LT_QUANTUM, WeeksType.THREE_FOUR);
    }

    @Test
    public void replenishmentIsSalesLtQuantumFourEight() {
        assertSalesWhereStockTransitZero(new int[]{6, 7, 8, 9, 9, 9, 9},
                Recommendation.Filter.SALES_LT_QUANTUM, WeeksType.FOUR_EIGHT);
    }

    @Test
    public void replenishmentIsSCTwoThree() {
        assertStockCover(new int[]{100, 100, 100, 100, 100, 100, 100},
                WeeksType.TWO_THREE);
    }

    @Test
    public void replenishmentIsSCFourEight() {
        assertStockCover(new int[]{50, 50, 50, 50, 50, 50, 50},
                WeeksType.FOUR_EIGHT);
    }

    private void assertSalesWhereStockTransitZero(int[] orders,
                                                  Recommendation.Filter expectedFilter,
                                                  WeeksType expectedWeeks) {
        assertSales(orders, 0, 0, expectedFilter, expectedWeeks);
    }

    private static final int[] LEADING_ZERO = {0};

    private void assertSales(int[] orders, int stock, int transit,
                             Recommendation.Filter expectedFilter,
                             WeeksType expectedWeeks) {
        RecommendationNew r = TestUtils.new1pRecommendation();
        r.setAbc(null);
        r.getRegionInfo().setStockOverall(stock);
        r.getWarehouseInfo().setTransit(transit);
        r.setShipmentQuantum(10);
        r.getRegionInfo().setSalesAll(ArrayUtils.addAll(LEADING_ZERO, orders));

        Pair<Recommendation.Filter, WeeksType> section = replenishmentFilterCalculatorService
                .resolve(r);

        assertNotNull(section);
        assertThat(section.first, equalTo(expectedFilter));
        assertThat(section.second, equalTo(expectedWeeks));
    }

    private void assertStockCover(int[] orders, WeeksType expectedWeeks) {
        var r = TestUtils.new1pRecommendation();
        r.setAbc(null);
        r.getWarehouseInfo().setStock(1L);
        r.getRegionSupplierInfo().setStock(11L);
        r.getStocksWithLifetimes().setStocksList(new Long[]{11L});
        r.getStocksWithLifetimes().setLifetimesList(new Integer[]{65535});
        r.setAdjustedPurchQty(30);
        r.getRegionInfo().setSalesAll(ArrayUtils.addAll(LEADING_ZERO, orders));
        r.setDeliveryDate(LocalDate.now());

        var transitsMaps = new HashMap<LocalDate, Long>();
        transitsMaps.put(LocalDate.now().plusDays(1), 21L);
        r.setTransits(transitsMaps);

        Pair<Recommendation.Filter, WeeksType> section = replenishmentFilterCalculatorService
                .resolve(r);

        assertNotNull(section);
        assertThat(section.first, equalTo(Recommendation.Filter.SC));
        assertThat(section.second, equalTo(expectedWeeks));
    }
}

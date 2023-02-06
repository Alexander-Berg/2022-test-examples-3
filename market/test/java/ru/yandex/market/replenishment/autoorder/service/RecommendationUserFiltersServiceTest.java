package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationUserFilter;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.service.user_filters.RecommendationUserFiltersService;
import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate;
import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterableResult;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.CATEGORY;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.CATEGORY2;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.CATEGORY3;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.CORE_FIX_MATRIX;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.MSKU;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.PROMO_PURCHASE;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.PURCH_QTY;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.SALES;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.SSKU;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.STOCK;
@ActiveProfiles("unittest")
public class RecommendationUserFiltersServiceTest extends FunctionalTest {

    @Autowired
    RecommendationUserFiltersService<RecommendationNew> recommendationUserFiltersService;

    @Test
    public void testLongEqual() {
        testLong(11, UserFilterFieldPredicate.EQUAL, "11", true);
        testLong(10, UserFilterFieldPredicate.EQUAL, "11", false);
    }

    @Test
    public void testLongLess() {
        testLong(10, UserFilterFieldPredicate.LESS, "11", true);
        testLong(11, UserFilterFieldPredicate.LESS, "11", false);
        testLong(11, UserFilterFieldPredicate.LESS, "10", false);
    }

    @Test
    public void testLongLessOrEquals() {
        testLong(10, UserFilterFieldPredicate.LESS_OR_EQUALS, "11", true);
        testLong(11, UserFilterFieldPredicate.LESS_OR_EQUALS, "11", true);
        testLong(11, UserFilterFieldPredicate.LESS_OR_EQUALS, "10", false);
    }

    @Test
    public void testLongGreater() {
        testLong(11, UserFilterFieldPredicate.GREATER, "10", true);
        testLong(11, UserFilterFieldPredicate.GREATER, "11", false);
        testLong(10, UserFilterFieldPredicate.GREATER, "11", false);
    }

    @Test
    public void testLongGreaterOrEquals() {
        testLong(11, UserFilterFieldPredicate.GREATER_OR_EQUALS, "10", true);
        testLong(11, UserFilterFieldPredicate.GREATER_OR_EQUALS, "11", true);
        testLong(10, UserFilterFieldPredicate.GREATER_OR_EQUALS, "11", false);
    }

    @Test
    public void testLongInList1() {
        testLong(11, UserFilterFieldPredicate.IN_LIST, "11\n22", true);
        testLong(22, UserFilterFieldPredicate.IN_LIST, "11\n22", true);
        testLong(10, UserFilterFieldPredicate.IN_LIST, "11\n22", false);
    }

    @Test
    public void testLongInList2() {
        testLong(11, UserFilterFieldPredicate.IN_LIST, "11,22", true);
        testLong(22, UserFilterFieldPredicate.IN_LIST, "11,22", true);
        testLong(10, UserFilterFieldPredicate.IN_LIST, "11,22", false);
    }

    @Test
    public void testLongInLis3() {
        testLong(11, UserFilterFieldPredicate.IN_LIST, "11, 22", true);
        testLong(22, UserFilterFieldPredicate.IN_LIST, "11, 22", true);
        testLong(10, UserFilterFieldPredicate.IN_LIST, "11, 22", false);
    }

    @Test
    public void testStringInList() {
        testString("11", UserFilterFieldPredicate.IN_LIST, "11, 22", true);
        testString("22", UserFilterFieldPredicate.IN_LIST, "11, 22", true);
        testString("10", UserFilterFieldPredicate.IN_LIST, "11, 22", false);
    }

    @Test
    public void testStringEqual() {
        testString("11", UserFilterFieldPredicate.EQUAL, "11", true);
        testString("10", UserFilterFieldPredicate.EQUAL, "11", false);
    }

    @Test
    public void testStringStartsWith() {
        testString("11abc", UserFilterFieldPredicate.STARTS_WITH, "11", true);
        testString("10abc", UserFilterFieldPredicate.STARTS_WITH, "11", false);
    }

    @Test
    public void testStringContains() {
        testString("a11bcs", UserFilterFieldPredicate.CONTAINS, "11", true);
        testString("a12bcs", UserFilterFieldPredicate.CONTAINS, "11", false);
    }

    @Test
    public void testBoolTrue() {
        testBoolCoreFixMatrix(true, UserFilterFieldPredicate.TRUE, true);
        testBoolCoreFixMatrix(false, UserFilterFieldPredicate.TRUE, false);

        testBoolPromoPurchase(true, UserFilterFieldPredicate.TRUE, true);
        testBoolPromoPurchase(false, UserFilterFieldPredicate.TRUE, false);
    }

    @Test
    public void testBoolFalse() {
        testBoolCoreFixMatrix(true, UserFilterFieldPredicate.FALSE, false);
        testBoolCoreFixMatrix(false, UserFilterFieldPredicate.FALSE, true);

        testBoolPromoPurchase(true, UserFilterFieldPredicate.FALSE, false);
        testBoolPromoPurchase(false, UserFilterFieldPredicate.FALSE, true);
    }

    @Test
    public void testMultipleFilters() {
        UserFilterableResult<RecommendationNew> result = recommendationUserFiltersService.filter(
                Arrays.asList(
                        createRecommendation(10, 12L, 1, 50L),
                        createRecommendation(11, 10L, 1, 20L),
                        createRecommendation(12, 1L, 5, 75L)
                ),
                Arrays.asList(
                        new RecommendationUserFilter(SSKU, UserFilterFieldPredicate.EQUAL, "10"),
                        new RecommendationUserFilter(MSKU, UserFilterFieldPredicate.LESS, "12"),
                        new RecommendationUserFilter(MSKU, UserFilterFieldPredicate.EQUAL, "10")
                )
        );
        assertEquals(result.getResults().size(), 1);
        assertEquals(result.getUserFiltersCount()[0], 3);
        assertEquals(result.getUserFiltersCount()[1], 2);
        assertEquals(result.getUserFiltersCount()[2], 1);
    }

    @Test
    public void testCategory() {
        UserFilterableResult<RecommendationNew> result = recommendationUserFiltersService.filter(
                Arrays.asList(
                        createRecommendation(10, 12L, 1, 50L),
                        createRecommendation(11, 10L, 1, 20L),
                        createRecommendation(12, 1L, 5, 75L)
                ),
                Arrays.asList(
                        new RecommendationUserFilter(CATEGORY, UserFilterFieldPredicate.EQUAL, "cat1"),
                        new RecommendationUserFilter(CATEGORY2, UserFilterFieldPredicate.EQUAL, "cat2"),
                        new RecommendationUserFilter(CATEGORY3, UserFilterFieldPredicate.EQUAL, "cat3")
                )
        );
        assertEquals(result.getUserFiltersCount()[0], 3);
        assertEquals(result.getUserFiltersCount()[1], 3);
        assertEquals(result.getUserFiltersCount()[2], 3);
    }

    @Test
    public void testStock() {
        UserFilterableResult<RecommendationNew> result = recommendationUserFiltersService.filter(
                Arrays.asList(
                        createRecommendation(10, 12L, 1, 50L),
                        createRecommendation(11, 10L, 1, 20L),
                        createRecommendation(12, 1L, 5, 75L)
                ),
                Collections.singletonList(
                        new RecommendationUserFilter(STOCK, UserFilterFieldPredicate.GREATER, "5")
                )
        );
        assertEquals(result.getUserFiltersCount()[0], 2);
    }

    @Test
    public void testPurchQty() {
        UserFilterableResult<RecommendationNew> result = recommendationUserFiltersService.filter(
                Arrays.asList(
                        createRecommendation(10, 12L, 1, 50L),
                        createRecommendation(11, 10L, 1, 20L),
                        createRecommendation(12, 1L, 5, 75L)
                ),
                Collections.singletonList(
                        new RecommendationUserFilter(PURCH_QTY, UserFilterFieldPredicate.EQUAL, "1")
                )
        );
        assertEquals(result.getUserFiltersCount()[0], 2);
    }

    @Test
    public void testSales() {
        UserFilterableResult<RecommendationNew> result = recommendationUserFiltersService.filter(
                Arrays.asList(
                        createRecommendation(10, 12L, 1, 50L),
                        createRecommendation(11, 10L, 1, 20L),
                        createRecommendation(12, 1L, 5, 75L)
                ),
                Collections.singletonList(
                        new RecommendationUserFilter(SALES, UserFilterFieldPredicate.LESS_OR_EQUALS, "50")
                )
        );
        assertEquals(result.getUserFiltersCount()[0], 2);
    }

    @NotNull
    private RecommendationNew createRecommendation(int msku, Long stock, int purchQty, Long sales) {
        final RecommendationNew recommendation = TestUtils.new1pRecommendation();
        assert recommendation.getWarehouseInfo() != null;
        assert recommendation.getRegionInfo() != null;

        recommendation.setMsku(msku);
        recommendation.setSsku("10");
        recommendation.setBarcode("10");
        recommendation.setCategoryName("cat1");
        recommendation.setSubCategory("cat2");
        recommendation.setSubSubCategory("cat3");
        recommendation.setVendorName("vend1");
        recommendation.getWarehouseInfo().setStock(stock);
        recommendation.setPurchQty(purchQty);
        recommendation.getRegionInfo().setActualSales(sales);
        return recommendation;
    }

    private void testLong(long recommendationValue, UserFilterFieldPredicate predicate, String value,
                          boolean matches) {
        final RecommendationNew recommendation = TestUtils.new1pRecommendation();
        recommendation.setMsku(recommendationValue);
        final RecommendationUserFilter filter = new RecommendationUserFilter();
        filter.setField(MSKU);
        filter.setValue(value);
        filter.setPredicate(predicate);
        UserFilterableResult<RecommendationNew> result = recommendationUserFiltersService
                .filter(Collections.singletonList(recommendation), Collections.singletonList(filter));
        assertResult(result, matches);
    }

    private void testString(String recommendationValue, UserFilterFieldPredicate predicate, String value,
                            boolean matches) {
        final RecommendationNew recommendation = TestUtils.new1pRecommendation();
        recommendation.setSsku(recommendationValue);
        final RecommendationUserFilter filter = new RecommendationUserFilter();
        filter.setField(SSKU);
        filter.setValue(value);
        filter.setPredicate(predicate);
        UserFilterableResult<RecommendationNew> result = recommendationUserFiltersService
                .filter(Collections.singletonList(recommendation), Collections.singletonList(filter));
        assertResult(result, matches);
    }

    private void testBoolCoreFixMatrix(boolean recommendationValue, UserFilterFieldPredicate predicate,
                                       boolean matches) {
        final RecommendationNew recommendation = TestUtils.new1pRecommendation();
        recommendation.setCoreFixMatrix(recommendationValue);
        var filter = arrangeBooleanUserFilter(CORE_FIX_MATRIX, predicate);

        UserFilterableResult<RecommendationNew> result = recommendationUserFiltersService
                .filter(Collections.singletonList(recommendation), Collections.singletonList(filter));

        assertResult(result, matches);
    }

    private void testBoolPromoPurchase(boolean recommendationValue, UserFilterFieldPredicate predicate,
                                       boolean matches) {
        final LocalDate today = LocalDate.now();
        final RecommendationNew recommendation = TestUtils.new1pRecommendation();

        if (recommendationValue) {
            recommendation.setPurchasePromoStart(today);
            recommendation.setPurchasePromoEnd(today);
        }

        RecommendationUserFilter filter = arrangeBooleanUserFilter(PROMO_PURCHASE, predicate);

        UserFilterableResult<RecommendationNew> result = recommendationUserFiltersService
                .filter(Collections.singletonList(recommendation), Collections.singletonList(filter));

        assertResult(result, matches);
    }

    private RecommendationUserFilter arrangeBooleanUserFilter(RecommendationField filterType,
                                                              UserFilterFieldPredicate predicate) {
        final RecommendationUserFilter filter = new RecommendationUserFilter();
        filter.setField(filterType);
        filter.setValue(predicate.toString());
        filter.setPredicate(predicate);
        return filter;
    }

    private void assertResult(UserFilterableResult<RecommendationNew> result, boolean matches) {
        assertEquals(result.getResults().size(), matches ? 1 : 0);
        assertEquals(result.getUserFiltersCount().length, 1);
        assertEquals(result.getUserFiltersCount()[0], matches ? 1 : 0);
    }
}

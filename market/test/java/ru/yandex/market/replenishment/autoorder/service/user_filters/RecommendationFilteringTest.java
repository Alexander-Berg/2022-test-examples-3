package ru.yandex.market.replenishment.autoorder.service.user_filters;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationUserFilter;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.RecommendationRegionInfo;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.RecommendationWarehouseInfo;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.StocksWithLifetimes;
import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate;
import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterableResult;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.replenishment.autoorder.model.ABC.A;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.EQUAL;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.TRUE;

public class RecommendationFilteringTest extends FunctionalTest {

    @Autowired
    RecommendationUserFiltersService<RecommendationNew> recommendationUserFiltersService;

    RecommendationNew emptyRecommendation = emptyRecommendation();

    RecommendationNew r = emptyRecommendation();

    @NotNull
    private RecommendationNew emptyRecommendation() {
        RecommendationNew r = new RecommendationNew();
        r.setDemandType(DemandType.TYPE_1P);
        r.setStocksWithLifetimes(new StocksWithLifetimes());
        r.setWarehouseInfo(new RecommendationWarehouseInfo());
        r.setRegionInfo(new RecommendationRegionInfo());
        return r;
    }

    @Test
    public void testExtractors() {
        test(RecommendationField.ABC, EQUAL, r -> r.setAbc(A), "A");
        test(RecommendationField.MSKU, EQUAL, r -> r.setMsku(123L), "123");
        test(RecommendationField.SSKU, EQUAL, r -> r.setSsku("123.123"), "123.123");
        test(RecommendationField.TITLE, EQUAL, r -> r.setTitle("title"), "title");
        test(RecommendationField.BARCODE, EQUAL, r -> r.setBarcode("barcode"), "barcode");
        test(RecommendationField.MANUFACTURER, EQUAL, r -> r.setManufacturer("manufacturer"), "manufacturer");
        test(RecommendationField.BRAND, EQUAL, r -> r.setVendorName("brand"), "brand");
        test(RecommendationField.CORE_FIX_MATRIX, TRUE, r -> r.setCoreFixMatrix(true), "true");
        test(RecommendationField.CATEGORY, EQUAL, r -> r.setCategoryName("category"), "category");
        test(RecommendationField.CATEGORY2, EQUAL, r -> r.setSubCategory("category2"), "category2");
        test(RecommendationField.CATEGORY3, EQUAL, r -> r.setSubSubCategory("category3"), "category3");
        test(RecommendationField.WAREHOUSE, EQUAL, r -> r.setWarehouseId(171L), "171");
        test(RecommendationField.STOCK, EQUAL, r -> {
            assert r.getWarehouseInfo() != null;
            r.getWarehouseInfo().setStock(10);
        }, "10");
        test(RecommendationField.PURCH_QTY, EQUAL, r -> r.setPurchQty(30), "30");
        test(RecommendationField.SALES, EQUAL, r -> {
            assert r.getRegionInfo() != null;
            r.getRegionInfo().setActualSales(12L);
        }, "12");
        test(RecommendationField.PROMO_PURCHASE, TRUE, r -> {
            r.setPurchasePromoStart(LocalDate.MIN);
            r.setPurchasePromoEnd(LocalDate.MAX);
        }, "true");
        test(RecommendationField.PROMO_SALE, TRUE, r -> {
            r.setSalePromoStart(LocalDate.MIN);
            r.setSalePromoEnd(LocalDate.MAX);
        }, "true");
        test(RecommendationField.GOAL_MSKU, TRUE, r -> r.setGoalMsku(true), "true");

        Consumer<RecommendationNew> scSetter = r -> {
            assert r.getStocksWithLifetimes() != null;
            r.getStocksWithLifetimes().setStocksList(new Long[] {10L});
            r.getStocksWithLifetimes().setLifetimesList(new Integer[] {65535});

            assert r.getRegionInfo() != null;
            r.getRegionInfo().setSalesAll(new int[] {0, 0, 0, 150, 150, 150, 150, 150});
            r.getRegionInfo().setSalesForecast28days(50.0);
            r.getRegionInfo().setMissedOrders28d(50.0);
            r.getRegionInfo().setStockOverall(10L);

            assert r.getWarehouseInfo() != null;
            r.getWarehouseInfo().setTransit(20);
            r.setAdjustedPurchQty(30);
            r.setDeliveryDate(LocalDate.now().plusDays(2));
            r.setGoalMsku(false);

            var transitsMaps = new HashMap<LocalDate, Long>();
            transitsMaps.put(LocalDate.now(), 20L);
            r.setTransits(transitsMaps);
        };

        test(RecommendationField.SCB, EQUAL, scSetter, "9");
        test(RecommendationField.SCF, EQUAL, scSetter, "31");
    }


    private void test(
        RecommendationField field,
        UserFilterFieldPredicate predicate,
        Consumer<RecommendationNew> fieldSetter,
        String value
    ) {
        fieldSetter.accept(r);
        UserFilterableResult<RecommendationNew> result =
            recommendationUserFiltersService.filter(
                Arrays.asList(emptyRecommendation, r),
                Collections.singletonList(new RecommendationUserFilter(field, predicate, value))
            );
        assertEquals(1, result.getResults().size());
        assertEquals(1, result.getUserFiltersCount()[0]);
    }
}


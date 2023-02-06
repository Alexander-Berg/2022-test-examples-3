package ru.yandex.market.partner.mvc.controller.order;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.partner.mvc.controller.util.ResponseJsonUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link OrdersControllerV2#getOrderItemsDetails}.
 */
@DbUnitDataSet(before = "OrdersControllerV2ItemsDetailsTest.before.csv")
class OrdersControllerV2ItemsDetailsTest extends FunctionalTest {

    @Test
    @DisplayName("Проверка детализации позиций заказа без возвратов и невыкупов")
    void testGetOrderItemsStatusBreakdownProcessing() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "v2/campaigns/100800/orders/55/items/statuses/details");
        assertJsonAnyFieldOrder(response, this.getClass(), "OrdersControllerV2ItemsDetailsTest.processing.json");
    }

    @Test
    @DisplayName("Проверка детализации позиций заказа c частичным возвратом")
    void testGetOrderItemsStatusBreakdownReturns() {
        //в заказе две позиции - одна доставлена полностью
        //во второй 5 штук - 2 возвращены 2020-12-23 на годный сток,
        //одна возвращена 2020-12-24 на сток брака
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "v2/campaigns/100800/orders/56/items/statuses/details");
        assertJsonAnyFieldOrder(response, this.getClass(), "OrdersControllerV2ItemsDetailsTest.returns.json");
    }

    @Test
    @DisplayName("Проверка детализации позиций заказа c невыкупом")
    void testGetOrderItemsStatusBreakdownUnredeemed() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "v2/campaigns/100800/orders/57/items/statuses/details");
        assertJsonAnyFieldOrder(response, this.getClass(), "OrdersControllerV2ItemsDetailsTest.unredeemed.json");
    }

    @Test
    @DisplayName("Проверка детализации позиций заказа c удалением части товаров")
    void testGetOrderItemsStatusBreakdownRemoved() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "v2/campaigns/100800/orders/58/items/statuses/details");
        assertJsonAnyFieldOrder(response, this.getClass(), "OrdersControllerV2ItemsDetailsTest.removed.json");
    }

    private static void assertJsonAnyFieldOrder(@Nonnull ResponseEntity<String> actualResponse,
                                                @Nonnull Class<?> contextClass, @Nonnull String filename) {
        var expectedString = StringTestUtil.getString(contextClass, filename);
        var actualResult = ResponseJsonUtil.getResult(actualResponse);
        JSONAssert.assertEquals(expectedString, actualResult, JSONCompareMode.STRICT_ORDER);
    }
}

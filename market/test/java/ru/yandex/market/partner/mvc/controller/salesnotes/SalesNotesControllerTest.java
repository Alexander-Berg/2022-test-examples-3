package ru.yandex.market.partner.mvc.controller.salesnotes;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link SalesNotesController}
 *
 * @author au-rikka
 */
@DbUnitDataSet(before = "SalesNotes.before.csv")
class SalesNotesControllerTest extends FunctionalTest {
    private static final Pair<Long, Currency> OFF_VALID_SHOP_REGION_CURRENCY = Pair.of(157L, Currency.BYN);
    private static final Pair<Long, Currency> ON_INVALID_SHOP_REGION_CURRENCY = Pair.of(162L, Currency.KZT);
    private static final Pair<Long, Currency> NEW_SHOP_REGION_CURRENCY = Pair.of(143L, Currency.UAH);
    private static final int VALID_SUM_VALUE = 50;
    private static final int INVALID_ZERO_SUM_VALUE = 0;
    private static final int INVALID_SUM_VALUE = -50;
    private static final String BAD_ORDER_MIN_COST_ARG_VALUE_RESPONSE = "" +
            "Order min cost value must be greater than zero.";
    private static final String BAD_ORDER_MIN_COST_TABLE_VALUE_RESPONSE = "" +
            "Can not turn order min cost on because its value is invalid. " +
            "Set greater than zero order min cost value first.";
    private static final String ABSENT_ORDER_MIN_COST_VALUE_RESPONCE = "" +
            "Can not turn order min cost on because its value is absent. " +
            "Set greater than zero order min cost value first.";
    private static Pair<Long, String> OFF_INVALID_SHOP = Pair.of(111L, "off_invalid_shop_response.json");
    private static Pair<Long, String> OFF_VALID_SHOP = Pair.of(222L, "off_valid_shop_response.json");
    private static Pair<Long, String> ON_INVALID_NULL_SHOP = Pair.of(666L, "on_invalid_null_shop_response.json");
    private static Pair<Long, String> ON_VALID_SHOP = Pair.of(777L, "on_valid_shop_response.json");
    private static Pair<Long, String> NEW_SHOP = Pair.of(123L, "new_shop_response.json");
    @Autowired
    private RegionService regionService;

    @BeforeEach
    void resetMock() {
        reset(regionService);
    }


    private String shopSalesNotesUrl(long shopId) {
        return baseUrl + String.format("/shops/sales-notes/%d", shopId);
    }

    private void toggleOrderMinCost(long shopId, boolean status) {
        final String url = shopSalesNotesUrl(shopId) + String.format("/toggle-order-min-cost?status=%b", status);
        FunctionalTestHelper.put(url, new HttpEntity(null));
    }

    private void setOrderMinCost(long shopId, int value) {
        final String url = shopSalesNotesUrl(shopId) + String.format("/set-order-min-cost?value=%d", value);
        FunctionalTestHelper.put(url, new HttpEntity(null));
    }

    private void checkGetSalesNotes(Pair<Long, String> shopInfo) {
        final String url = shopSalesNotesUrl(shopInfo.getFirst());
        final ResponseEntity<String> response = FunctionalTestHelper.get(url);
        final String jsonFileName = String.format("/mvc/salesnotes/%s", shopInfo.getSecond());
        JsonTestUtil.assertEquals(response, this.getClass(), jsonFileName);
    }


    /**
     * Тест для ручки {@code shops/sales-notes/${shopId}/toggle-order-min-cost}.
     * Выключение включенной минимальной суммы для существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.toggleMinCostOnToOff.after.csv")
    void testToggleOrderMinCostOnToOff() {
        toggleOrderMinCost(ON_VALID_SHOP.getFirst(), false);
    }

    /**
     * Тест для ручки {@code shops/sales-notes/${shopId}/toggle-order-min-cost}.
     * Выключение выключенной минимальной суммы (некорректной) для существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testToggleOrderMinCostOffToOff() {
        toggleOrderMinCost(OFF_INVALID_SHOP.getFirst(), false);
    }

    /**
     * Тест для ручки {@code shops/sales-notes/${shopId}/toggle-order-min-cost}.
     * Выключение минимальной суммы для несуществующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testToggleOrderMinCostNullToOff() {
        toggleOrderMinCost(NEW_SHOP.getFirst(), false);
    }

    /**
     * Тест для ручки {@code shops/sales-notes/${shopId}/toggle-order-min-cost}.
     * Включение выключенной минимальной суммы (корректной) для существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.toggleMinCostOffToOn.after.csv")
    void testToggleOrderMinCostOffToOn() {
        toggleOrderMinCost(OFF_VALID_SHOP.getFirst(), true);
    }

    /**
     * Тест для ручки {@code shops/sales-notes/${shopId}/toggle-order-min-cost}.
     * Включение выключенной минимальной суммы (некорректной) для существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testToggleOrderMinCostOffToOnBadValue() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> toggleOrderMinCost(OFF_INVALID_SHOP.getFirst(), true)
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.FORBIDDEN));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorMessage(BAD_ORDER_MIN_COST_TABLE_VALUE_RESPONSE));
    }

    /**
     * Тест для ручки {@code shops/sales-notes/${shopId}/toggle-order-min-cost}.
     * Включение минимальной суммы для несуществующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testToggleOrderMinCostNullToOn() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> toggleOrderMinCost(NEW_SHOP.getFirst(), true)
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.FORBIDDEN));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorMessage(ABSENT_ORDER_MIN_COST_VALUE_RESPONCE));
    }

    /**
     * Тест для ручки {@code shops/sales-notes/${shopId}/toggle-order-min-cost}.
     * Включение включенной минимальной суммы для существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testToggleOrderMinCostOnToOn() {
        toggleOrderMinCost(ON_VALID_SHOP.getFirst(), true);
    }

    /**
     * Тест для ручки {@code shops/sales-notes/${shopId}/toggle-order-min-cost}.
     * Включение включенной некорректной минимальной суммы для существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testToggleOrderMinCostOnToOnBadValue() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> toggleOrderMinCost(ON_INVALID_NULL_SHOP.getFirst(), true)
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.FORBIDDEN));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorMessage(BAD_ORDER_MIN_COST_TABLE_VALUE_RESPONSE));
    }


    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}/set-order-min-cost}.
     * Выставление корректного значения минимальной суммы для включенной существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.setMinCostOn.after.csv")
    void testSetValidOrderMinCostOn() {
        setOrderMinCost(ON_VALID_SHOP.getFirst(), VALID_SUM_VALUE);
    }

    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}/set-order-min-cost}.
     * Выставление корректного значения минимальной суммы для выключенной существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.setMinCostOff.after.csv")
    void testSetValidOrderMinCostOff() {
        setOrderMinCost(OFF_INVALID_SHOP.getFirst(), VALID_SUM_VALUE);
    }

    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}/set-order-min-cost}.
     * Выставление корректного значения минимальной суммы для несуществующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.setMinCostNew.after.csv")
    void testSetValidOrderMinCost() {
        setOrderMinCost(NEW_SHOP.getFirst(), VALID_SUM_VALUE);
    }

    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}/set-order-min-cost}.
     * Выставление некорректного значения минимальной суммы для включенной существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testSetInvalidOrderMinCostOn() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> setOrderMinCost(ON_VALID_SHOP.getFirst(), INVALID_SUM_VALUE)
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorMessage(BAD_ORDER_MIN_COST_ARG_VALUE_RESPONSE));

    }

    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}/set-order-min-cost}.
     * Выставление некорректного значения минимальной суммы для выключенной существующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testSetInvalidOrderMinCostOff() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> setOrderMinCost(OFF_VALID_SHOP.getFirst(), INVALID_ZERO_SUM_VALUE)
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorMessage(BAD_ORDER_MIN_COST_ARG_VALUE_RESPONSE));

    }

    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}/set-order-min-cost}.
     * Выставление некорректного значения минимальной суммы для несуществующей записи.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testSetInvalidOrderMinCost() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> setOrderMinCost(NEW_SHOP.getFirst(), INVALID_ZERO_SUM_VALUE)
        );
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorMessage(BAD_ORDER_MIN_COST_ARG_VALUE_RESPONSE));
    }


    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}}.
     * Получение информации для выключенного магазина с некорректным значением минимальной суммы.
     * Валюта магазина выставляется по умолчанию (RUR).
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testGetSalesNotesOffBadValue() {
        checkGetSalesNotes(OFF_INVALID_SHOP);
    }

    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}}.
     * Получение информации для выключенного магазина.
     * Валюта магазина выставляется из параметра {@link ParamType#CPA_SHOP_CURRENCY}.
     * {@link ParamType#HOME_REGION} тоже указан, но не влияет на валюту.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testGetSalesNotesOff() {
        when(regionService.getRegionCurrency(eq(OFF_VALID_SHOP_REGION_CURRENCY.getFirst()))).thenReturn(OFF_VALID_SHOP_REGION_CURRENCY.getSecond());
        checkGetSalesNotes(OFF_VALID_SHOP);
    }

    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}}.
     * Получение информации для включенного магазина c некорректным значением минимальной суммы.
     * Валюта магазина берется из поля currency таблицы shops_web.sales_notes.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testGetSalesNotesOnInvalid() {
        when(regionService.getRegionCurrency(eq(ON_INVALID_SHOP_REGION_CURRENCY.getFirst()))).thenReturn(ON_INVALID_SHOP_REGION_CURRENCY.getSecond());
        checkGetSalesNotes(ON_INVALID_NULL_SHOP);
    }

    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}}.
     * Получение информации для включенного магазина.
     * Валюта магазина выставляется из параметра {@link ParamType#CPA_SHOP_CURRENCY}.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testGetSalesNotesOn() {
        checkGetSalesNotes(ON_VALID_SHOP);
    }

    /**
     * Тест для ручки {@code /shops/sales-notes/${shopId}}.
     * Получение информации для нового магазина.
     * Валюта магазина определяется по параметру региона {@link ParamType#HOME_REGION}.
     */
    @Test
    @DbUnitDataSet(after = "SalesNotes.before.csv")
    void testGetSalesNotesNew() {
        when(regionService.getRegionCurrency(eq(NEW_SHOP_REGION_CURRENCY.getFirst()))).thenReturn(NEW_SHOP_REGION_CURRENCY.getSecond());
        checkGetSalesNotes(NEW_SHOP);
    }

}

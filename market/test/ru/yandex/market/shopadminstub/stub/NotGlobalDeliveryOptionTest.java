package ru.yandex.market.shopadminstub.stub;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.report.model.outlet.Outlet;
import ru.yandex.market.helpers.CartHelper;
import ru.yandex.market.helpers.CartParameters;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.ItemProvider;
import ru.yandex.market.providers.OutletProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.ItemDeliveryOption;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.providers.CartRequestProvider.DEFAULT_REGION_2;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkItem;

public class NotGlobalDeliveryOptionTest extends AbstractTestBase {

    @Autowired
    private CartHelper cartHelper;

    /**
     * Стаб возвращает опции доставки полученные из репорта
     * <p>
     * Подготовка:
     * 1. Подкладываем ответ репорта
     * 2. Подкладываем ответ фиддиспатчера
     * <p>
     * Действие:
     * 1. Дергаем ручку /{shopId}/cart
     * <p>
     * Проверка:
     * 1. Проверяем, что стаб вернул опции доставки из репорта + аутлеты
     */
    @Test
    public void testShouldNotClearDeliveryOptionsForNotGlobal() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();
        cartRequest.setRegionId(DEFAULT_REGION_2);

        List<Outlet> outlets = asList(OutletProvider.buildFirst(), OutletProvider.buildSecond());

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, outlets);

        cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(xpath("/cart/items/item/@delivery").string("true"))
                .andExpect(xpath("/cart/delivery-options/delivery").nodeCount(4));
    }

    /**
     * Стаб должен отдавать доставку в родной регион
     * <p>
     * Подготовка:
     * 1. Кладем в настройки домашний регион = спб
     * 2. Подкладываем ответ репорта
     * 3. Подкладываем ответ фиддиспатчера
     * <p>
     * Действие:
     * 1. Дергаем ручку /{shopId}/cart
     * <p>
     * Проверка:
     * 1. Проверяем, что есть опция доставки, которую вернул репорт
     */
    @Test
    public void testStubPushApi() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();
        cartRequest.setRegionId(DEFAULT_REGION_2);

        List<Outlet> outlets = asList(OutletProvider.buildFirst(), OutletProvider.buildSecond());

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, outlets);


        ResultActions resultActions = cartHelper.cart(cartParameters);

        LocalDate today = LocalDate.now();

        checkItem(resultActions, 1, ItemProvider.DEFAULT_FEED_ID, ItemProvider.DEFAULT_OFFER_ID);
        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 1, StubPushApiTestUtils.ALL_POSTPAID, "Курьер", "DELIVERY", "100.00", 1, 2);
        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 2, StubPushApiTestUtils.ONLY_CASH, "Курьер", "DELIVERY", "50.00", 14, 14);
        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 3, StubPushApiTestUtils.ONLY_CASH, "Курьер", "DELIVERY", "0.00", 31, null);
        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 4, StubPushApiTestUtils.ONLY_CARD, "Самовывоз", "PICKUP", "50", 30, null);

        resultActions
                .andExpect(xpath("/cart/delivery-options/delivery[@type='PICKUP']/outlets/outlet/@code").string("69"));

        resultActions
                .andExpect(xpath("/cart/payment-methods/payment-method").nodeCount(2))
                .andExpect(xpath("/cart/payment-methods/payment-method[1]/text()").string(PaymentMethod.CASH_ON_DELIVERY.name()))
                .andExpect(xpath("/cart/payment-methods/payment-method[2]/text()").string(PaymentMethod.CARD_ON_DELIVERY.name()));
    }

    /**
     * Стаб должен выбирать одну опцию доставки из возможных с учетом orderBefore
     * <p>
     * Подготовка:
     * 1. Подкладываем ответ репорта с двумя опциями доставки с разным orderBefore и разной ценой
     * 2. Задаем текущее время меньше минимального orderBefore
     * <p>
     * Действие:
     * 1. Дергаем ручку /{shopId}/cart
     * <p>
     * Проверка:
     * 1. Проверяем, что есть одна опция доставки, с минимальным orderBefore
     */
    @Test
    public void testStubReturnsSingleOptionWhenNowBeforeFirstOrderBefore() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION);

        List<Outlet> outlets = asList(OutletProvider.buildFirst(), OutletProvider.buildSecond());

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, outlets);
        Item foundOffer = Iterables.getOnlyElement(cartParameters.getReportParameters().getCartRequest().getItems().values());
        ItemDeliveryOption before18 = new ItemDeliveryOption();
        before18.setOrderBefore(18);
        before18.setFromDay(0);
        before18.setToDay(0);
        before18.setPrice(BigDecimal.ZERO);
        before18.setPaymentMethods(asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        ItemDeliveryOption before20 = new ItemDeliveryOption();
        before20.setOrderBefore(20);
        before20.setFromDay(0);
        before20.setToDay(0);
        before20.setPrice(BigDecimal.TEN);
        before20.setPaymentMethods(asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        foundOffer.setDeliveryOptions(asList(before18, before20));
        cartParameters.setFakeNow(LocalDateTime.of(2018, Month.FEBRUARY, 8, 16, 0, 0));

        ResultActions resultActions = cartHelper.cart(cartParameters);

        LocalDate today = LocalDate.of(2018, Month.FEBRUARY, 8);

        checkItem(resultActions, 1, ItemProvider.DEFAULT_FEED_ID, ItemProvider.DEFAULT_OFFER_ID);
        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 1, StubPushApiTestUtils.ALL_POSTPAID, "Курьер", "DELIVERY", "0.00", 0, 0);
        StubPushApiTestUtils.checkDeliveryOptionsCount(resultActions, 1);
    }

    /**
     * Стаб должен выбирать одну опцию доставки из возможных с учетом orderBefore
     * <p>
     * Подготовка:
     * 1. Подкладываем ответ репорта с двумя опциями доставки с разным orderBefore и разной ценой
     * 2. Задаем текущее время в промежутке между минимальным и максимальным orderBefore
     * <p>
     * Действие:
     * 1. Дергаем ручку /{shopId}/cart
     * <p>
     * Проверка:
     * 1. Проверяем, что есть две опции доставки, одна из которых сдвинута на один день
     */
    @Test
    public void testStubReturnsTwoOptionsWhenNowAfterFirstOrderBefore() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION);

        List<Outlet> outlets = asList(OutletProvider.buildFirst(), OutletProvider.buildSecond());

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, outlets);
        Item foundOffer = Iterables.getOnlyElement(cartParameters.getReportParameters().getCartRequest().getItems().values());
        ItemDeliveryOption before18 = new ItemDeliveryOption();
        before18.setOrderBefore(18);
        before18.setFromDay(0);
        before18.setToDay(0);
        before18.setPrice(BigDecimal.ZERO);
        before18.setPaymentMethods(asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        ItemDeliveryOption before20 = new ItemDeliveryOption();
        before20.setOrderBefore(20);
        before20.setFromDay(0);
        before20.setToDay(0);
        before20.setPrice(BigDecimal.TEN);
        before20.setPaymentMethods(asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        foundOffer.setDeliveryOptions(asList(before18, before20));
        cartParameters.setFakeNow(LocalDateTime.of(2018, Month.FEBRUARY, 8, 19, 0, 0));

        ResultActions resultActions = cartHelper.cart(cartParameters);

        LocalDate today = LocalDate.of(2018, Month.FEBRUARY, 8);

        checkItem(resultActions, 1, ItemProvider.DEFAULT_FEED_ID, ItemProvider.DEFAULT_OFFER_ID);
        StubPushApiTestUtils.checkDeliveryOptionsCount(resultActions, 2);
        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 1, StubPushApiTestUtils.ALL_POSTPAID, "Курьер", "DELIVERY", "10.00", 0, 0);
        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 2, StubPushApiTestUtils.ALL_POSTPAID, "Курьер", "DELIVERY", "0.00", 1, 1);
    }

    /**
     * Стаб должен выбирать одну опцию доставки из возможных с учетом orderBefore
     * <p>
     * Подготовка:
     * 1. Подкладываем ответ репорта с двумя опциями доставки с разным orderBefore и разной ценой
     * 2. Задаем текущее время больше максимального orderBefore
     * <p>
     * Действие:
     * 1. Дергаем ручку /{shopId}/cart
     * <p>
     * Проверка:
     * 1. Проверяем, что есть одна опция доставки - на следующий день с минимальным orderBefore
     */
    @Test
    public void testStubReturnsTwoOptionsWhenNowAfterSecondOrderBefore() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION);

        List<Outlet> outlets = asList(OutletProvider.buildFirst(), OutletProvider.buildSecond());

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, outlets);
        Item foundOffer = Iterables.getOnlyElement(cartParameters.getReportParameters().getCartRequest().getItems().values());
        ItemDeliveryOption before18 = new ItemDeliveryOption();
        before18.setOrderBefore(18);
        before18.setFromDay(0);
        before18.setToDay(0);
        before18.setPrice(BigDecimal.ZERO);
        before18.setPaymentMethods(asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        ItemDeliveryOption before20 = new ItemDeliveryOption();
        before20.setOrderBefore(20);
        before20.setFromDay(0);
        before20.setToDay(0);
        before20.setPrice(BigDecimal.TEN);
        before20.setPaymentMethods(asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        foundOffer.setDeliveryOptions(asList(before18, before20));
        cartParameters.setFakeNow(LocalDateTime.of(2018, Month.FEBRUARY, 8, 21, 0, 0));

        ResultActions resultActions = cartHelper.cart(cartParameters);

        LocalDate today = LocalDate.of(2018, Month.FEBRUARY, 8);

        checkItem(resultActions, 1, ItemProvider.DEFAULT_FEED_ID, ItemProvider.DEFAULT_OFFER_ID);
        StubPushApiTestUtils.checkDeliveryOptionsCount(resultActions, 1);
        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 1, StubPushApiTestUtils.ALL_POSTPAID, "Курьер", "DELIVERY", "0.00", 1, 1);
    }

    @Test
    public void testStubReturnsSingleOptionWhenNowAfterFirstOrderBefore() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION);

        List<Outlet> outlets = asList(OutletProvider.buildFirst(), OutletProvider.buildSecond());

        CartParameters cartParameters = new CartParameters(cartRequest);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, outlets);
        Item foundOffer = Iterables.getOnlyElement(cartParameters.getReportParameters().getCartRequest().getItems().values());
        ItemDeliveryOption before18 = new ItemDeliveryOption();
        before18.setOrderBefore(18);
        before18.setFromDay(1);
        before18.setToDay(3);
        before18.setPrice(BigDecimal.ZERO);
        before18.setPaymentMethods(asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        ItemDeliveryOption before20 = new ItemDeliveryOption();
        before20.setOrderBefore(20);
        before20.setFromDay(1);
        before20.setToDay(3);
        before20.setPrice(BigDecimal.TEN);
        before20.setPaymentMethods(asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        foundOffer.setDeliveryOptions(asList(before18, before20));
        cartParameters.setFakeNow(LocalDateTime.of(2018, Month.FEBRUARY, 8, 21, 0, 0));

        ResultActions resultActions = cartHelper.cart(cartParameters);

        LocalDate today = LocalDate.of(2018, Month.FEBRUARY, 8);

        checkItem(resultActions, 1, ItemProvider.DEFAULT_FEED_ID, ItemProvider.DEFAULT_OFFER_ID);
        StubPushApiTestUtils.checkDeliveryOptionsCount(resultActions, 1);
        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 1, StubPushApiTestUtils.ALL_POSTPAID, "Курьер", "DELIVERY", "0.00", 2, 4);
    }

    @Test
    public void shouldReturnCostZeroIfNotSpecified() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();
        cartRequest.setRegionId(DEFAULT_REGION_2);

        CartParameters cartParameters = new CartParameters(242103L, cartRequest);
        cartParameters.getReportGeoParameters()
                .setResourceUrl(
                        ItemProvider.DEFAULT_WARE_MD5,
                        Collections.singletonList(OutletProvider.buildFourth())
                );
        cartParameters.setFakeNow(LocalDateTime.of(2018, Month.FEBRUARY, 8, 21, 0, 0));


        ResultActions resultActions = cartHelper.cart(cartParameters);

        LocalDate today = LocalDate.of(2018, Month.FEBRUARY, 8);

        StubPushApiTestUtils.checkDeliveryOption(resultActions, today, 4, StubPushApiTestUtils.ONLY_CASH, "Самовывоз", "PICKUP", "0", 30, null);
    }
}

package ru.yandex.market.shopadminstub.stub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.report.model.outlet.Outlet;
import ru.yandex.market.helpers.CartHelper;
import ru.yandex.market.helpers.CartParameters;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.ItemDeliveryOptionProvider;
import ru.yandex.market.providers.ItemProvider;
import ru.yandex.market.providers.OutletProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.ItemDeliveryOption;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.ALL_POSTPAID;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.ONLY_CARD;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.ONLY_CASH;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkDeliveryOption;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkDeliveryOptionsCount;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkItem;

public class PickupOptionsMergeTest extends AbstractTestBase {
    @Autowired
    private CartHelper cartHelper;

    @Test
    public void testReturnsTwoPickupOptionsForTwoOutlets() throws Exception {
        int shopId = StubPushApiTestUtils.DEFAULT_SHOP_ID;

        List<ItemDeliveryOption> secondItemDeliveryOptions = buildSecondItemDeliveryOptions();

        Item secondItem = ItemProvider.buildItem(383183L, "2");
        secondItem.setDeliveryOptions(secondItemDeliveryOptions);

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(
                ItemProvider.buildItem(383182L, "1"), secondItem
        );
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION_2);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.of(today, LocalTime.of(20, 15, 30));

        List<Outlet> outlets = Arrays.asList(OutletProvider.buildFirst(), OutletProvider.buildThird());

        CartParameters cartParameters = new CartParameters(shopId, cartRequest);
        cartParameters.setFakeNow(now);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, outlets);

        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/cart/@delivery-currency").string("RUR"))
                .andExpect(xpath("/cart/items/item").nodeCount(2));

        checkItem(resultActions, 1, 383182L, "1");
        checkItem(resultActions, 2, 383183L, "2");

        checkDeliveryOption(resultActions, today, 1, ALL_POSTPAID, "Курьер", "DELIVERY", "100.00", 1, 2);
        checkDeliveryOption(resultActions, today, 2, ONLY_CASH, "Курьер", "DELIVERY", "0.00", 31, null);
        checkDeliveryOption(resultActions, today, 3, ONLY_CARD, "Самовывоз", "PICKUP", "50", 30, null);
        checkDeliveryOption(resultActions, today, 4, ONLY_CASH, "Самовывоз", "PICKUP", "50", 30, null);

        resultActions
                .andExpect(xpath("/cart/delivery-options/delivery[3]/outlets/outlet/@code").string("69"))
                .andExpect(xpath("/cart/delivery-options/delivery[4]/outlets/outlet/@code").string("70"));

        resultActions
                .andExpect(xpath("/cart/payment-methods/payment-method").nodeCount(2))
                .andExpect(xpath("/cart/payment-methods/payment-method[1]/text()").string(PaymentMethod.CASH_ON_DELIVERY.name()))
                .andExpect(xpath("/cart/payment-methods/payment-method[2]/text()").string(PaymentMethod.CARD_ON_DELIVERY.name()))
        ;
    }

    @Test
    public void testReturnsSinglePickupOptionForTwoOffers() throws Exception {
        int shopId = StubPushApiTestUtils.DEFAULT_SHOP_ID;

        CartRequest cartRequest = prepareCartRequest();

        LocalDateTime now = LocalDateTime.parse("2017-10-13T12:15:30");
        LocalDate today = now.toLocalDate();

        CartParameters cartParameters = new CartParameters(shopId, cartRequest);
        cartParameters.setFakeNow(now);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, Collections.singletonList(OutletProvider.buildFirst()));
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.ANOTHER_WARE_MD5, Collections.singletonList(OutletProvider.buildFirst()));

        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/cart/@delivery-currency").string(Currency.RUR.name()));

        checkItem(resultActions, 1, 383182L, "1");
        checkItem(resultActions, 2, 383183L, "2");

        checkDeliveryOption(resultActions, today, 1, ALL_POSTPAID, "Курьер", "DELIVERY", "100.00", 1, 2);
        checkDeliveryOption(resultActions, today, 2, ONLY_CASH, "Курьер", "DELIVERY", "0.00", 31, null);
        checkDeliveryOption(resultActions, today, 3, ONLY_CARD, "Самовывоз", "PICKUP", "50", 30, null);

        resultActions
                .andExpect(xpath("/cart/delivery-options/delivery[3]/outlets/outlet/@code").string("69"))
                .andExpect(xpath("/cart/payment-methods/payment-method").nodeCount(2))
                .andExpect(xpath("/cart/payment-methods/payment-method[1]/text()").string(PaymentMethod.CASH_ON_DELIVERY.name()))
                .andExpect(xpath("/cart/payment-methods/payment-method[2]/text()").string(PaymentMethod.CARD_ON_DELIVERY.name()));
    }

    @Test
    public void testReturnsNoPickupOptionForTwoOffersWithDifferentPaymentMethodsForOutlets() throws Exception {
        int shopId = StubPushApiTestUtils.DEFAULT_SHOP_ID;

        CartRequest cartRequest = prepareCartRequest();

        LocalDateTime now = LocalDateTime.parse("2017-10-13T12:15:30");
        LocalDate today = now.toLocalDate();

        Outlet first = OutletProvider.buildFirst();
        Outlet firstButCash = new Outlet(first.getId(),first.getSelfDeliveryRule(), Collections.singleton(PaymentMethod.CASH_ON_DELIVERY.name()));

        CartParameters cartParameters = new CartParameters(shopId, cartRequest);
        cartParameters.setFakeNow(now);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, Collections.singletonList(first));
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.ANOTHER_WARE_MD5, Collections.singletonList(firstButCash));

        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk());
        resultActions
                .andExpect(xpath("/cart/@delivery-currency").string(Currency.RUR.name()));
        StubPushApiTestUtils.checkDeliveryOptionsCount(resultActions, 2);

        resultActions
                .andExpect(xpath("/cart/payment-methods/payment-method").nodeCount(2))
                .andExpect(xpath("/cart/payment-methods/payment-method[1]/text()").string(PaymentMethod.CASH_ON_DELIVERY.name()))
                .andExpect(xpath("/cart/payment-methods/payment-method[2]/text()").string(PaymentMethod.CARD_ON_DELIVERY.name()));

        checkItem(resultActions, 1, 383182L, "1");
        checkItem(resultActions, 2, 383183L, "2");
        checkDeliveryOption(resultActions, today, 1, ALL_POSTPAID, "Курьер", "DELIVERY", "100.00", 1, 2);
        checkDeliveryOption(resultActions, today, 2, ONLY_CASH, "Курьер", "DELIVERY", "0.00", 31, null);
    }

    @Test
    public void testReturnsNoPickupOptionsForTwoItemsWithDifferentOutlets() throws Exception {
        int shopId = StubPushApiTestUtils.DEFAULT_SHOP_ID;

        CartRequest cartRequest = prepareCartRequest();

        LocalDateTime now = LocalDateTime.parse("2017-10-13T12:15:30");
        LocalDate today = now.toLocalDate();

        CartParameters cartParameters = new CartParameters(shopId, cartRequest);
        cartParameters.setFakeNow(now);
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.DEFAULT_WARE_MD5, Collections.singletonList(OutletProvider.buildFirst()));
        cartParameters.getReportGeoParameters().setResourceUrl(ItemProvider.ANOTHER_WARE_MD5, Collections.singletonList(OutletProvider.buildThird()));


        ResultActions resultActions = cartHelper.cart(cartParameters)
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/cart/@delivery-currency").string(Currency.RUR.name()))
                .andExpect(xpath("/cart/delivery-options/delivery").nodeCount(2))
                .andExpect(xpath("/cart/payment-methods/payment-method").nodeCount(2))
                .andExpect(xpath("/cart/payment-methods/payment-method[1]/text()").string(PaymentMethod.CASH_ON_DELIVERY.name()))
                .andExpect(xpath("/cart/payment-methods/payment-method[2]/text()").string(PaymentMethod.CARD_ON_DELIVERY.name()));

        checkItem(resultActions, 1, 383182L, "1");
        checkItem(resultActions, 2, 383183L, "2");
        checkDeliveryOptionsCount(resultActions, 2);
        checkDeliveryOption(resultActions, today, 1, ALL_POSTPAID, "Курьер", "DELIVERY", "100.00", 1, 2);
        checkDeliveryOption(resultActions, today, 2, ONLY_CASH, "Курьер", "DELIVERY", "0.00", 31, null);
    }

    private static CartRequest prepareCartRequest() {
        Item first = ItemProvider.buildItem(383182L, "1");
        first.setDeliveryOptions(buildFirstItemDeliveryOptions());

        Item second = ItemProvider.buildItem(383183L, "2");
        second.setWareMd5(ItemProvider.ANOTHER_WARE_MD5);
        second.setDeliveryOptions(buildSecondItemDeliveryOptions());

        CartRequest cartRequest = CartRequestProvider.buildCartRequest(first, second);
        cartRequest.setRegionId(CartRequestProvider.DEFAULT_REGION_2);
        return cartRequest;
    }

    private static List<ItemDeliveryOption> buildFirstItemDeliveryOptions() {
        ItemDeliveryOption fastest = ItemDeliveryOptionProvider.buildFastest();
        fastest.setOrderBefore(14);

        ItemDeliveryOption mostExpensive = ItemDeliveryOptionProvider.buildMostExpensive();
        mostExpensive.setOrderBefore(18);

        return Arrays.asList(
                ItemDeliveryOptionProvider.buildFree(),
                ItemDeliveryOptionProvider.buildAverage(),
                fastest,
                mostExpensive
        );
    }

    private static List<ItemDeliveryOption> buildSecondItemDeliveryOptions() {
        ItemDeliveryOption free = ItemDeliveryOptionProvider.buildFree();
        free.setPaymentMethods(ALL_POSTPAID);

        ItemDeliveryOption average = ItemDeliveryOptionProvider.buildAverage();
        average.setPaymentMethods(ONLY_CARD);

        ItemDeliveryOption fastest = ItemDeliveryOptionProvider.buildFastest();
        fastest.setPaymentMethods(ALL_POSTPAID);

        return Arrays.asList(free, average, fastest);
    }
}

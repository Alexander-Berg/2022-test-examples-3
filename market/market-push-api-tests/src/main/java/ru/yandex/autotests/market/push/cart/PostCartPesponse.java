package ru.yandex.autotests.market.push.cart;

import org.junit.Test;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.pushapi.beans.response.PushApiResponse;
import ru.yandex.autotests.market.pushapi.beans.response.builders.ResponsePushApiCartBuilder;
import ru.yandex.autotests.market.pushapi.data.ShopResource;
import ru.yandex.autotests.market.pushapi.data.bodies.DeliveryProvider;
import ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;

import static ru.yandex.autotests.market.pushapi.data.bodies.ItemProvider.createSimpleFirstItem;
import static ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse.formatResponseAsJson;

/**
 * Вспомогательная утилита, которая кладет нужную заглушку в ответ карт магазина.
 */
public class PostCartPesponse {

    public static final String CART = "manual_cart";
    private static PushApiCompareSteps tester = new PushApiCompareSteps();
    private static ShopResponse shopResponse = new ShopResponse(ShopResource.CART);

    @Test
    public void postResponseFromFile() {
        // manual_cart_shop_response в этот файл положить нужный ответ магазина
        final String manualCart = shopResponse.getResponseForCase(CART);
        tester.setShopResponse(manualCart, 349120, ShopResource.CART);
    }

    @Test
    public void postResponseFromGeneratedSource() {
        // сгенерировать
        PushApiResponse response = new PushApiResponse(new ResponsePushApiCartBuilder()
                .withItems(createSimpleFirstItem(1, true))
                .withPaymentMethods(PaymentMethod.CARD_ON_DELIVERY)
                .withDeliveryOptions(
                        DeliveryProvider.simplePickup(),
                        DeliveryProvider.simpleDelivery())
                .build());
        tester.setShopResponse(formatResponseAsJson(response), 349120, ShopResource.CART);
    }

}

package ru.yandex.autotests.market.push.cart;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.push.api.beans.request.cart.Cart;
import ru.yandex.autotests.market.push.api.beans.request.cart.DeliveryType;
import ru.yandex.autotests.market.pushapi.beans.response.PushApiResponse;
import ru.yandex.autotests.market.push.api.beans.response.cart.PushApiCartResponse;
import ru.yandex.autotests.market.pushapi.data.ShopResource;
import ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider;
import ru.yandex.autotests.market.pushapi.data.bodies.ItemProvider;
import ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider;
import ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse;
import ru.yandex.autotests.market.pushapi.steps.CartSteps;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.market.common.steps.AssertSteps.assertStep;
import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider.formatRequestAsXML;
import static ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse.formatResponseAsJson;


/**
 * Created by poluektov on 13.10.16.
 */
@Feature("Cart resource")
@Aqua.Test(title = "Оформление заказа для оффера с тэгом CPA NO в фиде")
public class CheckoutOfferWithCpaNoTest {
    private static final long SHOP_ID = ShopIdProvider.SHOP_ELP_JSON_PI;
    private PushApiCartResponse response;

    @Before
    public void setValidResponse() {
        PushApiResponse pushApiResponse = ShopResponse.makeShopResponseCartForPickup(ItemProvider.offerCpaNoPI());
        new PushApiCompareSteps().setShopResponse(formatResponseAsJson(pushApiResponse), SHOP_ID, ShopResource.CART);
        Cart cartRequestBody = new CartRequestProvider().createRequestForShopResponse(pushApiResponse, SHOP_ID, 213, DeliveryType.PICKUP);
        PushApiRequestData cartRequest = requestWithBodyForCase(SHOP_ID, formatRequestAsXML(cartRequestBody), "");
        response = new CartSteps().getCartResponse(cartRequest);
    }

    @Test
    @Title("Пушапи возвращает delivery false для оффера с CPA NO")
    public void testDeliveryFalse() {
        assertStep("в ответе не пустой список айтемов", response.getItems(), notNullValue());
        assertStep("delivery = false", response.getItems().getItems().get(0).getDelivery(), equalTo("false"));
    }
}

package ru.yandex.autotests.market.push.log;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.push.api.beans.request.cart.Cart;
import ru.yandex.autotests.market.push.api.beans.request.cart.DeliveryType;
import ru.yandex.autotests.market.pushapi.beans.response.PushApiResponse;
import ru.yandex.autotests.market.pushapi.data.ShopResource;
import ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider;
import ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider;
import ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse;
import ru.yandex.autotests.market.pushapi.steps.CartSteps;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.utils.ShopAdminStubLogProvider;
import ru.yandex.qatools.allure.annotations.Issue;

import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider.formatRequestAsXML;
import static ru.yandex.autotests.market.pushapi.data.bodies.ItemProvider.offerOnStockPI;
import static ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse.formatResponseAsJson;

/**
 * Created by poluektov on 31.08.16.
 */

//TODO: NEED INFO -> выяснить воспроизводится ли кейс, если да, то закончить, иначе удалить
//@Aqua.Test(title = "Тест лога market-checkout-shopadmin-stub.log.shell")
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-2641")
@Feature("Logs")
public class ShopAdminStubLogTest {
    private long shopId = ShopIdProvider.SHOP_ELP_JSON_PI;
    private final int REGION = 213;

    @Before
    public void makeCartRequest() {
        PushApiResponse pushApiResponse = ShopResponse.makeShopResponseCartForPickup(offerOnStockPI());
        new PushApiCompareSteps().setShopResponse(formatResponseAsJson(pushApiResponse), shopId, ShopResource.CART);
        Cart cartRequestBody = new CartRequestProvider()
                .createRequestForShopResponse(pushApiResponse, shopId, REGION, DeliveryType.PICKUP);
        new CartSteps().getCartResponse(requestWithBodyForCase(shopId, formatRequestAsXML(cartRequestBody), ""));
    }

    @Test
    public void testLogContainsReportRequest() {
        String result = ShopAdminStubLogProvider.grepLogContent("Requesting report");
    }
}



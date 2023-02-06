package ru.yandex.autotests.market.push.cart;

import org.junit.Test;
import org.openqa.jetty.http.HttpResponse;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.pushapi.beans.config.ShopConfig;
import ru.yandex.autotests.market.pushapi.beans.config.ShopConfigBuilder;
import ru.yandex.autotests.market.pushapi.data.ShopResource;
import ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider;
import ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.steps.PushApiSteps;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.market.common.steps.AssertSteps.assertStep;
import static ru.yandex.autotests.market.pushapi.data.WrongTokenRequestData.validWrongTokenRequest;


@Aqua.Test
@Description("Ручка /cart/wrong-token возвращает 422, если /cart 500")
@Features("Cart")
@Stories("Cart.WrongToken")
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-5529")
public class CartWrongTokenErrorTest {

    private static final int STATUS = HttpResponse.__422_Unprocessable_Entity;
    @Parameter("shop-id")
    private final Long shopId = ShopIdProvider.SHOP_ELP_XML;
    private final PushApiCompareSteps pushApi = new PushApiCompareSteps();
    private final ShopResponse shopResponse = new ShopResponse(ShopResource.CART);

    @Test
    public void wrongTokenStatusTest() {
        pushApi.setShopResponse(
                shopResponse.getResponseForCase("internal_error"),
                shopId,
                ShopResource.CART
        );
        PushApiSteps.setConfigForShop(
                shopId,
                new ShopConfigBuilder()
                        .withToken("token")
                        .withDataType(ShopConfig.ConfigDataType.XML)
                        .withAuthType(ShopConfig.ConfigAuthType.URL)
                        .build()
        );
        PushApiRequestData request = validWrongTokenRequest(shopId);
        assertStep("статус в ответе равен " + STATUS, pushApi.getHttpResponseStatus(request), is(STATUS));
    }
}

package ru.yandex.autotests.market.push.cart;


import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.push.api.beans.request.cart.Cart;
import ru.yandex.autotests.market.push.api.beans.request.cart.DeliveryType;
import ru.yandex.autotests.market.pushapi.beans.response.PushApiResponse;
import ru.yandex.autotests.market.pushapi.data.ShopResource;
import ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider;
import ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider;
import ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.steps.StorageSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.market.pushapi.beans.PushApiDataUtils.getEnclosingClassName;
import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider.formatRequestAsXML;
import static ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse.formatResponseAsJson;

/**
 * Created by strangelet on 07.09.15.
 */
@Feature("Cart resource")
@Aqua.Test(title = "Проверка  длины deliveryid в ответе push_api.")
@RunWith(Parameterized.class)
@Issue("AUTOTESTMARKET-1075")
public class LongDeliveryIdTest {

    private static ShopResponse shopResponse = new ShopResponse(ShopResource.CART);
    private static long shopId = ShopIdProvider.SHOP_ELP_JSON;
    private static PushApiCompareSteps pushApiSteps = new PushApiCompareSteps();
    private CartRequestProvider cartRequestProvider = new CartRequestProvider();
    private StorageSteps storageSteps = new StorageSteps();
    private PushApiRequestData requestData;
    private PushApiResponse pushApiResponse;
    @Parameterized.Parameter(0)
    public int idLength;


    @Parameterized.Parameters(name = "cart with deliveryId length {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{51},
                new Object[]{50},
                new Object[]{0}

        );
    }

    @Before
    public void prepare() {
        pushApiResponse = shopResponse.takeCartWithDeliveryIdLength(idLength);
        pushApiSteps.setShopResponse(formatResponseAsJson(pushApiResponse), shopId, ShopResource.CART);
        Cart cartRequest = cartRequestProvider.createRequestForShopResponse(pushApiResponse, shopId, DeliveryType.PICKUP);
        String requestBody = formatRequestAsXML(cartRequest);
        requestData = requestWithBodyForCase(shopId, requestBody, getEnclosingClassName(2) + shopId + idLength);
        //pushApiSteps.saveExpectedToStorage(requestData);   // сохраналка  результатов на элиптикс
    }

    @BeforeClass
    public static void prepareValidShopConfig() {
        pushApiSteps.setValidJsonShopConfig(shopId);
    }

    @Test
    public void testCompareWithCachedResponse() {
        pushApiSteps.compareWithStoredResult(requestData);
    }


    @After
    public void setValidShopResponse() {
        PushApiResponse cart = new ShopResponse(ShopResource.CART).cartWithDeliveryAndCount(1, true, true);
        pushApiSteps.setShopResponse(ShopResponse.formatResponseAsJson(cart), requestData, ShopResource.CART);
        //storageSteps.checkCartHaveNoErrorsLogs(shopId, requestData);
    }


}

package ru.yandex.autotests.market.push.cart;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.common.request.utils.BodyUtils;
import ru.yandex.autotests.market.push.api.beans.response.cart.PushApiCartResponse;
import ru.yandex.autotests.market.pushapi.steps.CartSteps;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.autotests.market.pushapi.request.PushApiTestCase;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider.*;

/**
 * Created by poluektov on 03.08.16.
 */
@Feature("Cart resource")
@Aqua.Test(title = "тесты cart на auto-shop")
@RunWith(Parameterized.class)
public class AutoShopCartTest {

    private PushApiCartResponse response;
    private PushApiCartResponse requestBody;
    private PushApiRequestData requestData;
    private Long shopId;
    private Boolean shouldContainsError;


    private static PushApiCompareSteps compareSteps = new PushApiCompareSteps();
    private CartSteps cartSteps = new CartSteps();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new PushApiTestCase("MBI-8842-cart"), false},
                {new PushApiTestCase("MBI-8841-cart"), true},
                {new PushApiTestCase("MBI-8839-feed-category-id"), true},
                {new PushApiTestCase("MBI-8843"), true},
                {new PushApiTestCase("MBI-8842-err-cart"), true}
        });
    }

    public AutoShopCartTest(PushApiTestCase testCase, Boolean containsError) {
        this.requestData = requestWithBodyForCase(testCase.getShopId(), testCase.getTestCase());
        this.requestBody = BodyUtils.unmarshalAsXml(PushApiCartResponse.class ,requestData.getBody());
        this.shouldContainsError = containsError;
    }

    @BeforeClass
    public static void initElpConfig() {
        compareSteps.setValidAutoGenShopConfig(SHOP_ELP_JSON_AUTO);
    }

    @Test
    public void testSvnShop() {
        shopId = SHOP_SVN_JSON_AUTO;
        checkResponse();
    }

    @Test
    public void testElpShop() {
        shopId = SHOP_ELP_JSON_AUTO;
        checkResponse();
    }

    @Test
    public void testAutoShop() {
        shopId = SHOP_AUTO;
        checkResponse();
    }

    public void checkResponse() {
        requestData = requestData.withShopId(shopId);

        response = cartSteps.getCartResponse(requestData);
        if (shouldContainsError) {
            compareSteps.compareWithStoredResult(requestData);
        } else {
            compareSteps.compareResponseWithRequestBody(response, requestBody.getItems().getItems());
        }
    }
}

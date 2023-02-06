package ru.yandex.autotests.market.push.cart;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.pushapi.beans.response.PushApiResponse;
import ru.yandex.autotests.market.pushapi.data.ShopResource;
import ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.steps.StorageSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider.*;

/**
 * User: strangelet
 * Date: 25.08.15
 * Time: 12:34
 * <p>
 */
@Feature("Cart resource")
@Aqua.Test(title = "Тестирование ответа с count = 0 ")
@RunWith(Parameterized.class)
@Issue("AUTOTESTMARKET-748")
public class CartCountErrorTest {


    private transient static final Logger LOG = LogManager.getLogger(CartCountErrorTest.class);

    private static PushApiCompareSteps tester = new PushApiCompareSteps();
    private static StorageSteps storageSteps = new StorageSteps();

    private PushApiRequestData requestData;

    private String shopResponse;
    private static long shopId = SHOP_ELP_JSON;
    private String testcase;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        ShopResponse shopResponse = new ShopResponse(ShopResource.CART);
        boolean deliveryTrue = true;
        boolean deliveryOptionsTrue = true;
        boolean deliveryFalse = false;
        boolean deliveryOptionsEmpty = false;

        return Arrays.asList(
                new Object[]{
                        "Count 0 , deliveryOptions not empty, delivery true",
                        shopResponse.cartWithDeliveryAndCount(0, deliveryOptionsTrue, deliveryTrue)},
                new Object[]{
                        "Count 0 , deliveryOptions empty, delivery true",
                        shopResponse.cartWithDeliveryAndCount(0, deliveryOptionsEmpty, deliveryTrue)},
                new Object[]{
                        "Count 0 , deliveryOptions empty, delivery false",
                        shopResponse.cartWithDeliveryAndCount(0, deliveryOptionsEmpty, deliveryFalse)},
                new Object[]{
                        "Count 0 , deliveryOptions not empty, delivery false",
                        shopResponse.cartWithDeliveryAndCount(0, deliveryOptionsTrue, deliveryFalse)},
                new Object[]{
                        "Count 1 , deliveryOptions empty, delivery empty",
                        shopResponse.cartWithDeliveryAndCount(1, deliveryOptionsEmpty, null)}

        );
    }

    public CartCountErrorTest(String testcase, PushApiResponse shopResponse) {
        this.testcase = testcase;
        this.requestData = requestWithBodyForCase(shopId, "simple").withCase(testcase);
        this.shopResponse = ShopResponse.formatResponseAsJson(shopResponse);
    }

    @BeforeClass
    public static void prepareValidShopConfig() {
        tester.setValidJsonShopConfig(shopId);
    }

    @Before
    public void prepareEnvironment() {

        tester.setShopResponse(shopResponse, requestData, ShopResource.CART);
        //tester.saveExpectedToStorage(requestData);   // сохраналка  результатов на элиптикс
    }


    @Test
    public void testCompareWithCachedResponse() {
        tester.compareWithStoredResult(requestData);
    }


    @After
    public void setValidShopResponse() {
        PushApiResponse cart = new ShopResponse(ShopResource.CART).cartWithDeliveryAndCount(1, true, true);
        tester.setShopResponse(ShopResponse.formatResponseAsJson(cart), requestData, ShopResource.CART);
        storageSteps.checkCartHaveNoErrorsLogs(shopId, requestData);
    }


}

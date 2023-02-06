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
import ru.yandex.autotests.market.pushapi.data.ShopResource;
import ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.autotests.market.pushapi.request.PushApiTestCase;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.market.pushapi.data.CartRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider.*;

/**
 * User: jkt & strangelet xxldoctor@yandex-team.ru,strangelet@yandex-team.ru,jkt@yandex-team.ru,zubikova@yandex-team.ru, syrisko@yandex-team.ru
 * Date: 05.06.13
 * Time: 12:34
 * <p>
 */
@Feature("Cart resource")
@Aqua.Test(title = "BackToBack тесты сравнения с сохраненным результатом")
@RunWith(Parameterized.class)
public class CartTest {

    private transient static final Logger LOG = LogManager.getLogger(CartTest.class);

    private static PushApiCompareSteps tester = new PushApiCompareSteps();
    private static ShopResponse pushApiShopResponse = new ShopResponse(ShopResource.CART);

    private PushApiRequestData requestData;
    private String shopResponse;


    static final String withTwoItems = pushApiShopResponse.cartWithTwoItems();


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        final String withOneItem = pushApiShopResponse.cartWithOneItem();
        final String duplicateOfferId = pushApiShopResponse.cartWithDuplicateOfferId();
        final String withoutItems = pushApiShopResponse.cartWithoutItems();

        return Arrays.asList(new Object[][]{

                // с  url api http://guppy.yandex.ru:39003 (ответ магазина  поставляет тест в elliptics )
                {new PushApiTestCase(SHOP_ELP_XML, "MBI-9361", duplicateOfferId)}   //   не корректный ответ магазина
                , {new PushApiTestCase(SHOP_ELP_XML, "without_items", withoutItems)} //   не корректный ответ магазина
                , {new PushApiTestCase(SHOP_ELP_XML, "with_one_item", withoutItems)} //   не корректный ответ магазина
                , {new PushApiTestCase(SHOP_ELP_XML, "cartResponse_is_greater", withTwoItems)}
                , {new PushApiTestCase(SHOP_ELP_XML, "emptyBody", withOneItem)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8837", withTwoItems)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8838", withTwoItems)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8839-offer-id", withTwoItems)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8839-offer-name", withTwoItems)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8839-feed-category-id", withTwoItems)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8841-cart", withTwoItems)}  //  не верная валюта
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8842-err-cart", withTwoItems)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8842-cart", withTwoItems)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8843", withTwoItems)} //  изменён на нет region-id
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8927", withTwoItems)}
                , {new PushApiTestCase(SHOP_ELP_XML, "extra_item", withTwoItems)}

                // с url api https://mbi1ft.yandex.ru:39005 (ответ магазина из svn)
                , {new PushApiTestCase(SHOP_SVN_XML, "svn_cart", null)}  // тут хватит одной проверки, что работает.
                , {new PushApiTestCase(SHOP_SVN_JSON, "svn-cart-json", null)}

                // с авто-генерацией ответа generate-data=on
                // , {new PushApiTestCase(TEST_SHOP_GD, "generate_data", withTwoItems)}
        });
    }

    public CartTest(PushApiTestCase testCase) {
        this.requestData = requestWithBodyForCase(testCase.getShopId(), testCase.getTestCase());
    }


    @BeforeClass
    public static void prepareValidShopConfig() {
        tester.setValidShopConfig();
    }

    @Before
    public void prepareEnvironment() {
        tester.setShopResponse(shopResponse, requestData, ShopResource.CART);
//        tester.saveExpectedToStorage(requestData);   // сохраналка  результатов на элиптикс
    }

    @Test
    public void testCompareWithCachedResponse() {
        tester.compareWithStoredResult(requestData);
    }

    @After
    public void setValidShopResponse() {
        tester.setShopResponse(withTwoItems, requestData, ShopResource.CART);
    }
}

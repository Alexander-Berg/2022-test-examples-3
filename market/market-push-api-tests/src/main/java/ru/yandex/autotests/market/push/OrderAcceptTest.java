package ru.yandex.autotests.market.push;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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

import static ru.yandex.autotests.market.pushapi.data.OrderAcceptRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider.*;

/**
 * User: jkt
 * Date: 05.06.13
 * Time: 12:34
 */
@Feature("order/accept resource")
@Aqua.Test(title = "BackToBack тесты сравнения с сохраненным результатом")
@RunWith(Parameterized.class)
public class OrderAcceptTest {

    private transient static final Logger LOG = LogManager.getLogger(OrderAcceptTest.class);

    private final String shopResponse;

    private static PushApiCompareSteps tester = new PushApiCompareSteps();

    private PushApiRequestData requestData;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        ShopResponse shopResponse = new ShopResponse(ShopResource.ORDER_ACCEPT);
        final String accepted = shopResponse.accepted();
        final String outOfDate = shopResponse.reasonOutOfDate();
        final String other = shopResponse.reasonOther();
        return Arrays.asList(new Object[][]{
                //  с url api https://mbi1ft.yandex.ru:39005 (ответ магазина из svn)
                {new PushApiTestCase(SHOP_SVN_XML, "validRequestWithUrlAuthorization", null)}
                //с  url api http://guppy.yandex.ru:39003 (ответ магазина  поставляет тест в elliptics )
                , {new PushApiTestCase(SHOP_ELP_XML, "inverted_date", accepted)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8841", outOfDate)}  //  не верная валюта
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8842", accepted)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8842-err", accepted)}
                , {new PushApiTestCase(SHOP_ELP_XML, "withoutPickupOutletId", accepted)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8845", accepted)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8846", outOfDate)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8846-postpaid", other)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8846-prepaid", accepted)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8847", outOfDate)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8847err", accepted)}
                , {new PushApiTestCase(SHOP_ELP_XML, "withоutItems", accepted)}
                , {new PushApiTestCase(SHOP_SVN_JSON, "svn-json-shop", null)}
        });
    }


    public OrderAcceptTest(PushApiTestCase testCase) {
        this.requestData = requestWithBodyForCase(testCase);
        this.shopResponse = testCase.getShopResponse();
    }

    @BeforeClass
    public static void prepareValidShopConfig (){
        tester.setValidShopConfig();
    }

    @Before
    public void saveExpected() {
        tester.setShopResponse(shopResponse, requestData, ShopResource.ORDER_ACCEPT);
//        tester.saveExpectedToStorage(requestData);   // сохраналка  результатов на элиптикс
    }

    @Test
    public void testCompareWithCachedResponse() {
        tester.compareWithStoredResult(requestData);
    }
}

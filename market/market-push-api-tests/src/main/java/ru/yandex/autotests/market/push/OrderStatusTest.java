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

import static ru.yandex.autotests.market.pushapi.data.OrderStatusRequestData.requestWithBodyForCase;
import static ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider.*;

/**
 * User: jkt
 * Date: 05.06.13
 * Time: 12:34
 */
@Feature("order/status resource")
@Aqua.Test(title = "BackToBack тесты сравнения с сохраненным результатом")
@RunWith(Parameterized.class)
public class OrderStatusTest {


    private transient static final Logger LOG = LogManager.getLogger(OrderStatusTest.class);
    private final String shopResponse;
    private final String caseName;

    private static PushApiCompareSteps tester = new PushApiCompareSteps();

    private PushApiRequestData requestData;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {

        ShopResponse shopResponse = new ShopResponse(ShopResource.ORDER_STATUS);
        final String status = shopResponse.status();
        return Arrays.asList(new Object[][]{
                {new PushApiTestCase(SHOP_ELP_XML, "empty", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "MBI-8930", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "orderWithoutId", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "placing", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "placingWithSubstatus", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "processing", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "processingWithSubstatus", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "reserved", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "reservedWithSubstatus", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "unpaid", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "unpaidWithSubstatus", status)}
                , {new PushApiTestCase(SHOP_ELP_XML, "prepaid_cash", status)}

                , {new PushApiTestCase(SHOP_SVN_XML, "currency-eur", null)}
                , {new PushApiTestCase(SHOP_SVN_JSON, "processing-svn-json", null)}

        });
    }

    public OrderStatusTest(PushApiTestCase testCase) {
        caseName = testCase.getTestCase();
        this.requestData = requestWithBodyForCase(testCase.getShopId(), caseName);
        this.shopResponse = testCase.getShopResponse();
    }

    @Before
    public void saveExpected() {
        tester.setShopResponse(shopResponse, requestData, ShopResource.ORDER_STATUS);
        //tester.saveExpectedToStorage(requestData, true);   // сохраналка  результатов на элиптикс
    }

    @BeforeClass
    public static void prepareValidShopConfig (){
        tester.setValidShopConfig();
    }

    @Test
    public void testCompareWithCachedResponse() {
        final String statusCode = tester.getResponseStatusLine(requestData);
        tester.compareWithStoredResult(caseName, requestData, statusCode);

    }
}

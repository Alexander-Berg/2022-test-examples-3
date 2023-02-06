package ru.yandex.autotests.market.push.settings;

import org.junit.Before;
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
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.market.pushapi.data.SettingsRequestData.settingsForTestShop;
import static ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider.*;
import static ru.yandex.autotests.market.pushapi.data.shopresponse.ShopResponse.*;

/**
 * User: jkt
 * Date: 05.06.13
 * Time: 12:34
 */
@Feature("Settings resource")
@Aqua.Test(title = "Проверка, что установка параметров отрабатывает без ошибок")
@RunWith(Parameterized.class)
@Issues({
        @Issue("https://st.yandex-team.ru/AUTOTESTMARKET-1841")})
public class SettingsTest {


    private PushApiCompareSteps tester = new PushApiCompareSteps();

    private PushApiRequestData requestData;
    private String shopResponse;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        ShopResponse shopResponse = new ShopResponse(ShopResource.SETTINGS);
        final String elpConfig = shopResponse.elpConfig();
        final String jsonConfig = shopResponse.jsonConfig();
        return
                Arrays.asList(new Object[][]{
                        {new PushApiTestCase(SHOP_ELP_XML, ELP_CONFIG, elpConfig)},
                        {new PushApiTestCase(SHOP_ELP_JSON, JSON_CONFIG, jsonConfig)},
                        {new PushApiTestCase(SHOP_SVN_XML, SVN_CONFIG, null)}  //  ответ из svn

                });
    }

    @Before
    public void prepareEnvironment() {
        tester.setShopResponse(shopResponse, requestData, ShopResource.SETTINGS);
        // tester.saveExpectedToStorage(requestData);   // сохраналка  результатов на элиптикс
    }


    public SettingsTest(PushApiTestCase testCase) {
        this.requestData = settingsForTestShop(testCase.getShopId(), testCase.getTestCase());
        this.shopResponse = testCase.getShopResponse();
    }

    @Test
    public void testCompareWithCachedResponse() {
        final String statusCode = tester.getResponseStatusLine(requestData);
        tester.assertStatusOK(statusCode);

    }
}

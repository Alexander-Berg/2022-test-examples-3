package ru.yandex.autotests.market.push.settings;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.pushapi.data.SettingsRequestData;
import ru.yandex.autotests.market.pushapi.data.bodies.ShopIdProvider;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.steps.SettingsSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;

import static ru.yandex.autotests.common.request.data.RequestType.HTTP_DELETE;
import static ru.yandex.autotests.common.request.data.RequestType.HTTP_POST;
import static ru.yandex.autotests.market.pushapi.data.SettingsRequestData.*;
import static ru.yandex.autotests.market.pushapi.utils.PushApiUtils.changeSettingsFormat;

/**
 * Created by strangelet on 04.09.15.
 */
@Feature("Settings resource")
@Aqua.Test(title = "Тест на проверку удаления/создания/сохранения настроек пушапи.")
@Issues({
        @Issue("https://st.yandex-team.ru/AUTOTESTMARKET-857"),
        @Issue("https://st.yandex-team.ru/AUTOTESTMARKET-1841")})
public class DeleteSettingsTest {


    private long shopId = ShopIdProvider.SHOP_ELP_JSON;
    private PushApiRequestData getRequest;
    private String settingsForTest;
    private PushApiCompareSteps tester = new PushApiCompareSteps();
    private SettingsSteps settingsSteps = new SettingsSteps();


    @Before
    public void prepareEnvironment() {
        settingsForTest = prepareShopUrl(SettingsRequestData.getBodies().getRequestBodyForCase(ShopIdProvider.getConfigNameByShopId(shopId)));
        getRequest = getSettingsRequest(shopId).withCase("DeleteSettingsTest_for" + shopId);
    }

    @Test
    public void checkPostNewSettings() {
        String newSettings = changeSettingsFormat(settingsForTest);
        PushApiRequestData newPostRequest = postSettingsForShop(shopId, newSettings);
        settingsSteps.postNewSettings(shopId, HTTP_POST, newPostRequest);
        settingsSteps.getSettings(shopId, getRequest, newSettings);
    }

    @Test
    public void checkDeleteSettings() {
        PushApiRequestData deleteRequest = deleteSettingsRequest(shopId);
        settingsSteps.postNewSettings(shopId, HTTP_DELETE, deleteRequest);
        //tester.saveExpectedToStorage(getRequest);   // сохраналка  результатов на элиптикс
        tester.compareWithStoredResult(getRequest);
    }


    @After
    public void returnToValideSettings() {
        PushApiRequestData postRequest = postSettingsForShop(shopId, settingsForTest);
        settingsSteps.postNewSettings(shopId, HTTP_POST, postRequest);
    }
}

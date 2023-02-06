package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import com.google.gson.Gson;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.fakebsproxy.beans.updatestorecontent.Properties;
import ru.yandex.autotests.direct.fakebsproxy.beans.updatestorecontent.UpdateStoreContentMobileResponseBean;
import ru.yandex.autotests.directapi.darkside.model.bslogs.mobile.CountryCurrencies;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.enums.ContentType;
import ru.yandex.autotests.directapi.enums.OSType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by buhter on 20/08/15.
 */

@Aqua.Test
@Features(FeatureNames.PPC_UPDATE_STORE_CONTENT_MONITOR)
@Issue("https://st.yandex-team.ru/TESTIRT-7246")
@Description("Проверка работоспособности ручки http://django.search.yandex.net:8101/process/app")
@RunWith(Parameterized.class)
public class DjangoExternalResourceTest {
    public static final ContentType CONTENT_TYPE = ContentType.APP;
    public static final CountryCurrencies COUNTRY = CountryCurrencies.RU;

    private DarkSideSteps darkSideSteps = new ApiSteps().userSteps.getDarkSideSteps();

    @Parameterized.Parameter(0)
    public String appId;

    @Parameterized.Parameter(1)
    public OSType osType;

    private Properties appProperties;

    @Parameterized.Parameters(name = "appId = {0}, osType = {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"id343200656", OSType.IOS}
                , {"com.kiloo.subwaysurf", OSType.ANDROID}

        });
    }

    @Step("Получение ответа от ручки")
    @Before
    public void init() {
        UpdateStoreContentMobileResponseBean testHandleResponse = darkSideSteps.getDjangoSearchNetAppContentSteps()
                .getTestHandleResponse(
                        appId
                        , CONTENT_TYPE
                        , COUNTRY
                        , osType
                );
        AllureUtils.addJsonAttachment("Полученный ответ от ручки http://django.search.yandex.net:8101/process/app"
                , new Gson().toJson(testHandleResponse));
        assumeThat("в ответе есть непустой Results", testHandleResponse.getResponse().getResults()
                , iterableWithSize(greaterThan(0)));
        assumeThat("в Results есть непустой Groups", testHandleResponse.getResponse().getResults().get(0).getGroups()
                , iterableWithSize(greaterThan(0)));
        assumeThat("в Results есть непустой Documents"
                , testHandleResponse.getResponse().getResults().get(0).getGroups().get(0).getDocuments()
                , iterableWithSize(greaterThan(0)));
        appProperties = testHandleResponse.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0)
                .getProperties();
    }

    @Test
    public void testIcon() {
        assertThat("получена информация об иконке приложения", appProperties.getIcon()
                , allOf(notNullValue(), not(equalTo(""))));
    }
}

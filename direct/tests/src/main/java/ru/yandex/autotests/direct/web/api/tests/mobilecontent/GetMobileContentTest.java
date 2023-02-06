package ru.yandex.autotests.direct.web.api.tests.mobilecontent;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.MobileContentInfoResponse;
import ru.yandex.autotests.direct.web.api.models.WebMobileContent;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.junit.Assert.assertEquals;

@Aqua.Test
@Description("Получение данных о мобильном приложении")
@Stories(TestFeatures.MobileContent.GET_MOBILE_CONTENT)
@Features(TestFeatures.MOBILE_CONTENT)
@Tag(TrunkTag.YES)
@Tag(Tags.MOBILE_CONTENT)
@Issue("DIRECT-83180")
public class GetMobileContentTest {
    private static final String LOGIN = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @Rule
    public DirectRule directRule = DirectRule.defaultRule().as(LOGIN);

    @Test
    public void getMobileContentTest_gplay() {
        String url = "https://play.google.com/store/apps/details?id=com.yandex.browser&hl=ru";
        WebMobileContent.OsTypeEnum expectedOs = WebMobileContent.OsTypeEnum.ANDROID;
        getMobileContent(url, expectedOs, "com.yandex.browser");
    }

    @Test
    public void getMobileContentTest_itunes() {
        String url = "https://itunes.apple.com/ru/app/id472650686";
        WebMobileContent.OsTypeEnum expectedOs = WebMobileContent.OsTypeEnum.IOS;
        getMobileContent(url, expectedOs, "id472650686");
    }

    private void getMobileContent(String url, WebMobileContent.OsTypeEnum expectedOs, String id) {
        MobileContentInfoResponse response = directRule.webApiSteps().mobileAppSteps().getMobileContent(url, LOGIN);
        assertEquals(id, response.getResult().getStoreContentId());
        assertEquals(expectedOs, response.getResult().getOsType());
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN).mobileContentSteps()
                .deleteMobileContent(response.getResult().getMobileContentId());
    }
}

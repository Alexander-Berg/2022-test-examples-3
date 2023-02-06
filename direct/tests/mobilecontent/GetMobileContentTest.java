package ru.yandex.autotests.direct.intapi.java.tests.mobilecontent;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.java.steps.MobileAppControllerSteps;
import ru.yandex.autotests.direct.intapi.models.MobileContentInfoResponse;
import ru.yandex.autotests.direct.intapi.models.WebMobileContent;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.apiclient.config.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;

@Aqua.Test
@Description("Проверка работы GetMobileContentInfo")
@Stories(TestFeatures.MobileContent.GET_MOBILE_CONTENT)
@Features(TestFeatures.MOBILE_CONTENT)
@Tag(Tags.MOBILE_CONTENT)
@Tag(TagDictionary.TRUNK)
@Issue("DIRECT-82720")
public class GetMobileContentTest {
    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN_MAIN);
    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();
    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    @Rule
    public Trashman trasher = new Trashman(api);
    private String LOGIN = LOGIN_MAIN;
    private MobileAppControllerSteps mobileAppControllerSteps;
    private Long clientId;

    @Before
    public void setUp() throws Exception {
        clientId = api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN)
                .usersSteps().getUser(LOGIN).getClientid();
        mobileAppControllerSteps = directClassRule.intapiSteps().mobileAppControllerSteps();
    }

    @Test
    public void getMobileContentInfo_gplay() {
        MobileContentInfoResponse info = mobileAppControllerSteps.getMobileContent(clientId,
                "https://play.google.com/store/apps/details?id=com.yandex.browser&hl=ru");
        WebMobileContent.OsTypeEnum expectedOs = WebMobileContent.OsTypeEnum.ANDROID;
        getMobileContent(info, expectedOs, "com.yandex.browser");
    }

    @Test
    public void getMobileContentInfo_itunes() {
        MobileContentInfoResponse info = mobileAppControllerSteps.getMobileContent(clientId,
                "https://itunes.apple.com/ru/app/id472650686");
        WebMobileContent.OsTypeEnum expectedOs = WebMobileContent.OsTypeEnum.IOS;
        getMobileContent(info, expectedOs, "id472650686");
    }

    private void getMobileContent(MobileContentInfoResponse info, WebMobileContent.OsTypeEnum expectedOs, String id) {
        assertEquals(expectedOs, info.getResult().getOsType());
        assertEquals(id, info.getResult().getStoreContentId());
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(LOGIN).mobileContentSteps()
                .deleteMobileContent(info.getResult().getMobileContentId());
    }
}

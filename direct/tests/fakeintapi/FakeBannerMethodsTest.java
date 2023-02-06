package ru.yandex.autotests.directintapi.tests.fakeintapi;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerFakeInfo;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: xy6er
 * https://jira.yandex-team.ru/browse/TESTIRT-1406
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.FAKE_METHODS)
public class FakeBannerMethodsTest {

    private static DarkSideSteps darkSideSteps;
    private static Long bid;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @BeforeClass
    public static void createBanner() {
        darkSideSteps = api.userSteps.getDarkSideSteps();
        //Проставим баллы для использования апи
        int units = 31999;
        darkSideSteps.getClientFakeSteps().setAPIUnits(Logins.LOGIN_MAIN, units);

        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        bid = api.userSteps.adsSteps().addDefaultTextAd(pid);
    }


    @Test
    public void getBannerParamsTest() {
        BannerFakeInfo bannerFakeInfo = darkSideSteps.getBannersFakeSteps().getBannerParams(bid);
        assertThat("Неверный bid у баннера", bannerFakeInfo.getBid(), equalTo(bid));
        assertNotNull(bannerFakeInfo.getLogin());
    }

    @Test
    public void updateBannerParamsTest() {
        String bannerID = String.valueOf(Math.abs(RandomUtils.getNextInt()));
        BannerFakeInfo bannerFakeInfo = darkSideSteps.getBannersFakeSteps().getBannerParams(bid);
        bannerFakeInfo.setBannerID(bannerID);
        darkSideSteps.getBannersFakeSteps().updateBannerParams(bannerFakeInfo);
        assertThat("Неверный bannerID у баннера",
                darkSideSteps.getBannersFakeSteps().getBannerParams(bid).getBannerID(), equalTo(bannerID));
    }

    @Test      //https://jira.yandex-team.ru/browse/DIRECT-27178
    public void canNotChangeBannerLoginTest() {
        BannerFakeInfo bannerFakeInfo = darkSideSteps.getBannersFakeSteps().getBannerParams(bid);
        String login = bannerFakeInfo.getLogin();
        bannerFakeInfo.setLogin("newLogin");
        darkSideSteps.getBannersFakeSteps().updateBannerParams(bannerFakeInfo);
        assertThat("Неверный Login у баннера",
                darkSideSteps.getBannersFakeSteps().getBannerParams(bid).getLogin(), equalTo(login));
    }

    @Test      //https://jira.yandex-team.ru/browse/DIRECT-27178
    public void canChangeBannerImageTest() {
        Long bannerIDWithImage = 306439294L;
        Long imageID = (long) RandomUtils.getNextInt(1000);
        BannerFakeInfo bannerFakeInfo = darkSideSteps.getBannersFakeSteps().getBannerParams(bannerIDWithImage);
        bannerFakeInfo.setImageImageID(imageID);
        bannerFakeInfo.setImageStatusModerate(Status.NEW);
        darkSideSteps.getBannersFakeSteps().updateBannerParams(bannerFakeInfo);
        bannerFakeInfo = darkSideSteps.getBannersFakeSteps().getBannerParams(bannerIDWithImage);
        assertThat("Неверный imageStatusModerate у баннера",
                bannerFakeInfo.getImageStatusModerate(), equalTo(Status.NEW));
        assertThat("Неверный imageImageID у баннера", bannerFakeInfo.getImageImageID(), equalTo(imageID));
    }

    @Test      //https://jira.yandex-team.ru/browse/DIRECT-27178
    public void canNotUpdateImageFieldsForBannerWithoutImageTest() {
        BannerFakeInfo bannerFakeInfo = darkSideSteps.getBannersFakeSteps().getBannerParams(bid);
        bannerFakeInfo.setImageImageID((long) RandomUtils.getNextInt(1000));
        bannerFakeInfo.setImageStatusModerate(Status.NEW);
        darkSideSteps.getBannersFakeSteps().updateBannerParams(bannerFakeInfo);
        bannerFakeInfo = darkSideSteps.getBannersFakeSteps().getBannerParams(bid);
        assertNull(bannerFakeInfo.getImageImageID());
        assertNull(bannerFakeInfo.getImageStatusModerate());
    }

}

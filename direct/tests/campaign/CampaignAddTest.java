package ru.yandex.autotests.directmonitoring.tests.campaign;


import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.directapi.common.api45.BannerInfo;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directmonitoring.tests.BaseDirectMonitoringTest;
import ru.yandex.autotests.directmonitoring.tests.Project;
import ru.yandex.autotests.directweb.objects.banners.BannerInfoWeb;
import ru.yandex.autotests.directweb.util.beanutils.MongoPropertyLoader;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.direct.utils.beans.BeanWrapper.wrap;
import static ru.yandex.autotests.directweb.util.converters.Banners.toBannerInfoWeb;
import static ru.yandex.autotests.directweb.util.matchers.IsDisplayedMatcher.isDisplayed;

/**
 * User: omaz
 * Date: 23.07.13
 * Time: 12:40
 */
@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.CAMPAIGN)
@Title("Создание кампании")
public class CampaignAddTest extends BaseDirectMonitoringTest {

    String campaignID;
    private BannerInfoWeb bannerInfoWeb;
    private ApiSteps apiSteps;

    @Override
    public void additionalActions() {}

    @Before
    public void before() {
        MongoPropertyLoader<BannerInfo> loader = new MongoPropertyLoader<>(BannerInfo.class);
        bannerInfoWeb = toBannerInfoWeb(loader.getBean("BannerWithRequiredFields"));
        bannerInfoWeb.getPhrases()[0].setPhrase(RandomStringUtils.random(10));
        AllureUtils.addJsonAttachment("Параметры баннера", wrap(bannerInfoWeb).toString());
        user.inCaptchaForm().fillCaptcha();
        apiSteps = new ApiSteps().as(CLIENT_LOGIN);
    }

    @Test
    @Title("Проверка добавления компании")
    public void campaignAdd(){
        user.inBrowserAddressBar().openNewCampaignParamsForm();
        user.onEditCampPage().saveCampaign();

        user.inCaptchaPopup().fillCaptcha();
        user.onAddNewBannerPage().fillBannerParameters(wrap(bannerInfoWeb));
        user.onAddNewBannerPage().goNextAndCheckNextPageURL();
        user.onAddNewBannerStep2Page().goNext();
        campaignID = user.inOperatingSystem().getCampaignIDFromURL();

        user.inBrowserAddressBar().openShowCampsPage();
        user.onShowCampsPage().shouldSeeCampaign(campaignID, isDisplayed());
    }

    @After
    public void deleteCampaign() {
        if (campaignID != null) {
            apiSteps.userSteps.campaignSteps().deleteCampaignsQuietly(Integer.valueOf(campaignID));
        }
    }

}

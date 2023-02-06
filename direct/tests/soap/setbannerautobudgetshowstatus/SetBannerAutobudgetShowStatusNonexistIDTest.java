package ru.yandex.autotests.directintapi.tests.soap.setbannerautobudgetshowstatus;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.GroupFakeInfo;
import ru.yandex.autotests.directapi.darkside.datacontainers.soap.SetBannerAutobudgetShowStatusRequestData;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.SoapClientSteps;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignNetworkStrategyAddMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.TextCampaignSearchStrategyAddMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: omaz
 * Date: 11.09.13
 * https://jira.yandex-team.ru/browse/TESTIRT-864
 */

@Aqua.Test(title = "setBannerAutobudgetShowStatus - несуществующие ID баннеров")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.SOAP_SET_BANNER_AUTOBUDGET_SHOW_STATUS)
public class SetBannerAutobudgetShowStatusNonexistIDTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    public String currentMethodName = SoapClientSteps.SET_AUTOBUDGET_SHOW_STATUS;
    private Long campaignId;
    private Long bannerID;
    private Long pid;
    private Long bannerIDForTest;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Before
    public void init() {
        log.info("Подготовка данных для теста");
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaignWithStrategies(
                new TextCampaignSearchStrategyAddMap().defaultWbMaximumClicks(Currency.RUB),
                new TextCampaignNetworkStrategyAddMap().defaultNetworkDefault()
        );
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        bannerID = api.userSteps.adsSteps().addDefaultTextAd(pid);
    }

    @Test
    public void setBannerAutobudgetShowStatusNonexistIDTest() {
        bannerIDForTest = 1L;
        log.info("Вызываем метод " + currentMethodName);
        darkSideSteps.getSoapClientSteps().checkSetBannerAutobudgetShowStatusNoErrors(
                currentMethodName, new SetBannerAutobudgetShowStatusRequestData().withBanner(bannerIDForTest, false)
        );      //silent mode
    }

    @Test
    //https://jira.yandex-team.ru/browse/DIRECT-24049
    public void setBannerAutobudgetShowStatusNegativeIDTest() {
        bannerIDForTest = (-1) * api.userSteps.bannersFakeSteps().getBannerParams(bannerID).getPid();
        log.info("Вызываем метод " + currentMethodName);
        darkSideSteps.getSoapClientSteps().setBannerAutobudgetShowStatusExpectedError(
                currentMethodName,
                new SetBannerAutobudgetShowStatusRequestData().withBanner(bannerIDForTest, false),
                startsWith("Incorrect GroupExportID")
        );

        log.info("Проверяем, что значение autobudgetShowStatus не поменялось");
        GroupFakeInfo groupFakeInfo = api.userSteps.groupFakeSteps().getGroupParams(pid);
        assertThat("Значение autobudgetShowStatus изменилось", groupFakeInfo.getStatusAutobudgetShow(), equalTo("Yes"));
    }

    @Test
    //https://jira.yandex-team.ru/browse/DIRECT-24401
    public void setBannerAutobudgetShowStatusZeroIDTest() {
        bannerIDForTest = 0L;
        log.info("Вызываем метод " + currentMethodName);
        darkSideSteps.getSoapClientSteps().setBannerAutobudgetShowStatusExpectedError(
                currentMethodName,
                new SetBannerAutobudgetShowStatusRequestData().withBanner(bannerIDForTest, false),
                startsWith("Incorrect GroupExportID")
        );
    }

    @Test
    public void setBannerAutobudgetShowStatusDeletedBannerTest() {
        bannerIDForTest = api.userSteps.bannersFakeSteps().getBannerParams(bannerID).getPid();
        api.userSteps.adsSteps().adsDelete(campaignId, bannerID);
        log.info("Вызываем метод " + currentMethodName);
        darkSideSteps.getSoapClientSteps().checkSetBannerAutobudgetShowStatusNoErrors(
                currentMethodName, new SetBannerAutobudgetShowStatusRequestData().withBanner(bannerIDForTest, false)
        );      //silent mode
    }

}

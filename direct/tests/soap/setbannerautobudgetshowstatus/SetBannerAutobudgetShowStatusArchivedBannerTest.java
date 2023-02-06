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

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: omaz
 * Date: 11.09.13
 * https://jira.yandex-team.ru/browse/TESTIRT-864
 */

@Aqua.Test(title = "setBannerAutobudgetShowStatus - архивный баннер")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.SOAP_SET_BANNER_AUTOBUDGET_SHOW_STATUS)
public class SetBannerAutobudgetShowStatusArchivedBannerTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    public String currentMethodName = SoapClientSteps.SET_AUTOBUDGET_SHOW_STATUS;
    private Long pid;
    private Long bannerIDForTest;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @Before
    public void init() {
        log.info("Подготовка данных для теста");
        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaignWithStrategies(
                new TextCampaignSearchStrategyAddMap().defaultWbMaximumClicks(Currency.RUB),
                new TextCampaignNetworkStrategyAddMap().defaultNetworkDefault()
        );
        pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        Long bannerID = api.userSteps.adsSteps().addDefaultTextAd(pid);
        api.userSteps.makeBannerActive(bannerID);
        //архивируем баннер
        api.userSteps.adsSteps().adsSuspend(bannerID);
        api.userSteps.adsSteps().adsArchive(bannerID);
        bannerIDForTest = api.userSteps.bannersFakeSteps().getBannerParams(bannerID).getPid();
    }

    @Test
    public void setBannerAutobudgetShowStatusArchivedBannerTest() {
        log.info("Вызываем метод " + currentMethodName);
        darkSideSteps.getSoapClientSteps().checkSetBannerAutobudgetShowStatusNoErrors(
                currentMethodName, new SetBannerAutobudgetShowStatusRequestData().withBanner(bannerIDForTest, false)
        );
        log.info("Проверяем, что значение autobudgetShowStatus поменялось");
        GroupFakeInfo groupFakeInfo = api.userSteps.groupFakeSteps().getGroupParams(pid);
        assertThat("Не удалось изменить значение autobudgetShowStatus", groupFakeInfo.getStatusAutobudgetShow(),
                equalTo("No"));
    }

}

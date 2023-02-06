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

@Aqua.Test(title = "setBannerAutobudgetShowStatus - два баннера")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.SOAP_SET_BANNER_AUTOBUDGET_SHOW_STATUS)
public class SetBannerAutobudgetShowStatusMultipleBannersTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    public String currentMethodName = SoapClientSteps.SET_AUTOBUDGET_SHOW_STATUS;
    private Long bannerID1;
    private Long pid1;
    private Long bannerID2;
    private Long pid2;

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
        pid1 = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        bannerID1 = api.userSteps.adsSteps().addDefaultTextAd(pid1);
        pid2 = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        bannerID2 = api.userSteps.adsSteps().addDefaultTextAd(pid2);

    }

    @Test
    public void setBannerAutobudgetShowStatusMultipleBannersTest() {
        log.info("Вызываем метод " + currentMethodName);
        darkSideSteps.getSoapClientSteps().checkSetBannerAutobudgetShowStatusNoErrors(
                currentMethodName, new SetBannerAutobudgetShowStatusRequestData()
                        .withBanner(pid1, false)
                        .withBanner(pid2, false)
        );
        log.info("Проверяем, что значение autobudgetShowStatus поменялось");
        GroupFakeInfo groupFakeInfo1 = api.userSteps.groupFakeSteps().getGroupParams(pid1);
        GroupFakeInfo groupFakeInfo2 = api.userSteps.groupFakeSteps().getGroupParams(pid2);
        assertThat("Не удалось изменить значение autobudgetShowStatus у " + bannerID1,
                groupFakeInfo1.getStatusAutobudgetShow(), equalTo("No"));
        assertThat("Не удалось изменить значение autobudgetShowStatus у " + bannerID2,
                groupFakeInfo2.getStatusAutobudgetShow(), equalTo("No"));
    }

}

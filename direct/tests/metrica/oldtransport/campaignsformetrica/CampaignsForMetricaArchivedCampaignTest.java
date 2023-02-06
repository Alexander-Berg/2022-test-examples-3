package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.campaignsformetrica;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.yaml.CampaignsForMetricaResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.enums.AdGroupType;
import ru.yandex.autotests.directapi.enums.BannerType;
import ru.yandex.autotests.directapi.enums.CampaignType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * User: omaz
 * Date: 27.09.13
 * https://jira.yandex-team.ru/browse/TESTIRT-1044
 */
@Aqua.Test(title = "CampaignsForMetrica для архивной кампании")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.METRICA_CAMPAIGNS_FOR_METRICA)
@RunWith(Parameterized.class)
public class CampaignsForMetricaArchivedCampaignTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter(0)
    public CampaignType campaignType;

    @Parameterized.Parameter(1)
    public AdGroupType adGroupType;

    @Parameterized.Parameter(2)
    public BannerType bannerType;

    @Parameterized.Parameters(name = "campaignType = {0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {CampaignType.TEXT, AdGroupType.BASE, BannerType.TEXT},
                {CampaignType.DYNAMIC, AdGroupType.DYNAMIC, BannerType.DYNAMIC},
        };
        return Arrays.asList(data);
    }

    @Test
    public void campaignsForMetricaArchivedCampaignTest() {
        int uid = Integer.valueOf(api.userSteps.clientFakeSteps()
                .getClientData(Logins.LOGIN_MAIN).getPassportID());

        long campaignId = api.userSteps.campaignSteps().addDefaultCampaign(campaignType);
        long adGroupID = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId, adGroupType);
        api.userSteps.adsSteps().addDefaultAd(adGroupID, bannerType);
        api.userSteps.campaignSteps().campaignsSuspend(campaignId);
        api.userSteps.campaignSteps().campaignsArchive(campaignId);
        CampaignsForMetricaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().campaignsForMetricaNoErrors(uid);
        CampaignsForMetricaResponse.CampaignData campaignData =
                selectFirst(response.getCampaignList(),
                        having(on(CampaignsForMetricaResponse.CampaignData.class).getCid(), equalTo(campaignId)));

        assertThat("Кампания " + campaignId + "не должна была попасть в ответ",
                campaignData,
                nullValue()
        );
    }

}

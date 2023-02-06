package ru.yandex.autotests.directintapi.tests.metrica.oldtransport.campaignsformetrica;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import com.yandex.direct.api.v5.ads.AdFieldEnum;
import com.yandex.direct.api.v5.ads.GetResponse;
import com.yandex.direct.api.v5.ads.TextAdFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignTypeEnum;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.yaml.CampaignsForMetricaResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.enums.AdGroupType;
import ru.yandex.autotests.directapi.enums.BannerType;
import ru.yandex.autotests.directapi.enums.CampaignType;
import ru.yandex.autotests.directapi.model.api5.ads.AdsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.GetRequestMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * User: xy6er
 * Date: 19.11.14
 * https://st.yandex-team.ru/TESTIRT-3365
 */
@Aqua.Test(title = "CampaignsForMetrica - Отдавать кампании Метрике для любого из представителей клиентов")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.METRICA_CAMPAIGNS_FOR_METRICA)
@RunWith(Parameterized.class)
public class CampaignsForMetricaRepresentativeTest {
    private static DarkSideSteps darkSideSteps;
    private static final String MAIN_LOGIN = "at-intapi-test1";
    private static int uid;
    private static final String REP_LOGIN = "at-intapi-test1-rep";
    private static int repUid;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(MAIN_LOGIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Parameterized.Parameter(0)
    public CampaignType campaignType;

    @Parameterized.Parameter(1)
    public AdGroupType adGroupType;

    @Parameterized.Parameter(2)
    public BannerType bannerType;

    private Long campaignId;
    private Long adGroupID;
    private Long adID;
    private int orderID;

    @Parameterized.Parameters(name = "campaignType = {0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {CampaignType.TEXT, AdGroupType.BASE, BannerType.TEXT},
                {CampaignType.DYNAMIC, AdGroupType.DYNAMIC, BannerType.DYNAMIC},
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void initTestData() {
        darkSideSteps = new DarkSideSteps();
        uid = Integer.valueOf(api.userSteps.clientFakeSteps().getClientData(MAIN_LOGIN).getPassportID());
        repUid = Integer.valueOf(api.userSteps.clientFakeSteps().getClientData(REP_LOGIN).getPassportID());
    }

    @Before
    public void prepareCampaigns() {
        campaignId = api.userSteps.campaignSteps().addDefaultCampaign(campaignType);
        adGroupID = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId, adGroupType);
        adID = api.userSteps.adsSteps().addDefaultAd(adGroupID, bannerType);
        orderID = api.userSteps.campaignFakeSteps().setRandomOrderID(campaignId);
    }

    @Test
    public void repCanSeeNewCampaignInCampaignsForMetricaTest() {
        CampaignsForMetricaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().campaignsForMetricaNoErrors(repUid, true);
        Optional<CampaignsForMetricaResponse.CampaignData> campaignData = response.getCampaignList().stream()
                .filter(campData -> campData.getCid() == campaignId.intValue())
                .findFirst();

        com.yandex.direct.api.v5.campaigns.GetResponse campaignInfo = api.userSteps.campaignSteps().campaignsGet(
                new GetRequestMap()
                        .withSelectionCriteria(
                                new CampaignsSelectionCriteriaMap()
                                        .withIds(campaignId)
                                        .withTypes(CampaignTypeEnum.TEXT_CAMPAIGN, CampaignTypeEnum.DYNAMIC_TEXT_CAMPAIGN))
                        .withFieldNames(CampaignFieldEnum.NAME, CampaignFieldEnum.STATISTICS),
                MAIN_LOGIN);
        assumeThat("получена кампания", campaignInfo.getCampaigns().size(), equalTo(1));
        GetResponse bannerInfo = api.userSteps.adsSteps().adsGet(new ru.yandex.autotests.directapi.model.api5.ads.GetRequestMap()
                .withSelectionCriteria(new AdsSelectionCriteriaMap().withIds(adID))
                .withFieldNames(AdFieldEnum.ID)
                .withTextAdFieldNames(TextAdFieldEnum.DISPLAY_DOMAIN));
        assumeThat("получено одно объявление", bannerInfo.getAds().size(), equalTo(1));
        CampaignsForMetricaResponse.CampaignData expected = new CampaignsForMetricaResponse.CampaignData();
        expected.setOrderID(String.valueOf(orderID));
        expected.setShows(JsonUtils.toString(campaignInfo.getCampaigns().get(0).getStatistics().getImpressions()));
        expected.setName(campaignInfo.getCampaigns().get(0).getName());
        if (campaignType == CampaignType.TEXT) {
            expected.setDomain(bannerInfo.getAds().get(0).getTextAd().getDisplayDomain().getValue());
        } else {
            expected.setDomain(null);
        }

        assertThat("Данные из ответа не совпадают с данными кампании",
                campaignData,
                beanEquals(expected));
    }

    @Test
    public void repCanSeeAllCampaignOfMainLoginInCampaignsForMetricaTest() {
        CampaignsForMetricaResponse response =
                darkSideSteps.getMetricaOldTransportSteps().campaignsForMetricaNoErrors(uid, true);
        CampaignsForMetricaResponse repResponse =
                darkSideSteps.getMetricaOldTransportSteps().campaignsForMetricaNoErrors(repUid, true);

        assertThat("Список кампаний для клиента расходиться со списком представителя",
                response.getCampaignList(),
                beanDiffer(repResponse.getCampaignList()));
    }
}

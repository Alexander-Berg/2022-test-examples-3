package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.util.List;

import com.yandex.direct.api.v5.adgroups.AdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.AdGroupGetItem;
import com.yandex.direct.api.v5.ads.AdFieldEnum;
import com.yandex.direct.api.v5.ads.AdGetItem;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import com.yandex.direct.api.v5.dynamictextadtargets.WebpageFieldEnum;
import com.yandex.direct.api.v5.dynamictextadtargets.WebpageGetItem;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.CopyCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.cmd.util.CampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.adgroups.AdGroupsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.ads.AdsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.dynamictextadtargets.WebpagesSelectionCriteriaMap;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;

@Aqua.Test
@Description("Проверка копирования ДТО кампании контроллером copyCamp")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.DYNAMIC)
public class CopyDynamicCampTest {

    private static final String DEFAULT_COPY_REQUEST = "cmd.copyCamp.request.default";
    private static final String MANAGER = Logins.MANAGER;
    private static final String CLIENT = "at-direct-b-copydyncamp";
    private static final int CAMPAIGNS_AMOUNT = 1;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private CopyCampRequest copyCampRequest;
    private Long campaignId;

    @After
    public void deleteCampaigns() {
        cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT);
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(MANAGER));

        cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT);

        cmdRule.getApiStepsRule().as(MANAGER);

        campaignId = cmdRule.apiSteps().campaignSteps().addDefaultDynamicTextCampaign(CLIENT);
        Long adgroupId = cmdRule.apiSteps().adGroupsSteps().addDefaultGroupDynamic(campaignId, CLIENT);
        cmdRule.apiSteps().adsSteps().addDefaultDynamicTextAd(adgroupId, CLIENT);
        cmdRule.apiSteps().dynamicTextAdTargetsSteps().addDefaultWebpage(adgroupId, CLIENT);

        copyCampRequest = BeanLoadHelper.loadCmdBean(DEFAULT_COPY_REQUEST, CopyCampRequest.class);
        copyCampRequest.setCidFrom(String.valueOf(this.campaignId));
        copyCampRequest.setNewLogin(CLIENT);
        copyCampRequest.setOldLogin(CLIENT);
    }


    @Test
    @Description("Проверяем параметры ДТО кампании при копировании кампаний")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9403")
    public void campaignParametersTest() {
        cmdRule.cmdSteps().copyCampSteps().postCopyCamp(copyCampRequest);
        CampaignHelper.copyCampaignsByScript(cmdRule, CLIENT, Long.parseLong(copyCampRequest.getCidFrom()));

        List<CampaignGetItem> campaigns = cmdRule.apiSteps().campaignSteps().getCampaigns(CLIENT);

        assertThat("Количество кампаний совпадает с ожидаемым для клиента " + CLIENT,
                campaigns.size(), equalTo(CAMPAIGNS_AMOUNT * 2));
        assertThat("Параметры кампании после копирования совпадают с исходными",
                campaigns.get(0),
                beanDiffer(campaigns.get(1))
                        .useCompareStrategy(allFieldsExcept(
                                newPath("id"),
                                newPath("notification", "emailSettings", ".*"), //настройки уведомлений — не копируются
                                newPath("clientInfo")
                        )));
    }

    @Test
    @Description("Проверяем параметры ДТО баннеров при копировании кампаний")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9404")
    public void bannerParametersTest() {
        cmdRule.cmdSteps().copyCampSteps().postCopyCamp(copyCampRequest);
        CampaignHelper.copyCampaignsByScript(cmdRule, CLIENT, Long.parseLong(copyCampRequest.getCidFrom()));

        List<CampaignGetItem> campaigns = cmdRule.apiSteps().campaignSteps().getCampaigns(CLIENT);
        Long copiedCampaignId = campaigns
                .stream()
                .filter(camp -> !camp.getId().equals(campaignId))
                .findFirst()
                .get()
                .getId();

        List<AdGroupGetItem> expectedGroups = cmdRule.apiSteps().adGroupsSteps().adGroupsGet(
                new ru.yandex.autotests.directapi.model.api5.adgroups.GetRequestMap()
                        .withSelectionCriteria(new AdGroupsSelectionCriteriaMap().withCampaignIds(campaignId))
                        .withFieldNames(AdGroupFieldEnum.values())
                , CLIENT).getAdGroups();
        List<AdGroupGetItem> actualGroups = cmdRule.apiSteps().adGroupsSteps().adGroupsGet(
                new ru.yandex.autotests.directapi.model.api5.adgroups.GetRequestMap()
                        .withSelectionCriteria(new AdGroupsSelectionCriteriaMap().withCampaignIds(copiedCampaignId))
                        .withFieldNames(AdGroupFieldEnum.values())
                , CLIENT).getAdGroups();
        assumeThat("группы получены", actualGroups, Matchers.not(Matchers.empty()));

        List<AdGetItem> expectedAds = cmdRule.apiSteps().adsSteps().adsGet(
                new ru.yandex.autotests.directapi.model.api5.ads.GetRequestMap()
                        .withSelectionCriteria(new AdsSelectionCriteriaMap().withCampaignIds(campaignId))
                        .withFieldNames(AdFieldEnum.values()),
                CLIENT
        ).getAds();
        List<AdGetItem> actualAds = cmdRule.apiSteps().adsSteps().adsGet(
                new ru.yandex.autotests.directapi.model.api5.ads.GetRequestMap()
                        .withSelectionCriteria(new AdsSelectionCriteriaMap().withCampaignIds(copiedCampaignId))
                        .withFieldNames(AdFieldEnum.values()),
                CLIENT
        ).getAds();
        assumeThat("объявления получены", actualAds, Matchers.not(Matchers.empty()));

        List<WebpageGetItem> expectedAdTargets = cmdRule.apiSteps().dynamicTextAdTargetsSteps().dynamicTextAdTargetsGet(
                new ru.yandex.autotests.directapi.model.api5.dynamictextadtargets.GetRequestMap()
                        .withSelectionCriteria(new WebpagesSelectionCriteriaMap().withCampaignIds(campaignId))
                        .withFieldNames(WebpageFieldEnum.values()),
                CLIENT
        ).getWebpages();
        List<WebpageGetItem> actualAdTargets = cmdRule.apiSteps().dynamicTextAdTargetsSteps().dynamicTextAdTargetsGet(
                new ru.yandex.autotests.directapi.model.api5.dynamictextadtargets.GetRequestMap()
                        .withSelectionCriteria(new WebpagesSelectionCriteriaMap().withCampaignIds(copiedCampaignId))
                        .withFieldNames(WebpageFieldEnum.values()),
                CLIENT
        ).getWebpages();
        assumeThat("AdTargets получены", actualAdTargets, Matchers.not(Matchers.empty()));


        assertThat("Количество кампаний совпадает с ожидаемым для клиента " + CLIENT,
                campaigns.size(),
                equalTo(CAMPAIGNS_AMOUNT * 2));
        assertThat("Параметры ДТО групп после копирования совпадают с исходными",
                actualGroups, beanDiffer(expectedGroups).useCompareStrategy(allFieldsExcept(
                        newPath(".*", "campaignId"),
                        newPath(".*", "id")
                )));
        assertThat("Параметры ДТО объявлений после копирования совпадают с исходными",
                actualAds, beanDiffer(expectedAds).useCompareStrategy(allFieldsExcept(
                        newPath(".*", "campaignId"),
                        newPath(".*", "adGroupId"),
                        newPath(".*", "id")
                )));
        assertThat("Параметры нацеливаний после копирования совпадают с исходными",
                actualAdTargets, beanDiffer(expectedAdTargets).useCompareStrategy(allFieldsExcept(
                        newPath(".*", "campaignId"),
                        newPath(".*", "adGroupId"),
                        newPath(".*", "id")
                )));
    }
}

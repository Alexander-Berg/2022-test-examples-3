package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import com.yandex.direct.api.v5.adgroups.AdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.AdGroupGetItem;
import com.yandex.direct.api.v5.ads.AdFieldEnum;
import com.yandex.direct.api.v5.ads.AdGetItem;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import com.yandex.direct.api.v5.keywords.KeywordFieldEnum;
import com.yandex.direct.api.v5.keywords.KeywordGetItem;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.CopyCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.cmd.util.CampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.api5.adgroups.AdGroupsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.ads.AdsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.keywords.GetRequestMap;
import ru.yandex.autotests.directapi.model.api5.keywords.KeywordsSelectionCriteriaMap;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;

@Aqua.Test
@Description("Проверка копирования ТГО кампании контроллером copyCamp")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
public class CopyCampTest {

    private static final String DEFAULT_COPY_REQUEST = "cmd.copyCamp.request.default";
    private static final String CLIENT = "at-direct-b-copycamp-3";
    private static final int CAMPAIGNS_AMOUNT = 1;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(new TestWatcher() {
        @Override
        protected void starting(org.junit.runner.Description description) {
            cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT);
        }
    }, bannersRule);

    private CopyCampRequest copyCampRequest;
    private Long campaignId;

    @After
    public void deleteCampaigns() {
        cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT);
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        LocalDate startDay = LocalDate.now().plusDays(1);
        cmdRule.apiSteps().getDirectJooqDbSteps().useShardForLogin(CLIENT)
                .campaignsSteps().setStartDate(campaignId, Date.valueOf(startDay));
        copyCampRequest = BeanLoadHelper.loadCmdBean(DEFAULT_COPY_REQUEST, CopyCampRequest.class);
        copyCampRequest.setCidFrom(String.valueOf(campaignId));
        copyCampRequest.setNewLogin(CLIENT);
        copyCampRequest.setOldLogin(CLIENT);
    }

    @Test
    @Description("Проверяем параметры ТГО кампании при копировании кампаний")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10817")
    public void campaignParametersTest() {
        cmdRule.cmdSteps().copyCampSteps().postCopyCamp(copyCampRequest);
        CampaignHelper.copyCampaignsByScript(cmdRule, CLIENT, Long.parseLong(copyCampRequest.getCidFrom()));
        List<CampaignGetItem> campaigns = cmdRule.apiSteps().campaignSteps().getCampaigns(CLIENT);

        assertThat("Количество кампаний совпадает с ожидаемым для клиента " + CLIENT,
                campaigns.size(), equalTo(CAMPAIGNS_AMOUNT * 2));
        assertThat("Параметры кампании после копирования совпадают с исходными",
                campaigns.get(0), beanDiffer(campaigns.get(1))
                        .useCompareStrategy(allFieldsExcept(
                                newPath("id"),
                                newPath("notification", "emailSettings", "email"),
                                newPath("clientInfo")
                        )));
    }

    @Test
    @Description("Проверяем параметры ТГО баннеров при копировании кампаний")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10818")
    public void bannerParametersTest() {
        cmdRule.cmdSteps().copyCampSteps().postCopyCamp(copyCampRequest);
        CampaignHelper.copyCampaignsByScript(cmdRule, CLIENT, Long.parseLong(copyCampRequest.getCidFrom()));

        List<CampaignGetItem> campaigns = cmdRule.apiSteps().campaignSteps().getCampaigns(CLIENT);
        List<CampaignGetItem> campaignsExceptCopied = campaigns
                .stream()
                .filter(camp -> !camp.getId().equals(campaignId))
                .sorted()
                .collect(toList());
        checkState(campaignsExceptCopied.size() == 1,
                "после копирования у клиента должна быть только одна кампания, "
                        + "кроме копируемой (т.е. созданная копированием)");

        Long newCampaignId = campaignsExceptCopied.get(0).getId();

        List<AdGroupGetItem> expectedGroups = cmdRule.apiSteps().adGroupsSteps().adGroupsGet(
                new ru.yandex.autotests.directapi.model.api5.adgroups.GetRequestMap()
                        .withSelectionCriteria(new AdGroupsSelectionCriteriaMap().withCampaignIds(campaignId))
                        .withFieldNames(AdGroupFieldEnum.values())
                , CLIENT).getAdGroups();
        List<AdGroupGetItem> actualGroups = cmdRule.apiSteps().adGroupsSteps().adGroupsGet(
                new ru.yandex.autotests.directapi.model.api5.adgroups.GetRequestMap()
                        .withSelectionCriteria(new AdGroupsSelectionCriteriaMap().withCampaignIds(newCampaignId))
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
                        .withSelectionCriteria(new AdsSelectionCriteriaMap().withCampaignIds(newCampaignId))
                        .withFieldNames(AdFieldEnum.values()),
                CLIENT
        ).getAds();
        assumeThat("объявления получены", actualAds, Matchers.not(Matchers.empty()));

        List<KeywordGetItem> expectedKeywords = cmdRule.apiSteps().keywordsSteps().keywordsGet(
                CLIENT,
                new GetRequestMap()
                        .withSelectionCriteria(new KeywordsSelectionCriteriaMap().withCampaignIds(campaignId))
                        .withFieldNames(KeywordFieldEnum.values())
        );
        List<KeywordGetItem> actualKeywords = cmdRule.apiSteps().keywordsSteps().keywordsGet(
                CLIENT,
                new GetRequestMap()
                        .withSelectionCriteria(new KeywordsSelectionCriteriaMap().withCampaignIds(newCampaignId))
                        .withFieldNames(KeywordFieldEnum.values())
        );
        assumeThat("keywords получены", actualKeywords, Matchers.not(Matchers.empty()));

        assertThat("Количество кампаний совпадает с ожидаемым для клиента " + CLIENT,
                campaigns.size(),
                equalTo(CAMPAIGNS_AMOUNT * 2));
        assertThat("Параметры ТГО групп после копирования совпадают с исходными",
                actualGroups, beanDiffer(expectedGroups).useCompareStrategy(allFieldsExcept(
                        newPath(".*", "campaignId"),
                        newPath(".*", "id")
                )));
        assertThat("Параметры ТГО объявлений после копирования совпадают с исходными",
                actualAds, beanDiffer(expectedAds).useCompareStrategy(allFieldsExcept(
                        newPath(".*", "campaignId"),
                        newPath(".*", "adGroupId"),
                        newPath(".*", "id")
                )));
        assertThat("Параметры ключевых фраз после копирования совпадают с исходными",
                actualKeywords, beanDiffer(expectedKeywords).useCompareStrategy(allFieldsExcept(
                        newPath(".*", "campaignId"),
                        newPath(".*", "adGroupId"),
                        newPath(".*", "id")
                )));

    }

}

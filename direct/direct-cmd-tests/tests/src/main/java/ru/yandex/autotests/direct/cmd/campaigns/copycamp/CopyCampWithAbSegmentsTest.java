package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper.runDeleteCampaignScriptAndIgnoreResult;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка копирования ТГО кампании с аб-сегментом контроллером copyCamp")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(CampTypeTag.TEXT)
@Tag(ObjectTag.CAMPAIGN)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class CopyCampWithAbSegmentsTest {
    private static final String CLIENT = "at-direct-absegment5";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private Long newCid;
    private CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "Проверка копирования аб-сегментов при копировании кампании. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
                {CampaignTypeEnum.DMO},
        });
    }

    public CopyCampWithAbSegmentsTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideCampTemplate(new SaveCampRequest()
                        .withMetrika_counters(MetrikaCountersData.DEFAULT_COUNTER.getCounterId().toString())
                        .withAbSectionsStat(singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId()))
                        .withAbSegmentsRetargeting(
                                singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSegmentId())))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @After
    public void after() {
        if (newCid != null) {
            if (campaignType == CampaignTypeEnum.DMO) {
                cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT, newCid);
                runDeleteCampaignScriptAndIgnoreResult(cmdRule, Long.parseLong(User.get(CLIENT).getClientID()), newCid);
            } else {
                cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCid);
            }
        }
    }

    @Test
    @Description("Проверка копирования аб-сегментов при копировании кампании")
    @TestCaseId("11052")
    public void copyCampAbSegmentsTest() {
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT, bannersRule.getCampaignId());

        EditCampResponse response = cmdRule.cmdSteps().campaignSteps()
                .getEditCamp(newCid, CLIENT);

        assertThat("Аб-сегменты были скопированы", response.getCampaign(),
                beanDiffer(getExpectedCampaign()).useCompareStrategy(onlyExpectedFields()));
    }

    private Campaign getExpectedCampaign() {
        return new Campaign()
                .withAbSectionsStatistic(singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId()))
                .withAbSegmentsRetargeting(singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSegmentId()));
    }
}

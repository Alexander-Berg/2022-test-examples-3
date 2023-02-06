package ru.yandex.autotests.direct.cmd.campaigns.absegments;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.beans.retargeting.Goal;
import ru.yandex.autotests.direct.db.beans.retargeting.RetargetingConditionRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.RetargetingConditionsRetargetingConditionsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.RetargetingConditionsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Добавление аб-сегментов в кампаниях")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.MCBANNER)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class SaveCampWithAbSegmentsExistingTest {

    private static final String CLIENT = "at-direct-absegment4";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private SaveCampRequest request;
    private Long expectedRetCondId;

    @Parameterized.Parameters(name = "Проверка сохранения аб-сегментов со страницы кампании с существующими ret_cond. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
                {CampaignTypeEnum.DMO},
                {CampaignTypeEnum.MCBANNER},
        });
    }

    public SaveCampWithAbSegmentsExistingTest(CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory
                .getBannersRuleBuilderByCampType(campaignType)
                .overrideCampTemplate(new SaveCampRequest()
                        .withMetrika_counters(MetrikaCountersData.DEFAULT_COUNTER.getCounterId().toString()))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
        TestEnvironment.newDbSteps(CLIENT).retargetingConditionSteps()
                .deleteRetargeingCondition(RetargetingConditionsRetargetingConditionsType.ab_segments, clientId);
        expectedRetCondId = TestEnvironment.newDbSteps(CLIENT).retargetingConditionSteps()
                .addRetargetingCondition(new RetargetingConditionsRecord().setClientid(clientId)
                                .setRetargetingConditionsType(RetargetingConditionsRetargetingConditionsType.ab_segments)
                                .setConditionJson(singletonList(new RetargetingConditionRule()
                                        .withType("or")
                                        .withGoals(
                                                singletonList(new Goal()
                                                        .withGoalId(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSegmentId())))
                                        .toString()).toString()),
                        clientId);

        request = bannersRule.getSaveCampRequest()
                .withMobileAppId(null)
                .withCid(String.valueOf(bannersRule.getCampaignId()))
                .withAbSectionsStat(singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId()))
                .withAbSegmentsRetargeting(singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSegmentId()))
                .withUlogin(CLIENT);
    }

    @Test
    @TestCaseId("11046")
    public void checkSaveAbSegmentsExisting() {
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);
        CampaignsRecord campaign =
                TestEnvironment.newDbSteps(CLIENT).campaignsSteps().getCampaignById(bannersRule.getCampaignId());
        Long abSegmentRetCondId = campaign.getAbSegmentRetCondId();
        assertThat("Показываемая группа соответсвует ожиданиям", abSegmentRetCondId, equalTo(expectedRetCondId));
    }
}

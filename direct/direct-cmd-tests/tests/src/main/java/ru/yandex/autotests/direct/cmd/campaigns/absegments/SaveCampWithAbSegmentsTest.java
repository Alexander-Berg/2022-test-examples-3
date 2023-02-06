package ru.yandex.autotests.direct.cmd.campaigns.absegments;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
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
public class SaveCampWithAbSegmentsTest {

    private static final String CLIENT = "at-direct-absegment";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private SaveCampRequest request;
    private Long segmentId;

    @Parameterized.Parameters(name = "Проверка сохранения аб-сегментов со страницы кампании. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
                {CampaignTypeEnum.DMO},
                {CampaignTypeEnum.MCBANNER},
        });
    }

    public SaveCampWithAbSegmentsTest(CampaignTypeEnum campaignType) {
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
        segmentId = MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSegmentId();
        request = bannersRule.getSaveCampRequest()
                .withMobileAppId(null)
                .withCid(String.valueOf(bannersRule.getCampaignId()))
                .withAbSectionsStat(singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId()))
                .withAbSegmentsRetargeting(singletonList(segmentId))
                .withUlogin(CLIENT);
    }

    @Test
    @TestCaseId("11047")
    public void checkSaveAbSegments() {
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);
        CampaignsRecord campaign =
                TestEnvironment.newDbSteps(CLIENT).campaignsSteps().getCampaignById(bannersRule.getCampaignId());

        List<Long> retConds = TestEnvironment.newDbSteps(CLIENT).retargetingConditionSteps()
                .getRetargetingConditionsByClient(Long.valueOf(User.get(CLIENT).getClientID()))
                .stream()
                .filter(t -> RetargetingConditionsRetargetingConditionsType.ab_segments.equals(t.getRetargetingConditionsType()))
                .map(RetargetingConditionsRecord::getRetCondId)
                .collect(Collectors.toList());
        Long expectedStatRetCondId = retConds.stream().min(Long::compareTo)
                .orElseThrow(() -> new DirectCmdStepsException("ret cond id not found"));
        Long expectedRetCondId = retConds.stream().max(Long::compareTo)
                .orElseThrow(() -> new DirectCmdStepsException("ret cond id not found"));
        assertThat("Аб сегменты статистики соответсвтуют ожиданию", campaign.getAbSegmentStatRetCondId(),
                equalTo(expectedStatRetCondId));
        assertThat("Аб сегменты ретаргетинга соответсвтуют ожиданию", campaign.getAbSegmentRetCondId(),
                equalTo(expectedRetCondId));
    }
}

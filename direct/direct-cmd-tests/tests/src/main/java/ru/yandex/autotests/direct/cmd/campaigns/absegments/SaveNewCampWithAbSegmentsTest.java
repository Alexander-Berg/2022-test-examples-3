package ru.yandex.autotests.direct.cmd.campaigns.absegments;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.RetargetingConditionsRetargetingConditionsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.RetargetingConditionsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение кампании с аб-сегментами")
@Stories(TestFeatures.Campaigns.SAVE_NEW_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class SaveNewCampWithAbSegmentsTest {

    private static final String CLIENT = "at-direct-absegment2";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private SaveCampRequest request;
    private Long campaignId;

    @Before
    public void before() {
        Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
        TestEnvironment.newDbSteps(CLIENT).retargetingConditionSteps()
                .deleteRetargeingCondition(RetargetingConditionsRetargetingConditionsType.ab_segments, clientId);
        request = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_NEW_TEXT_CAMP_DEFAULT, SaveCampRequest.class)
                .withMetrika_counters(MetrikaCountersData.DEFAULT_COUNTER.getCounterId().toString())
                .withAbSectionsStat(singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId()))
                .withAbSegmentsRetargeting(Collections.singletonList(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSegmentId()))
                .withUlogin(CLIENT);
    }

    @After
    public void after() {
        if (campaignId != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, campaignId);
        }
    }

    @Test
    @TestCaseId("11051")
    public void checkSaveAbSegments() {
        campaignId = cmdRule.cmdSteps().campaignSteps().saveNewCampaign(request);
        CampaignsRecord campaign =
                TestEnvironment.newDbSteps(CLIENT).campaignsSteps().getCampaignById(campaignId);
        Long abSegmentRetCondId = campaign.getAbSegmentRetCondId();

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

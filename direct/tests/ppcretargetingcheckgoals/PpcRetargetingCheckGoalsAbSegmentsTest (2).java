package ru.yandex.autotests.directintapi.tests.ppcretargetingcheckgoals;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsStatusshow;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.tags.StageTag;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionGoalItemMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionItemMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionMap;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingType;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Tag(StageTag.RELEASE)
@Features(FeatureNames.PPC_RETARGETING_CHECK_GOALS)
@Issue("https://st.yandex-team.ru/DIRECT-78793")
@Description("Вызов скрипта ppcRetargetingCheckGoals.pm для аб-сегментов")
public class PpcRetargetingCheckGoalsAbSegmentsTest {
    private static final String CLIENT = "at-direct-absegm-stopcamp";
    private static final Long SECTION_ID = MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId();
    private static final Long NONACCESSIBLE_SEGMENT_ID = 2500000099L;

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule1;

    @Rule
    public DirectCmdRule cmdRule;

    private Long retargetingConditionId;
    private int shard;

    public PpcRetargetingCheckGoalsAbSegmentsTest() {
        TestEnvironment.newDbSteps(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
        bannersRule1 = new TextBannersRule()
                .overrideCampTemplate(new SaveCampRequest()
                        .withMetrika_counters(MetrikaCountersData.DEFAULT_COUNTER.getCounterId().toString())
                        .withAbSectionsStat(singletonList(SECTION_ID)))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule1);
    }

    @Before
    public void before() {
        cmdRule.darkSideSteps().getCampaignFakeSteps().makeCampaignActive(bannersRule1.getCampaignId());
        shard = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).getCurrentPpcShard();

        int[] retargetingConditionIds = api.userSteps.retargetingSteps().addRetargetingConditions(
                new RetargetingConditionMap(api.type())
                        .defaultRetargeting(CLIENT)
                        .withRetargetingConditionItems(
                                new RetargetingConditionItemMap(api.type())
                                        .withType(RetargetingType.OR)
                                        .withGoals(new RetargetingConditionGoalItemMap(api.type())
                                                .withTime(1)
                                                .withGoalID(NONACCESSIBLE_SEGMENT_ID))));
        assumeThat("создалось одно условие ретаргетинга", retargetingConditionIds.length, Matchers.equalTo(1));
        retargetingConditionId = (long) retargetingConditionIds[0];

        CampaignsRecord record = TestEnvironment.newDbSteps().useShard(shard).campaignsSteps()
                .getCampaignById(bannersRule1.getCampaignId());
        record.setAbSegmentRetCondId(retargetingConditionId);
        TestEnvironment.newDbSteps().useShard(shard).campaignsSteps().updateCampaigns(record);

        CampaignsRecord firstCamp = TestEnvironment.newDbSteps().useShard(shard)
                .campaignsSteps().getCampaignById(bannersRule1.getCampaignId());
        assumeThat("Кампания показывается", firstCamp.getStatusshow(),
                equalTo(CampaignsStatusshow.Yes));
    }

    @Test
    public void runScriptAndCheckResult() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps()
                .setIsAccessible(retargetingConditionId, NONACCESSIBLE_SEGMENT_ID, 1);
        cmdRule.apiSteps().getDarkSideSteps().getRunScriptSteps()
                .runPpcRetargetingCheckGoals(shard, User.get(CLIENT).getClientID());

        CampaignsRecord firstCamp = TestEnvironment.newDbSteps().useShard(shard)
                .campaignsSteps().getCampaignById(bannersRule1.getCampaignId());
        assertThat("Кампания остановлена", firstCamp.intoMap(),
                beanDiffer(getExpectedCamp()).useCompareStrategy(onlyExpectedFields()));
    }

    private Map<String, Object> getExpectedCamp() {
        return new CampaignsRecord()
                .setStatusshow(CampaignsStatusshow.No)
                .setStatusbssynced(CampaignsStatusbssynced.No)
                .intoMap();
    }
}

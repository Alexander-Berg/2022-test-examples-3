package ru.yandex.autotests.direct.cmd.groups.performance;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.group.GoalType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetConditionItemType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingGoal;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collections;
import java.util.stream.Collectors;

import static ru.yandex.autotests.direct.cmd.data.retargeting.RetargetingConditionsFactory.AUDIENCES;

@Aqua.Test
@Description("Проверка создания ДМО группы с сегментами аудиторий")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.RETAGRETING)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SavePerfomanceAdgroupsAudienceTest extends SavePerformanceAdgroupsTestBase {
    private static final String AUDIENCE_RET_COND_TEMPLATE =
            "cmd.common.request.retargetingCondition.AjaxSaveRetargetingCondAudienceTest";

    @Before
    @Override
    public void before() {
        super.before();
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
    }

    @Test
    @Description("сохранение группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9837")
    public void addPerformanceGroupWithAudienceSegment() {
        Long retCondId = cmdRule.cmdSteps().retargetingSteps()
                .saveRetargetingConditionWithAssumption(getEditRetargetingCondition(), CLIENT);
        PerformanceFilter filter = BeanLoadHelper.
                loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class);
        filter.withRetargeting(
                new RetargetingCondition()
                        .withRetCondId(retCondId)
        );
        expectedGroup.setPerformanceFilters(Collections.singletonList(filter));
        saveGroup();
        adgroupId = TestEnvironment.newDbSteps().bannersSteps().getBannersByCid(campaignId).get(0).getPid().toString();
        bids = StringUtils.join(TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .getBannersByCid(campaignId).stream().filter(t -> t.getPid().equals(Long.valueOf(adgroupId)))
                .map(BannersRecord::getBid).collect(Collectors.toList()), ',');
        check();
    }

    public RetargetingCondition getEditRetargetingCondition() {
        RetargetingCondition retCondition = BeanLoadHelper.loadCmdBean(AUDIENCE_RET_COND_TEMPLATE, RetargetingCondition.class).
                withConditionName("new name").
                withConditionDesc("new desc");
        retCondition.getCondition().forEach(c -> c.setType(RetConditionItemType.ALL.getValue()));
        retCondition.getCondition().get(0).getGoals().add(
                new RetargetingGoal().withGoalId(AUDIENCES[2]).withGoalType(GoalType.AUDIENCE));
        return retCondition;
    }

}

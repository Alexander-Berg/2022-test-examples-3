package ru.yandex.autotests.direct.cmd.bssynced.dynamiccondition;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.dynamicconditions.AjaxEditDynamicConditionsRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced группы при действиях над условием нацеливания ДТО")
@Stories(TestFeatures.Conditions.AJAX_EDIT_DYNAMIC_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag(ObjectTag.DYN_COND)
@Tag(CampTypeTag.DYNAMIC)
public class DynamicCondActionsBsSyncedTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final DynamicCondition firstCondition = BeanLoadHelper
            .loadCmdBean(CmdBeans.COMMON_REQUEST_DYNAMIC_COND_DEFAULT, DynamicCondition.class);
    private static final DynamicCondition secondCondition = BeanLoadHelper
            .loadCmdBean(CmdBeans.COMMON_REQUEST_DYNAMIC_COND_FULL, DynamicCondition.class);


    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private DynamicBannersRule bannersRule = new DynamicBannersRule()
            .overrideGroupTemplate(new Group()
                    .withDynamicConditions(new ArrayList<>(Arrays.asList(firstCondition, secondCondition))))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        prepareGroupStatuses();
        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
        BsSyncedHelper.setBannerBsSynced(cmdRule, bannersRule.getBannerId(), StatusBsSynced.YES);
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при удалении условия ДТО через сохранение")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9324")
    public void checkBsSyncedGroupDeleteDynamicCondTest() {
        Group group = getGroupWithIds();
        group.getDynamicConditions().remove(1);
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));

        check();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при удалении условия ДТО со страницы кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9325")
    public void checkBsSyncedDeleteDynamicCondTest() {
        String dynId = bannersRule.getCurrentGroup().getDynamicConditions().get(0).getDynId();
        cmdRule.cmdSteps().ajaxEditDynamicConditionsSteps()
                .dynamicConditionsDeleteWithAssumption(campaignId, bannersRule.getGroupId(), CLIENT, Long.valueOf(dynId));

        check();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при остановке условия ДТО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9326")
    public void checkBsSyncedSuspendDynamicCondTest() {
        suspendCondition("1");

        check();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при включении условия ДТО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9327")
    public void checkBsSyncedEnableDynamicCondTest() {
        suspendCondition("1");
        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
        BsSyncedHelper.setBannerBsSynced(cmdRule, bannersRule.getBannerId(), StatusBsSynced.YES);
        suspendCondition("0");

        check();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при добавлении условия ДТО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9328")
    public void checkBsSyncedAddDynamicCondTest() {
        Group group = getGroupWithIds();
        group.getDynamicConditions().add(BeanLoadHelper
                .loadCmdBean(CmdBeans.COMMON_REQUEST_DYNAMIC_COND_DEFAULT, DynamicCondition.class)
                .withDynamicConditionName("third cond"));
        group.getDynamicConditions().get(2).getConditions().get(0)
                .withValue(Collections.singletonList("Different Condition_3"));

        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));
        check();
    }

    private void prepareGroupStatuses() {
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerActive(bannersRule.getBannerId());
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        cmdRule.apiSteps().campaignFakeSteps().setStatusModerate(bannersRule.getCampaignId(), "Yes");
    }

    private void check() {
        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("статусы bsSynced соответствуют ожиданию", actualGroup,
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    private Group getExpectedGroup() {
        return new Group()
                .withStatusBsSynced(StatusBsSynced.NO.toString())
                .withBanners(Collections.singletonList(new Banner()
                        .withStatusBsSynced(StatusBsSynced.NO.toString())));
    }

    private Group getGroupWithIds() {
        Group group = bannersRule.getGroup();
        group.setAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.getBanners().get(0).setBid(bannersRule.getBannerId());
        return group;
    }

    private void suspendCondition(String suspendFlag) {
        DynamicCondition condition = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, String.valueOf(campaignId))
                .getGroups().get(0).getDynamicConditions().get(0);
        condition.setIsSuspended(suspendFlag);

        CommonResponse response = cmdRule.cmdSteps().ajaxEditDynamicConditionsSteps()
                .postAjaxEditDynamicConditions(AjaxEditDynamicConditionsRequest.fromDynamicCondition(condition)
                        .withCid(String.valueOf(campaignId))
                        .withUlogin(CLIENT));
        assumeThat("динамическое условие было остановлено", response.getResult(), equalTo("ok"));
    }
}

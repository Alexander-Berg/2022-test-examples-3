package ru.yandex.autotests.direct.cmd.bssynced.filters;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.AjaxEditPerformanceFiltersResponse;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilterBannersMap;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
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

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced группы при действиях над условием нацеливания ДМО")
@Stories(TestFeatures.Conditions.AJAX_EDIT_PERFORMANCE_FILTERS)
@Features(TestFeatures.CONDITIONS)
@Tag(ObjectTag.PERFORMANCE_FILTER)
@Tag(CampTypeTag.PERFORMANCE)
public class PerfFiltersActionsBsSyncedTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT2;
    private static final PerformanceFilter firstFilter = BeanLoadHelper
            .loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class);
    private static final PerformanceFilter secondFilter = BeanLoadHelper
            .loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_FULL, PerformanceFilter.class);


    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private PerformanceBannersRule bannersRule = new PerformanceBannersRule()
            .overrideGroupTemplate(new Group()
                    .withPerformanceFilters(new ArrayList<>(Arrays.asList(firstFilter, secondFilter))))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        BsSyncedHelper.moderateCamp(cmdRule, campaignId);

        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
        BsSyncedHelper.setBannerBsSynced(cmdRule, bannersRule.getBannerId(), StatusBsSynced.YES);
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при удалении условия ДМО через сохранение")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9335")
    public void checkBsSyncedGroupDeletePerfFilterTest() {
        Group group = getGroupWithIds();
        group.getPerformanceFilters().remove(1);
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));

        check();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при удалении условия ДМО со страницы кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9336")
    public void checkBsSyncedDeletePerfFilterTest() {
        String filterId = bannersRule.getCurrentGroup().getPerformanceFilters().get(0).getPerfFilterId();
        cmdRule.cmdSteps().ajaxEditPerformanceFiltersSteps()
                .performanceFiltersDeleteWithAssumption(campaignId, bannersRule.getGroupId(), CLIENT, filterId);

        check();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при остановке условия ДМО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9337")
    public void checkBsSyncedSuspendPerfFilterTest() {
        suspendCondition("1");

        check();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при включении условия ДМО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9338")
    public void checkBsSyncedEnablePerfFilterTest() {
        suspendCondition("1");
        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
        BsSyncedHelper.setBannerBsSynced(cmdRule, bannersRule.getBannerId(), StatusBsSynced.YES);
        suspendCondition("0");

        check();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при добавлении условия ДМО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9339")
    public void checkBsSyncedAddPerfFilterTest() {
        Group group = getGroupWithIds();
        group.getPerformanceFilters().add(BeanLoadHelper
                .loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_FULL, PerformanceFilter.class)
                .withFilterName("third filter"));
        group.getPerformanceFilters().get(2).getConditions().get(0)
                .withValue(Collections.singletonList("Different Condition_3"));

        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));
        check();
    }

    private void check() {
        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("статусы bsSynced соответствуют ожиданию", actualGroup,
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    private Group getExpectedGroup() {
        return new Group()
                .withStatus_bs_synced(StatusBsSynced.NO.toString())
                .withBanners(Collections.singletonList(new Banner()
                        .withStatus_bs_synced(StatusBsSynced.NO.toString())));
    }

    private Group getGroupWithIds() {
        Group group = bannersRule.getGroup();
        group.setAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.getBanners().get(0).setBid(bannersRule.getBannerId());
        return group;
    }

    private void suspendCondition(String suspendFlag) {
        PerformanceFilter filter = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, String.valueOf(campaignId))
                .getGroups().get(0).getPerformanceFilters().get(0);
        filter.withIsSuspended(suspendFlag);

        AjaxEditPerformanceFiltersResponse response = cmdRule.cmdSteps().ajaxEditPerformanceFiltersSteps()
                .postAjaxEditPerformanceFilters(campaignId.toString(), CLIENT,
                        PerformanceFilterBannersMap
                                .forPerformanceFilter(bannersRule.getGroupId().toString(), filter.getPerfFilterId(), filter));
        assumeThat("действия над фильтром прошли успешно", response.getErrors(), nullValue());
        assumeThat("действия над фильтром прошли успешно", response.getError(), nullValue());
    }
}

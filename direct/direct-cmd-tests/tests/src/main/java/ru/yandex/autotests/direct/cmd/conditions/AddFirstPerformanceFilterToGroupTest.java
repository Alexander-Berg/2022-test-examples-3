package ru.yandex.autotests.direct.cmd.conditions;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

@Aqua.Test
@Description("Проверка статусов при добавлении первого условия (фраза)")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@Tag("TESTIRT-8612")
public class AddFirstPerformanceFilterToGroupTest extends AddFirstConditionToGroupTestBase {

    private PerformanceFilter expectedFilter;

    @Override
    public BannersRule getBannersRule() {
        return new PerformanceBannersRule().overrideGroupTemplate(new Group().withPerformanceFilters(emptyList()));
    }

    @Before
    public void before() {
        assumeThat("у группы нет условий", bannersRule.getCurrentGroup(),
                beanDiffer(new Group().withPerformanceFilters(emptyList()))
                        .useCompareStrategy(onlyFields(newPath("performanceFilters"))));
        expectedFilter = BeanLoadHelper
                .loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_FULL, PerformanceFilter.class)
                .withIsSuspended(isSuspended);
        expectedGroup = bannersRule.getCurrentGroup();
        expectedGroup.withPerformanceFilters(singletonList(expectedFilter));
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(expectedGroup, CampaignTypeEnum.DTO);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10860")
    public void addFistFilterToDraftGroupTest() {
        addFistConditionToDraftGroupTest();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10861")
    public void addFistFilterToActiveGroupTest() {
        addFistConditionToActiveGroupTest();
    }

    @Override
    protected Group getExpectedGroupDraft() {
        return new Group()
                .withStatus_moderate(StatusModerate.NEW.toString())
                .withStatus_bs_synced(StatusBsSynced.NO.toString())
                .withBanners(singletonList(new Banner()
                        .withStatus_moderate(StatusModerate.NEW.toString())
                        .withStatus_bs_synced(StatusBsSynced.NO.toString()))
                )
                .withPerformanceFilters(singletonList(getExpectedFilter()));
    }

    @Override
    protected Group getExpectedGroupActive() {
        return new Group()
                .withStatus_moderate(StatusModerate.YES.toString())
                .withStatus_bs_synced(StatusBsSynced.NO.toString())
                .withBanners(singletonList(new Banner()
                        .withStatus_moderate(StatusModerate.YES.toString())
                        .withStatus_bs_synced(StatusBsSynced.NO.toString()))
                )
                .withPerformanceFilters(singletonList(getExpectedFilter()));
    }

    private PerformanceFilter getExpectedFilter() {
        return expectedFilter
                .withPerfFilterId(null)
                .withStatusBsSynced(StatusBsSynced.NO.toString());
    }
}

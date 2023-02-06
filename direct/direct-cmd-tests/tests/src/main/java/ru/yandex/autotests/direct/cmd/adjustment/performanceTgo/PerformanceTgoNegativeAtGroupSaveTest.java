package ru.yandex.autotests.direct.cmd.adjustment.performanceTgo;

import java.util.Arrays;
import java.util.Collection;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.PerformanceTgoMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.groups.PerformanceTgoMultiplierPctGroupErrors;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка ошибок при настройке корректировок Смарт-ТГО (параметр performance_tgo_multiplier) " +
        "контроллера savePerformanceAdGroups")
@Stories(TestFeatures.Groups.ADJUSTMENT_PERFORMANCE_TGO)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.PERFORMANCE)
@RunWith(Parameterized.class)
public class PerformanceTgoNegativeAtGroupSaveTest {
    private final static String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private BannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
    }

    @Parameterized.Parameter(value = 0)
    public PerformanceTgoMultiplierPctGroupErrors performanceTgoMultiplierPctGroupErrors;
    @Parameterized.Parameter(value = 1)
    public String description;

    @Parameterized.Parameters(name = "Значение параметра multiplier_pct {0} ({1})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {PerformanceTgoMultiplierPctGroupErrors.TOO_SHORT, "Сохранение значения меньше допустимого"},
                {PerformanceTgoMultiplierPctGroupErrors.TOO_LONG, "Сохранение значения больше допустимого"},
                {PerformanceTgoMultiplierPctGroupErrors.NOT_AN_INT, "Сохранение нецелого значения"},
                {PerformanceTgoMultiplierPctGroupErrors.NOT_AN_INT_TOO_SHORT,
                        "Сохранение нецелого значения меньше допустимого"},
                {PerformanceTgoMultiplierPctGroupErrors.NOT_AN_INT_TOO_LONG,
                        "Сохранение нецелого значения больше допустимого"},
        });
    }

    @Test
    @Description("Проверяем валидацию при сохранении некорректных корректировок ставок контроллером savePerformanceAdGroups")
    @TestCaseId("11029")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        HierarchicalMultipliers hierarchicalMultipliers = new HierarchicalMultipliers().
                withPerformanceTgoMultiplier(new PerformanceTgoMultiplier().
                        withMultiplierPct(performanceTgoMultiplierPctGroupErrors.getValue()));

        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString()).
                withHierarchicalMultipliers(hierarchicalMultipliers);
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        GroupsParameters groupsParameters = GroupsParameters.forExistingCamp(CLIENT, campaignId, group);
        GroupErrorsResponse errorResponse = cmdRule.cmdSteps().groupsSteps().
                postSavePerformanceAdGroupsErrorResponse(groupsParameters);

        assertThat("корректировки ставок не сохранились",
                errorResponse.getErrors().getGroupErrors().getArrayErrors().get(0).getObjectErrors()
                        .getHierarchicalMultipliers().get(0).getDescription(),
                containsString(performanceTgoMultiplierPctGroupErrors.getErrorText()));
    }
}

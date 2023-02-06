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
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.PerformanceTgoMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка настройки корректировок для Смарт-ТГО для группы объявлений (параметр performance_tgo_multiplier)")
@Stories(TestFeatures.Groups.ADJUSTMENT_PERFORMANCE_TGO)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class GroupPerformanceTgoMultiplierTest {
    private final static String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();
    private BannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    protected Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
    }

    @Parameterized.Parameter(value = 0)
    public String multiplierPct;

    @Parameterized.Parameters(name = "Значение параметра multiplier_pct = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"151"},
                {"100"},
                {"50"}
        });
    }

    private HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers().
                withPerformanceTgoMultiplier(new PerformanceTgoMultiplier().
                        withMultiplierPct(multiplierPct));
    }

    private HierarchicalMultipliers getExpectedHierarchicalMultipliers() {
        return getHierarchicalMultipliers();
    }

    @Test
    @Description("Проверяем сохранение корректировок ставок контроллером saveTextAdGroups")
    @TestCaseId("11026")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        GroupsParameters groupsParameters = GroupsParameters.
                forExistingCamp(CLIENT, campaignId, getGroup());
        cmdRule.cmdSteps().groupsSteps().postSavePerformanceAdGroups(groupsParameters);

        ShowCampMultiEditRequest showCampMultiEditRequest = ShowCampMultiEditRequest.
                forSingleBanner(CLIENT, campaignId, bannersRule.getGroupId(), bannersRule.getBannerId());
        ShowCampMultiEditResponse actualResponse = cmdRule.cmdSteps().campaignSteps().
                getShowCampMultiEdit(showCampMultiEditRequest);

        check(actualResponse);
    }

    private void check(ShowCampMultiEditResponse actualResponse) {
        assertThat("корректировки ставок сохранились", actualResponse.getCampaign()
                        .getGroups().get(0).getHierarchicalMultipliers(),
                beanDiffer(getExpectedHierarchicalMultipliers()));
    }

    private Group getGroup() {
        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString()).
                withHierarchicalMultipliers(getHierarchicalMultipliers());
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        return group;
    }
}

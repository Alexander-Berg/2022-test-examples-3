package ru.yandex.autotests.direct.cmd.adjustment.retargeting;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.RetargetingMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceRequest;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка настройки цен на мобильных устройствах для группы объявлений ДМО кампаний" +
        " (корректировка ставок demography_multiplier)")
@Stories(TestFeatures.Groups.ADJUSTMENT_DEMOGRAPHY)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class GroupRetargetingMultiplierPerformanceCampTest {

    private final static String CLIENT = "at-direct-adjustment-ret21";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter
    public String multiplierPct;
    private BannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Long campaignId;
    private String retargetingId;

    @Parameterized.Parameters(name = "Значение параметра multiplier_pct = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"151"},
                {"100"},
                {"0"}
        });
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
        cmdRule.apiSteps().retargetingSteps().addConditionsForUser(CLIENT, 1);
        retargetingId = String.valueOf(cmdRule.apiSteps().
                retargetingSteps().getRetargetingConditions(CLIENT)[0]);
    }

    @Test
    @Description("Проверка ответа контроллера savePerformanceAdGroups")
    @ru.yandex.qatools.allure.annotations.TestCaseId("8981")
    public void savePerformanceAdGroupsTest() {
        cmdRule.cmdSteps().groupsSteps().
                postSavePerformanceAdGroups(GroupsParameters.forExistingCamp(CLIENT, campaignId, getGroup()));
        EditAdGroupsPerformanceResponse actualResponse = cmdRule.cmdSteps().groupsSteps()
                .getEditAdGroupsPerformance(new EditAdGroupsPerformanceRequest()
                        .withCid(String.valueOf(campaignId))
                        .withAdGroupIds(bannersRule.getGroupId().toString())
                        .withUlogin(CLIENT));

        check(actualResponse);
    }

    private void check(EditAdGroupsPerformanceResponse actualResponse) {
        assumeThat("группа сохранилась", actualResponse.getCampaign().getPerformanceGroups(), hasSize(1));

        assertThat("ДМО баннер в ответе контроллера соответствует отправленному в запросе",
                actualResponse.getCampaign().getPerformanceGroups().get(0).getHierarchicalMultipliers(),
                beanDiffer(getHierarchicalMultipliers()));
    }

    private HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers()
                .withRetargetingMultiplier(
                        RetargetingMultiplier.getDefaultRetargetingMultiplier(retargetingId, multiplierPct));
    }

    private Group getGroup() {
        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString()).
                withHierarchicalMultipliers(getHierarchicalMultipliers());
        group.getBanners().get(0).setBid(bannersRule.getBannerId());
        return group;
    }
}

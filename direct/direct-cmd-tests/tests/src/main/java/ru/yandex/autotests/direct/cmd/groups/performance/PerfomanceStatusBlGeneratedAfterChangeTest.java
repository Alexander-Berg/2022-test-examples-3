package ru.yandex.autotests.direct.cmd.groups.performance;
//Task: https://st.yandex-team.ru/TESTIRT-9323.

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AdgroupsPerformanceStatusblgenerated;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AdgroupsPerformanceRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBlGenerated;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Изменение статуса группы StatusBlGenerated при изменении группы")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class PerfomanceStatusBlGeneratedAfterChangeTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(0)
    public StatusBlGenerated savedStatus;
    @Parameterized.Parameter(1)
    public StatusBlGenerated expectedStatusAfterFilterChange;
    @Parameterized.Parameter(2)
    public StatusBlGenerated expectedStatusAfterGroupNameChange;
    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Parameterized.Parameters(name =
            "Статус, который сохраняется: {0}, статус который ожидается после изменения фильтра {1}, " +
                    "статус который ожидается после изменения имени {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {StatusBlGenerated.NO, StatusBlGenerated.PROCESS, StatusBlGenerated.NO},
                {StatusBlGenerated.YES, StatusBlGenerated.PROCESS, StatusBlGenerated.YES},
                {StatusBlGenerated.PROCESS, StatusBlGenerated.PROCESS, StatusBlGenerated.PROCESS},
        });
    }

    @Before
    public void before() {

        AdgroupsPerformanceRecord adgroupPerfomance =
                TestEnvironment.newDbSteps(CLIENT).adGroupsSteps().getAdgroupsPerformance(bannersRule.getGroupId());
        cmdRule.cmdSteps().bannerSteps()
                .sendModerate(CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId(), bannersRule.getBannerId());
        adgroupPerfomance.setStatusblgenerated(AdgroupsPerformanceStatusblgenerated.valueOf(savedStatus.toString()));
        TestEnvironment.newDbSteps(CLIENT).adGroupsSteps().updateAdgroupsPerformance(adgroupPerfomance);
    }


    @Test
    @Description("Изменение фильтра")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9834")
    public void changeFilter() {
        Group savingGroup = bannersRule.getGroup().withAdGroupID(String.valueOf(bannersRule.getGroupId()));
        savingGroup.getBanners().get(0).withBannerID(bannersRule.getBannerId());
        PerformanceFilter filter = savingGroup.getPerformanceFilters().get(0);
        filter.getConditions().get(0).withValue(Collections.singleton("test"));
        saveGroup(savingGroup);
        AdgroupsPerformanceRecord actualStatus = TestEnvironment.newDbSteps(CLIENT)
                .adGroupsSteps().getAdgroupsPerformance(bannersRule.getGroupId());
        assertThat("Статус генeрции дто соответствует ожиданиям",
                actualStatus.getStatusblgenerated().getLiteral(),
                equalTo(expectedStatusAfterFilterChange.toString()));
    }

    @Test
    @Description("Изменение имени группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9835")
    public void changeGroupName() {
        Group savingGroup = bannersRule.getGroup().withAdGroupID(String.valueOf(bannersRule.getGroupId()));
        savingGroup.getBanners().get(0).withBannerID(bannersRule.getBannerId());
        savingGroup.setAdGroupName("newName");
        saveGroup(savingGroup);
        AdgroupsPerformanceRecord actualStatus = TestEnvironment.newDbSteps(CLIENT)
                .adGroupsSteps().getAdgroupsPerformance(bannersRule.getGroupId());
        assertThat("Статус генeрции дто соответствует ожиданиям",
                actualStatus.getStatusblgenerated().getLiteral(),
                equalTo(expectedStatusAfterGroupNameChange.toString()));
    }

    private void saveGroup(Group group) {
        group.withCampaignID(String.valueOf(bannersRule.getCampaignId()))
                .withFeedId(String.valueOf(bannersRule.getFeedId()));
        group.getBanners().get(0)
                .getCreativeBanner().setCreativeId(bannersRule.getCreativeId());
        group.getBanners().forEach(b -> b.setCid(bannersRule.getCampaignId()));
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        String filterId =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, String.valueOf(bannersRule.getCampaignId()))
                        .getGroups().get(0).getPerformanceFilters().get(0).getPerfFilterId();
        group.getPerformanceFilters().get(0).setPerfFilterId(filterId);

        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        bannersRule.getDirectCmdSteps().groupsSteps().postSavePerformanceAdGroups(groupRequest);
    }
}

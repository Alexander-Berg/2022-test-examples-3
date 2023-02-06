package ru.yandex.autotests.direct.cmd.groups.performance;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Изменение гео креатива при удалении группы-черновика")
@Stories(TestFeatures.Banners.DELETE_BANNER)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.DEL_BANNER)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag("TESTIRT-10237")
public class DraftCreativeGeoDeleteGroupTest {

    public static final String CLIENT = "at-direct-perf-groups";
    public static final Geo GEO_ONE = Geo.RUSSIA;
    public static final Geo GEO_TWO = Geo.KAZAKHSTAN;

    private PerformanceBannersRule bannersRule = new PerformanceBannersRule()
            .overrideGroupTemplate(new Group().withGeo(GEO_ONE.getGeo()))
            .withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
    }

    @Test
    @Description("Гео креатива обновляется при удалении группы, если креатив в статусе new")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9827")
    public void creativeGeoDeleteGroup() {
        addSecondGroup();
        deleteGroup(bannersRule.getGroupId().toString(), bannersRule.getBannerId().toString());
        assumeThat("группа удалилась", getGroupsId(), not(hasItem(bannersRule.getGroupId().toString())));

        PerfCreativesRecord creativesRecord = getCreative();
        assertThat("Гео креатива изменился", creativesRecord.getSumGeo(),
                equalTo(GEO_TWO.getGeo()));
    }

    @Test
    @Description("Гео креатива обновляется при удалении последней группы, если креатив в статусе new")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9826")
    public void creativeGeoDeleteLastGroup() {
        deleteGroup(bannersRule.getGroupId().toString(), bannersRule.getBannerId().toString());
        assumeThat("последняя группа удалилась", cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannersRule.getCampaignId().toString()).getGroups(), hasSize(0));

        PerfCreativesRecord creativesRecord = getCreative();
        assertThat("Гео креатива изменился", creativesRecord.getSumGeo(), nullValue());
    }

    private void deleteGroup(String groupId, String bannerId) {
        cmdRule.cmdSteps().bannerSteps()
                .deleteBanner(bannersRule.getCampaignId().toString(), groupId, bannerId, CLIENT);
    }

    private List<String> getGroupsId() {
        return cmdRule.cmdSteps().groupsSteps()
                .getGroups(CLIENT, bannersRule.getCampaignId())
                .stream().map(Group::getAdGroupID)
                .collect(Collectors.toList());
    }

    private void addSecondGroup() {
        Group secondGroup = bannersRule.getGroup()
                .withGeo(GEO_TWO.getGeo());
        secondGroup.getBanners().get(0).getCreativeBanner()
                .setCreativeId(bannersRule.getCreativeId());
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), secondGroup));
        assumeThat("в кампании две группы", cmdRule.cmdSteps().groupsSteps()
                .getGroups(CLIENT, bannersRule.getCampaignId()), hasSize(2));
    }

    private PerfCreativesRecord getCreative() {
        return TestEnvironment.newDbSteps().perfCreativesSteps()
                .getPerfCreatives(bannersRule.getCreativeId());
    }
}

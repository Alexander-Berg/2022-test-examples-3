package ru.yandex.autotests.direct.cmd.groups.performance;

import java.util.Collections;

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
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Изменение гео креатива при изменении гео в группе-черновике")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag("TESTIRT-10237")
public class ChangeGeoInGroupForDraftCreativeTest {

    private static final String CLIENT = "at-direct-perf-groups";
    private static final Geo GEO_ONE = Geo.RUSSIA;
    private static final Geo GEO_TWO = Geo.UKRAINE;

    private PerformanceBannersRule bannersRule = new PerformanceBannersRule()
            .overrideGroupTemplate(new Group().withGeo(GEO_ONE.getGeo() + "," + GEO_TWO.getGeo() + "," + Geo.TURKEY))
            .withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
    }

    @Test
    @Description("Гео креатива изменяется на гео из группы, если креатив в статусе new")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9820")
    public void creativeGeoShouldBeSetAccordingGroup() {
        updateGroupWithGeo(GEO_ONE);
        PerfCreativesRecord creativesRecord = getCreative();
        assertThat("Гео креатива изменился", creativesRecord.getSumGeo(), equalTo(GEO_ONE.getGeo()));
    }

    @Test
    @Description("Гео креатива изменяется при добавлении группы, если креатив в статусе new")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9821")
    public void creativeGeoAddSecondGroup() {
        updateGroupWithGeo(GEO_ONE);
        addSecondGroup();

        PerfCreativesRecord creativesRecord = getCreative();
        assertThat("Гео креатива изменился", creativesRecord.getSumGeo(),
                equalTo(GEO_TWO.getGeo() + "," + GEO_ONE.getGeo()));
    }

    @Test
    @Description("Гео креатива изменяется при изменении одной из групп, если креатив в статусе new")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9822")
    public void creativeGeoUpdateSecondGroup() {
        updateGroupWithGeo(GEO_ONE);
        addSecondGroup();
        updateGroupWithGeo(GEO_TWO);

        PerfCreativesRecord creativesRecord = getCreative();
        assertThat("Гео креатива изменился", creativesRecord.getSumGeo(),
                equalTo(GEO_TWO.getGeo()));
    }

    @Test
    @Description("Гео креатива не изменяется на гео из группы, если креатив в статусе yes")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9823")
    public void cannotUpdateCreativeGeoAfterModeration() {
        PerfCreativesRecord creativesRecord = getCreative();
        creativesRecord.setStatusmoderate(PerfCreativesStatusmoderate.Yes);
        TestEnvironment.newDbSteps().perfCreativesSteps().updatePerfCreatives(creativesRecord);
        updateGroupWithGeo(GEO_ONE);
        creativesRecord = getCreative();
        assertThat("Гео креатива не изменился", creativesRecord.getSumGeo(), not(equalTo(GEO_ONE.getGeo())));
    }

    private void updateGroupWithGeo(Geo geo) {
        Group group = bannersRule.getCurrentGroup();
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, bannersRule.getMediaType());
        group.setGeo(geo.getGeo());

        cmdRule.cmdSteps().groupsSteps().postSavePerformanceAdGroups(CLIENT,
                bannersRule.getCampaignId().toString(), Collections.singletonList(group));
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

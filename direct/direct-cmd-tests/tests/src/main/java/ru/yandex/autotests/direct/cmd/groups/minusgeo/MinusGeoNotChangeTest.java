package ru.yandex.autotests.direct.cmd.groups.minusgeo;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersMinusGeoType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersMinusGeoRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Features(TestFeatures.Groups.MINUS_GEO)
@Stories(TestFeatures.GROUPS)
@Description("Проверка изменения минус гео")
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class MinusGeoNotChangeTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "Проверка изменения минус гео. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
        });
    }

    public MinusGeoNotChangeTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideGroupTemplate(new Group().withGeo(Geo.RUSSIA.getGeo() + "," + Geo.UKRAINE.getGeo()))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(bannersRule.getCampaignId());
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiSteps().phrasesFakeSteps().setPhraseStatusModerateYes(bannersRule.getGroupId());

        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.current, Geo.UKRAINE.getGeo());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.bs_synced, Geo.AUSTRIA.getGeo());
    }

    @Test
    @Description("Проверка получения гео в ответе showCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10851")
    public void minusGeoNotChangedTest() {
        Group saveGroup = bannersRule.getCurrentGroup();
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(saveGroup, campaignType);
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), saveGroup));

        BannersMinusGeoRecord actualRecord = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannersSteps().getBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.current);
        assumeThat("запись минус гео осталась", actualRecord, notNullValue());
        assertThat("Минус гео не изменилось", actualRecord.getMinusGeo(), equalTo(Geo.UKRAINE.getGeo()));
    }
}

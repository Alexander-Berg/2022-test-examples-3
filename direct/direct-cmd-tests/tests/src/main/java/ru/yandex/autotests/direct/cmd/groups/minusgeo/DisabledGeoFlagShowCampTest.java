package ru.yandex.autotests.direct.cmd.groups.minusgeo;


import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersMinusGeoType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesStatuspostmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Features(TestFeatures.Groups.MINUS_GEO)
@Stories(TestFeatures.GROUPS)
@Description("Проверка возврата флага camp_has_disabled_geo в ответе showCamp")
@Tag(CmdTag.SHOW_CAMP)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class DisabledGeoFlagShowCampTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    @Parameterized.Parameters(name = "флаг camp_has_disabled_geo для не архивных кампаний. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
        });
    }

    public DisabledGeoFlagShowCampTest(CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideGroupTemplate(new Group().withGeo(Geo.RUSSIA.getGeo() + "," + Geo.UKRAINE.getGeo()))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Test
    @Description("Проверка получения флага camp_has_disabled_geo в ответе showCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10847")
    public void disabledGeoShowCampTest() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.current, Geo.UKRAINE.getGeo());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps().setPhrasesStatusModerate(bannersRule.getGroupId(),
                PhrasesStatusmoderate.Yes, PhrasesStatuspostmoderate.Yes);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps().setBannerStatusModerate(bannersRule.getBannerId(),
                BannersStatusmoderate.Yes, BannersStatuspostmoderate.Yes);

        check("1");
    }

    @Test
    @Description("Проверка получения флага camp_has_disabled_geo в ответе showCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10848")
    public void disabledGeoZeroShowCampTest() {
        check("0");
    }

    private void check(String expectedDisabledGeo) {
        ShowCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannersRule.getCampaignId().toString());

        assertThat("camp_has_disabled_geo соответсвует ожиданиям", actualResponse.getCampHasDisabledGeo(),
                equalTo(expectedDisabledGeo));
    }
}

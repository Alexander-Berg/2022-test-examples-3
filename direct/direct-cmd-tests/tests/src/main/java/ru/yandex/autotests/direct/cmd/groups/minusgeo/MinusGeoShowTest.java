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
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersMinusGeoType;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Features(TestFeatures.Groups.MINUS_GEO)
@Stories(TestFeatures.GROUPS)
@Description("Проверка возврата гео")
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class MinusGeoShowTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    @Parameterized.Parameters(name = "Эффективный гео. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
        });
    }

    public MinusGeoShowTest(CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideGroupTemplate(new Group().withGeo(Geo.RUSSIA.getGeo() + "," + Geo.UKRAINE.getGeo()))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.current, Geo.UKRAINE.getGeo());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.bs_synced, Geo.AUSTRIA.getGeo());
    }

    @Test
    @Description("Проверка получения гео в ответе showCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10854")
    public void minusGeoShowCampTest() {
        Banner actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannersRule.getCampaignId().toString()).getGroups().get(0);

        assertThat("гео соответсвует ожиданиям", actualGroup,
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Проверка получения гео в ответе showCampMultiEdit")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10855")
    public void minusGeoShowCampMultiEditTest() {
        Group actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCampMultiEdit(CLIENT, bannersRule.getCampaignId()).getCampaign().getGroups().get(0);

        assertThat("гео соответсвует ожиданиям", actualGroup,
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    private Banner getExpectedBanner() {
        return new Banner()
                .withEffectiveGeo(Geo.RUSSIA.getGeo() + "," + Geo.CRIMEA.getGeo())
                .withMinusGeo(Geo.UKRAINE.getGeo())
                .withDisabledGeo(Geo.UKRAINE.getGeo());
    }

    private Group getExpectedGroup() {
        return new Group()
                .withEffectiveGeo(Geo.RUSSIA.getGeo() + "," + Geo.CRIMEA.getGeo())
                .withDisabledGeo(Geo.UKRAINE.getGeo())
                .withBanners(singletonList(new Banner().withMinusGeo(Geo.UKRAINE.getGeo())));
    }
}

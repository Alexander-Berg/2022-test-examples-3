package ru.yandex.autotests.direct.cmd.groups.minusgeo;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampRequest;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Features(TestFeatures.Groups.MINUS_GEO)
@Stories(TestFeatures.GROUPS)
@Description("Проверка поиска групп с минус-гео в showCamp")
@Tag(CmdTag.SHOW_CAMP)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class DisabledGeoOnlyShowCampTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "Проверка поиска групп с минус-гео. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
        });
    }

    public DisabledGeoOnlyShowCampTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideGroupTemplate(new Group().withGeo(Geo.RUSSIA.getGeo() + "," + Geo.UKRAINE.getGeo()))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.current, Geo.UKRAINE.getGeo());

        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(),
                bannersRule.getGroup()));
        assumeThat("в кампании две группы", cmdRule.cmdSteps().groupsSteps()
                .getGroups(CLIENT, bannersRule.getCampaignId()), hasSize(2));
    }

    @Test
    @Description("Проверка поиска группы с геолицензированием (запрос в showCamp)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10849")
    public void disabledGeoShowCampTest() {
        List<Banner> actualGroups = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(new ShowCampRequest()
                        .withCid(bannersRule.getCampaignId().toString())
                        .withTab("all")
                        .withDisabledGeoOnly(1)
                        .withUlogin(CLIENT)
                ).getGroups();

        assumeThat("Найдена одна группа", actualGroups, hasSize(1));
        assertThat("Нашлась группа с минус гео", actualGroups.get(0).getAdGroupId(),
                equalTo(bannersRule.getGroupId()));
    }
}

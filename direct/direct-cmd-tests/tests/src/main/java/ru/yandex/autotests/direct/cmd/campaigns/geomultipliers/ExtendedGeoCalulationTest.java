package ru.yandex.autotests.direct.cmd.campaigns.geomultipliers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo.ExtendedGeoItem;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo.NegativeExtendedGeoItem;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo.Partly;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Вычисление гео кампании по гео групп" )
@Stories(TestFeatures.Campaigns.EDIT_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.EDIT_CAMP)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class ExtendedGeoCalulationTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String ULOGIN = "at-direct-geo-changes-1";
    @Parameterized.Parameter(0)
    public String geo1;
    @Parameterized.Parameter(1)
    public String geo2;
    @Parameterized.Parameter(2)
    public ExtendedGeoHelper.ExtendedGeoMapBuilder expectedGeoItems;

    private Long adGroupId1;
    private Long adGroupId2;
    private Map<String, ExtendedGeoItem> actualGeoItems;
    private CampaignRule campaignRule = new CampaignRule().withMediaType(CampaignTypeEnum.TEXT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule).as(ULOGIN);

    @Parameterized.Parameters(name = "Данные: geo1 = {0}, geo2 = {1}, expected geos: {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{

                {Geo.RUSSIA.getGeo()+ "," + Geo.UKRAINE.getGeo(), Geo.RUSSIA.getGeo()+ "," + Geo.UKRAINE.getGeo(), ExtendedGeoHelper.ExtendedGeoMapBuilder.builder()
                        .add("225", new ExtendedGeoItem().withAll("1"))
                        .add("187", new ExtendedGeoItem().withAll("1"))
                },
                {Geo.RUSSIA.getGeo(), Geo.UKRAINE.getGeo(),
                        ExtendedGeoHelper.ExtendedGeoMapBuilder.builder()
                                .add(Geo.RUSSIA.getGeo(), new ExtendedGeoItem()
                                        .withPartly(new Partly()
                                                .withGroupIds("1")))
                                .add(Geo.UKRAINE.getGeo(), new ExtendedGeoItem()
                                .withPartly(new Partly()
                                        .withGroupIds("2")))
                },
                {Geo.RUSSIA.getGeo(), "-" + Geo.RUSSIA.getGeo(),
                        ExtendedGeoHelper.ExtendedGeoMapBuilder.builder()
                                .add("0", new ExtendedGeoItem()
                                        .withPartly(new Partly()
                                                .withGroupIds("2")))
                                .add(Geo.RUSSIA.getGeo(), new ExtendedGeoItem()
                                .withNegative(new NegativeExtendedGeoItem()
                                        .withPartly(new Partly()
                                                .withGroupIds("2")))
                                .withPartly(new Partly()
                                        .withGroupIds("1")))
                },
                {Geo.RUSSIA.getGeo(), Geo.RUSSIA.getGeo() + ",-" + Geo.SIBERIA.getGeo(),
                        ExtendedGeoHelper.ExtendedGeoMapBuilder.builder()
                                .add(Geo.RUSSIA.getGeo(), new ExtendedGeoItem().withAll("1"))
                                .add(Geo.SIBERIA.getGeo(), new ExtendedGeoItem()
                                .withNegative(new NegativeExtendedGeoItem()
                                        .withPartly(new Partly()
                                                .withGroupIds("2"))))
                }

        });
    }

    @Before
    public void setup() {
        cmdRule.cmdSteps().groupsSteps().addNewTextAdGroup(ULOGIN, campaignRule.getCampaignId(),
                GroupsFactory.getDefaultTextGroup().withGeo(geo1));
        cmdRule.cmdSteps().groupsSteps().addNewTextAdGroup(ULOGIN, campaignRule.getCampaignId(),
                GroupsFactory.getDefaultTextGroup().withGeo(geo2));
        List<Group> groups = cmdRule.cmdSteps().groupsSteps().getGroups(ULOGIN, campaignRule.getCampaignId());
        assumeThat("Создано 2 группы", groups, Matchers.hasSize(2));

        adGroupId1 = Long.valueOf(groups.get(0).getAdGroupID());
        adGroupId2 = Long.valueOf(groups.get(1).getAdGroupID());

        actualGeoItems = campaignRule.getCurrentCampaign().getExtendedGeoItemsMap();
    }

    @Test
    @Description("Ожидаем, что extended_geo корректно сформирован")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10889")
    public void campaignExtendedGeoTest() {
        assertThat("Получили ожидаемый extended_geo", actualGeoItems,
                beanDiffer(expectedGeoItems.build(adGroupId1, adGroupId2)));
    }
}

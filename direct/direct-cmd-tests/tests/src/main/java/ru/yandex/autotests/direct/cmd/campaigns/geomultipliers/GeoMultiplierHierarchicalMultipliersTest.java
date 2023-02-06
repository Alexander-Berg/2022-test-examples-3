package ru.yandex.autotests.direct.cmd.campaigns.geomultipliers;

import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.GeoMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.savecamp.GeoCharacteristic;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка в контроллера saveCamp: сохранение геокорректировок в hierarchical_multipliers")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SAVE_CAMP)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class GeoMultiplierHierarchicalMultipliersTest {
    private static final String RUSSIA_MULTIPLIER = "800";
    private static final String AUSTRIA_MULTIPLIER = "600";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String ULOGIN = "at-direct-geo-changes-2";

    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideGroupTemplate(new Group().withGeo(Geo.SIBERIA.getGeo()))
            .overrideCampTemplate(new SaveCampRequest().withGeo(Geo.RUSSIA.getGeo() + "," + Geo.AUSTRIA.getGeo()));

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(ULOGIN);

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10893")
    public void checkHierarchicalMultipliers() {

        SaveCampRequest request = bannersRule.getSaveCampRequest();

        Map<String, GeoCharacteristic> geoChanges = new HashMap<>();
        geoChanges.put(Geo.RUSSIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));
        geoChanges.put(Geo.AUSTRIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));

        request.withGeoChanges(geoChanges).withCid(bannersRule.getCampaignId().toString());
        Map<String, String> geoMultipliers = new HashMap<>();
        geoMultipliers.put(Geo.RUSSIA.getGeo(), RUSSIA_MULTIPLIER);
        geoMultipliers.put(Geo.AUSTRIA.getGeo(), AUSTRIA_MULTIPLIER);

        request.withGeoMultipliers(geoMultipliers);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        GeoMultiplier actualGeoMultiplier =
                bannersRule.getCurrentCampaign().getHierarchicalMultipliers().getGeoMultiplier();

        GeoMultiplier expectedGeoMultiplier = new GeoMultiplier()
                .withIsEnabled(0)
                .withRegions(geoMultipliers);
        assertThat("гео-корректировки соотвествует ожиданиям", actualGeoMultiplier,
                beanDiffer(expectedGeoMultiplier).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10894")
    public void checkEnablingHierarchicalMultipliersWithoutMultipliers() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();
        request.withGeoMultipliersEnabled(1);
        request.withCid(bannersRule.getCampaignId().toString());

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        GeoMultiplier actualGeoMultiplier =
                bannersRule.getCurrentCampaign().getHierarchicalMultipliers().getGeoMultiplier();

        assertThat("гео-корректировки соотвествует ожиданиям", actualGeoMultiplier.getIsEnabled(),
                nullValue());
    }

    @Test
    @Description("корректировки во вложенных локациях")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10895")
    public void checkHierarchicalMultipliersWithEnclosedGeo() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();

        Map<String, GeoCharacteristic> geoChanges = new HashMap<>();
        geoChanges.put(Geo.RUSSIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));
        geoChanges.put(Geo.AUSTRIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));

        request.withGeoChanges(geoChanges)
                .withCid(bannersRule.getCampaignId().toString())
                .withGeoMultipliersEnabled(1);
        Map<String, String> geoMultipliers = new HashMap<>();
        geoMultipliers.put(Geo.RUSSIA.getGeo(), RUSSIA_MULTIPLIER);
        geoMultipliers.put(Geo.SIBERIA.getGeo(), "1200");

        request.withGeoMultipliers(geoMultipliers);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        GeoMultiplier actualGeoMultiplier =
                bannersRule.getCurrentCampaign().getHierarchicalMultipliers().getGeoMultiplier();

        GeoMultiplier expectedGeoMultiplier = new GeoMultiplier()
                .withRegions(geoMultipliers);

        assertThat("гео-корректировки соотвествует ожиданиям", actualGeoMultiplier,
                beanDiffer(expectedGeoMultiplier).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}

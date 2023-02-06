package ru.yandex.autotests.direct.cmd.campaigns.geomultipliers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
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

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка в контроллера saveCamp: сохранение геокорректировок в hierarchical_multipliers с разным типом merge")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SAVE_CAMP)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class GeoMultiplierMergeTest {
    private static final String AUSTRIA_MULTIPLIER = "600";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String ULOGIN = "at-direct-geo-changes-2";

    private TextBannersRule bannersRule1;
    private TextBannersRule bannersRule2;

    @Rule
    public DirectCmdRule cmdRule;

    public GeoMultiplierMergeTest() {
        bannersRule1 = new TextBannersRule()
                .overrideGroupTemplate(new Group().withGeo(Geo.GERMANY.getGeo()))
                .overrideCampTemplate(new SaveCampRequest().withGeo(Geo.GERMANY + "," + Geo.AUSTRIA + "," + Geo.RUSSIA));
        bannersRule2 = new TextBannersRule()
                .forExistingCampaign(bannersRule1)
                .overrideGroupTemplate(new Group().withGeo(Geo.RUSSIA.getGeo()));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule1, bannersRule2).as(ULOGIN);
    }

    @Test
    @Description("сбрасываем гео-корректировку используя флаг merge_geo = 0")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10896")
    public void resetGeo() {
        SaveCampRequest request = bannersRule1.getSaveCampRequest();

        Map<String, Object> geoChanges = new HashMap<>();
        geoChanges.put(Geo.AUSTRIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));
        geoChanges.put("merge_geo", 0);

        request.withGeoChanges(geoChanges)
                .withCid(bannersRule1.getCampaignId().toString())
                .withGeoMultipliersEnabled(1);
        Map<String, String> geoMultipliers = new HashMap<>();
        geoMultipliers.put(Geo.AUSTRIA.getGeo(), AUSTRIA_MULTIPLIER);

        request.withGeoMultipliers(geoMultipliers);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);


        List<String> actualGeos = new ArrayList<>(bannersRule1.getCurrentCampaign().getExtendedGeoItemsMap().keySet());

        List<String> expectedGeos =
                Arrays.asList(Geo.AUSTRIA.getGeo());

        assertThat("гео-корректировки соотвествует ожиданиям", actualGeos,
                beanDiffer(expectedGeos).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("не сбрасываем гео-корректировку используя флаг geo_merge = 1")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10897")
    public void mergeGeo() {
        SaveCampRequest request = bannersRule1.getSaveCampRequest();

        Map<String, Object> geoChanges = new HashMap<>();
        geoChanges.put(Geo.AUSTRIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));
        geoChanges.put("merge_geo", 1);

        request.withGeoChanges(geoChanges)
                .withCid(bannersRule1.getCampaignId().toString())
                .withGeoMultipliersEnabled(1);
        Map<String, String> geoMultipliers = new HashMap<>();
        geoMultipliers.put(Geo.AUSTRIA.getGeo(), AUSTRIA_MULTIPLIER);

        request.withGeoMultipliers(geoMultipliers);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        List<String> actualGeos = new ArrayList<>(bannersRule1.getCurrentCampaign().getExtendedGeoItemsMap().keySet());
        Collections.sort(actualGeos);

        List<String> expectedGeos =
                Arrays.asList(Geo.AUSTRIA.getGeo(), Geo.RUSSIA.getGeo(), Geo.GERMANY.getGeo());
        Collections.sort(expectedGeos);

        assertThat("гео соответствует ожиданиям", actualGeos,
                beanDiffer(expectedGeos).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }
}

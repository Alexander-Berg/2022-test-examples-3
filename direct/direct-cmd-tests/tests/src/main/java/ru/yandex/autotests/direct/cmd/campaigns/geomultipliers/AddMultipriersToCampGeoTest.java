package ru.yandex.autotests.direct.cmd.campaigns.geomultipliers;

import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo.ExtendedGeoItem;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.savecamp.GeoCharacteristic;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка контроллера saveCamp: сохранение геокорректировок" )
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SAVE_CAMP)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class AddMultipriersToCampGeoTest {
    static final String RUSSIA_MULTIPLIER = "800";
    static final String NEW_RUSSIA_MULTIPLIER = "400";
    static final String AUSTRALIA_MULTIPLIER = "600";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String ULOGIN = "at-direct-geo-changes-2";

    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideGroupTemplate(new Group().withGeo(Geo.SIBERIA.getGeo()))
            .overrideCampTemplate(new SaveCampRequest().withGeo(null));

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(ULOGIN);

    @Test
    @Description("Установка корректировок на регионы сохраняется")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10887")
    public void setMultipliersForRegions() {
        SaveCampRequest request = setMultipliers();
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        Map<String, ExtendedGeoItem> actualExtendedGeoItemMap =
                bannersRule.getCurrentCampaign().getExtendedGeoItemsMap();

        Map<String, ExtendedGeoItem> expectedExtendedGeoItemMap = new HashMap<>();
        expectedExtendedGeoItemMap.put(Geo.RUSSIA.getGeo(),
                new ExtendedGeoItem().withMultiplierPct(RUSSIA_MULTIPLIER).withAll("1"));
        expectedExtendedGeoItemMap
                .put(Geo.AUSTRIA.getGeo(), new ExtendedGeoItem().withMultiplierPct(AUSTRALIA_MULTIPLIER).withAll("1"));

        assertThat("Получены данные по регионам",
                actualExtendedGeoItemMap,
                beanDiffer(expectedExtendedGeoItemMap).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Можно поменять значение процента корректировки на регион")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10888")
    public void changeMultipliersForRegions() {
        SaveCampRequest request = setMultipliers();
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);
        //change percentage
        Map<String, String> geoMultipliers = new HashMap<>();
        geoMultipliers.put(Geo.RUSSIA.getGeo(), NEW_RUSSIA_MULTIPLIER);
        request.withGeoMultipliers(geoMultipliers).withGeoChanges(new HashMap<>());
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        Map<String, ExtendedGeoItem> actualExtendedGeoItemMap =
                bannersRule.getCurrentCampaign().getExtendedGeoItemsMap();

        Map<String, ExtendedGeoItem> expectedExtendedGeoItemMap = new HashMap<>();
        expectedExtendedGeoItemMap.put(Geo.RUSSIA.getGeo(),
                new ExtendedGeoItem().withMultiplierPct(NEW_RUSSIA_MULTIPLIER).withAll("1"));
        expectedExtendedGeoItemMap
                .put(Geo.AUSTRIA.getGeo(), new ExtendedGeoItem().withAll("1"));

        assertThat("Получены данные по регионам",
                actualExtendedGeoItemMap,
                beanDiffer(expectedExtendedGeoItemMap).useCompareStrategy(onlyExpectedFields()));
    }

    private SaveCampRequest setMultipliers() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();
        Map<String, GeoCharacteristic> geoChanges = new HashMap<>();
        geoChanges.put(Geo.RUSSIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));
        geoChanges.put(Geo.AUSTRIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));

        request.withGeoChanges(geoChanges).withCid(bannersRule.getCampaignId().toString());
        Map<String, String> geoMultipliers = new HashMap<>();
        geoMultipliers.put(Geo.RUSSIA.getGeo(), RUSSIA_MULTIPLIER);
        geoMultipliers.put(Geo.AUSTRIA.getGeo(), AUSTRALIA_MULTIPLIER);
        request.withGeoMultipliers(geoMultipliers);

        return request;
    }
}

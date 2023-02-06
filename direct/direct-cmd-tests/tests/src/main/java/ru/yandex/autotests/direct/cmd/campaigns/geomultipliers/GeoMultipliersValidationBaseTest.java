package ru.yandex.autotests.direct.cmd.campaigns.geomultipliers;

import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.savecamp.GeoCharacteristic;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;

public abstract class GeoMultipliersValidationBaseTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static final String ULOGIN = "at-direct-geo-changes-2";

    protected TextBannersRule bannersRule = new TextBannersRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(ULOGIN);

    protected String percent;

    protected Map<String, String> geoMultipliers;
    protected Map<String, GeoCharacteristic> geoChanges;


    public void saveCampAndCheck() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();

        geoChanges = new HashMap<>();
        geoChanges.put(Geo.RUSSIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));

        request.withGeoChanges(geoChanges)
                .withCid(bannersRule.getCampaignId().toString())
                .withGeoMultipliersEnabled(1);
        geoMultipliers = new HashMap<>();
        geoMultipliers.put(Geo.RUSSIA.getGeo(), percent);

        request.withGeoMultipliers(geoMultipliers);

        saveCamp(request);
        checkCamp();
    }

    protected abstract void saveCamp(SaveCampRequest request);

    protected abstract void checkCamp();
}

package ru.yandex.autotests.direct.cmd.banners.forex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.savecamp.GeoCharacteristic;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.model.ModerationFlag;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestStepsEn.assumeThat;

public abstract class ForexDynamicBannerNegativeBaseTest {

    private static final String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public BannersRule bannersRule = getBannerRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    protected SaveCampRequest saveCampRequest;

    protected abstract BannersRule getBannerRule();

    @Before
    public void setup() {
    }

    public void changeTargeting(String... geo) {
        saveCampRequest = bannersRule.getSaveCampRequest().withMobileAppId(null);
        saveCampRequest.setCid(String.valueOf(bannersRule.getCampaignId()));
        Map<String, GeoCharacteristic> geoChanges = new HashMap<>();
        Arrays.stream(geo).forEach(x -> geoChanges.put(x, new GeoCharacteristic().withIsNegative("0")));
        saveCampRequest.withGeoChanges(geoChanges);
    }

    public void moderateAll() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignActive(bannersRule.getCampaignId().intValue());
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerActive(bannersRule.getBannerId());
    }

    public void setForexFlag() {
        cmdRule.apiSteps().bannersFakeSteps().setBannerFlags(bannersRule.getBannerId(), ModerationFlag.FOREX);
        BannersRecord bannersRecord = TestEnvironment.newDbSteps(CLIENT).bannersSteps().getBanner(bannersRule.getBannerId());
        assumeThat("Баннер имеет флаг forex", bannersRecord.getFlags().toString(), equalTo("forex"));
    }


    @Description("Меняем таргетинг с европы на европу")
    public void changeCampaignTargetingEuropeToEurope() {
        changeTargeting(Geo.GERMANY.getGeo());
        moderateAll();
        setForexFlag();

        changeTargeting(Geo.UKRAINE.getGeo());
        BannersRecord bannersRecord = TestEnvironment.newDbSteps(CLIENT).bannersSteps().getBanner(bannersRule.getBannerId());
        assertThat("Баннер промодерирован", bannersRecord.getStatusmoderate().toString(), equalTo("Yes"));
    }

    @Description("Меняем таргетинг с региона России на всю Россию")
    public void changeCampaignTargetingRegionOfRussiaToRussia() {
        changeTargeting(Geo.SIBERIA.getGeo());
        moderateAll();
        setForexFlag();

        changeTargeting(Geo.RUSSIA.getGeo(), Geo.CRIMEA.getGeo());
        BannersRecord bannersRecord = TestEnvironment.newDbSteps(CLIENT).bannersSteps().getBanner(bannersRule.getBannerId());
        assertThat("Баннер промодерирован", bannersRecord.getStatusmoderate().toString(), equalTo("Yes"));
    }

    @Description("Меняем таргетинг с Крыма на всю Россию")
    public void changeCampaignTargetingCrimeaToRussia() {
        changeTargeting(Geo.CRIMEA.getGeo());
        moderateAll();
        setForexFlag();

        changeTargeting(Geo.RUSSIA.getGeo(), Geo.CRIMEA.getGeo());
        BannersRecord bannersRecord = TestEnvironment.newDbSteps(CLIENT).bannersSteps().getBanner(bannersRule.getBannerId());
        assertThat("Баннер промодерирован", bannersRecord.getStatusmoderate().toString(), equalTo("Yes"));
    }

    @Description("Меняем таргетинг с европы на Россию без флага forex")
    public void campTargetingToRussiaWithoutForexFlag() {
        changeTargeting(Geo.GERMANY.getGeo());
        moderateAll();

        changeTargeting(Geo.RUSSIA.getGeo(), Geo.CRIMEA.getGeo());
        BannersRecord bannersRecord = TestEnvironment.newDbSteps(CLIENT).bannersSteps().getBanner(bannersRule.getBannerId());
        assertThat("Баннер промодерирован", bannersRecord.getStatusmoderate().toString(), equalTo("Yes"));
    }

    @Description("Меняем таргетинг с европы на регион России без флага forex")
    public void campTargetingToRegionOfRussiaWithoutForexFlag() {
        changeTargeting(Geo.GERMANY.getGeo());
        moderateAll();

        changeTargeting(Geo.SIBERIA.getGeo());
        BannersRecord bannersRecord = TestEnvironment.newDbSteps(CLIENT).bannersSteps().getBanner(bannersRule.getBannerId());
        assertThat("Баннер промодерирован", bannersRecord.getStatusmoderate().toString(), equalTo("Yes"));
    }
}

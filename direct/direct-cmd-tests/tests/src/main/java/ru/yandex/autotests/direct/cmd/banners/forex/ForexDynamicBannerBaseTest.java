package ru.yandex.autotests.direct.cmd.banners.forex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.savecamp.GeoCharacteristic;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.model.ModerationFlag;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestStepsEn.assumeThat;

public abstract class ForexDynamicBannerBaseTest {

    private static final String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private final HashMap<String, Object> geoChanges;
    public BannersRule bannersRule = getBannerRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule;
    protected SaveCampRequest saveCampRequest;

    public ForexDynamicBannerBaseTest() {
        geoChanges = new HashMap<>();
        geoChanges.put(Geo.GERMANY.getGeo(), new GeoCharacteristic().withIsNegative("0"));
        this.bannersRule = getBannerRule().overrideGroupTemplate(new Group().withGeo(Geo.GERMANY.getGeo()))
                .overrideCampTemplate(new SaveCampRequest().withGeo(Geo.GERMANY.getGeo()))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    protected abstract BannersRule getBannerRule();

    @Before
    public void setup() {
        BsSyncedHelper.moderateCamp(cmdRule, bannersRule.getCampaignId());
        setForexFlag();
    }

    public void changeTargeting(String... geo) {
        saveCampRequest = bannersRule.getSaveCampRequest().withMobileAppId(null);
        saveCampRequest.setCid(String.valueOf(bannersRule.getCampaignId()));
        Map<String, Object> geoChanges = new HashMap<>();
        Arrays.stream(geo).forEach(x -> geoChanges.put(x, new GeoCharacteristic().withIsNegative("0")));
        geoChanges.put("merge_geo", 0);
        saveCampRequest.withGeoChanges(geoChanges);
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
    }

    public void setForexFlag() {
        cmdRule.apiSteps().bannersFakeSteps().setBannerFlags(bannersRule.getBannerId(), ModerationFlag.FOREX);
        BannersRecord banners = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .getBanner(bannersRule.getBannerId());
        assumeThat("Баннер имеет флаг forex", banners.getFlags(), equalTo("forex"));
    }

    @Description("Меняем таргетинг кампании на Россию")
    public void changeCampaignTargetingToRussia() {
        changeTargeting(Geo.RUSSIA.getGeo(), Geo.CRIMEA.getGeo());
        check();
    }


    @Description("Таргетинг кампании начинает включать в себя Россию")
    public void includeRussiaToCampaignTargeting() {
        List<String> geos = new ArrayList(geoChanges.keySet());
        geos.add(Geo.RUSSIA.getGeo());
        geos.add(Geo.CRIMEA.getGeo());
        changeTargeting(geos.toArray(new String[]{}));
        check();
    }

    @Description("Таргетинг кампании начинает включать в себя Россию и другой регион")
    public void includeRussiaAndUkraineToCampaignTargeting() {
        List<String> geos = new ArrayList(geoChanges.keySet());
        geos.add(Geo.RUSSIA.getGeo());
        geos.add(Geo.CRIMEA.getGeo());
        geos.add(Geo.UKRAINE.getGeo());
        changeTargeting(geos.toArray(new String[]{}));
        check();
    }

    @Description("Меняем таргетинг кампании на регион России")
    public void changeCampaignTargetingToRegionOfRussia() {
        changeTargeting(Geo.SIBERIA.getGeo());
        check();
    }

    @Description("Меняем таргетинг кампании на Крым")
    public void changeCampaignTargetingToCrimea() {
        changeTargeting(Geo.CRIMEA.getGeo());
        check();
    }

    @Description("Таргетинг кампании начинает включать в себя один регион России")
    public void includeRegionOfRussiaToCampaignTargeting() {
        List<String> geos = new ArrayList(geoChanges.keySet());
        geos.add(Geo.SIBERIA.getGeo());
        changeTargeting(geos.toArray(new String[]{}));
        check();
    }

    @Description("Таргетинг кампании начинает включать в себя Крым")
    public void includeCrimeaToCampaignTargeting() {
        List<String> geos = new ArrayList(geoChanges.keySet());
        geos.add(Geo.CRIMEA.getGeo());
        changeTargeting(geos.toArray(new String[]{}));
        check();
    }

    private void check() {
        BannersRecord banner = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .getBanner(bannersRule.getBannerId());
        assertThat("Баннер отправлен на модерацию", banner.getStatusmoderate(),
                anyOf(equalTo(BannersStatusmoderate.Ready), equalTo(BannersStatusmoderate.Sending)));
    }
}

package ru.yandex.autotests.direct.cmd.banners.imagecreative.geo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

public abstract class CampGeoInCreativeImageBannerTestBase {

    protected static final String CLIENT = "at-direct-creative-construct3";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected CreativeBannerRule bannersRule;
    private CampaignTypeEnum campaignType;
    private Geo geo;
    private String bannerText;
    private Long creativeId;

    public CampGeoInCreativeImageBannerTestBase(CampaignTypeEnum campaignType, String bannerText, Geo geo) {
        this.campaignType = campaignType;
        this.geo = geo;
        this.bannerText = bannerText;
        bannersRule = new CreativeBannerRule(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        creativeId = bannersRule.getCreativeId();
        PerfCreativesRecord perfCreativesRecord = TestEnvironment.newDbSteps().perfCreativesSteps()
                .getPerfCreatives(creativeId);
        perfCreativesRecord.setModerateInfo(getModerateInfoString());
        TestEnvironment.newDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreativesRecord);
    }

    @After
    public void after() {
        if (creativeId != null) {
            PerformanceCampaignHelper.deleteCreativeQuietly(CLIENT, creativeId);
        }
    }

    protected SaveCampRequest getSaveCampRequest() {
        return bannersRule.getSaveCampRequest()
                .withMobileAppId(null)
                .withCid(bannersRule.getCampaignId().toString())
                .withGeo(geo.getGeo());
    }

    private String getModerateInfoString() {
        JsonObject root = new JsonObject();
        JsonArray texts = new JsonArray();
        JsonObject text = new JsonObject();
        text.addProperty("text", bannerText);
        texts.add(text);
        root.add("texts", texts);
        return new Gson().toJson(root);
    }
}

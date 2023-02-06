package ru.yandex.autotests.direct.cmd.banners.imagecreative.geo;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.CmdBeansMaps;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.common.CreativeBanner;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;

import static java.util.Collections.singletonList;

public abstract class GroupGeoInCreativeImageBannerTestBase {

    protected static final String CLIENT = "at-direct-creative-construct3";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected CampaignRule campaignRule;
    private CampaignTypeEnum campaignType;
    private Geo geo;
    private String bannerText;
    private Long creativeId;

    public GroupGeoInCreativeImageBannerTestBase(CampaignTypeEnum campaignType, String bannerText, Geo geo) {
        this.campaignType = campaignType;
        this.geo = geo;
        this.bannerText = bannerText;
        campaignRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);
    }

    @Before
    public void before() {
        creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.valueOf(User.get(CLIENT).getClientID()));
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

    protected Group getGroup() {
        Group group = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_GROUP_TEMPLATE.get(campaignType), Group.class);
        group.withBanners(new ArrayList<>(singletonList(BannersFactory
                .getDefaultImageBanner(campaignType)
                .withCreativeBanner(new CreativeBanner().withCreativeId(creativeId))
        )));
        group.withCampaignID(campaignRule.getCampaignId().toString()).
                withGeo(geo.getGeo());
        group.getBanners().get(0).withCid(campaignRule.getCampaignId());
        return group;
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

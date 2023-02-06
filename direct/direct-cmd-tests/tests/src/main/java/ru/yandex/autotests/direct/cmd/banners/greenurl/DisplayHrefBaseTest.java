package ru.yandex.autotests.direct.cmd.banners.greenurl;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;

public abstract class DisplayHrefBaseTest {

    protected static final String CLIENT = "at-backend-display-href";
    protected static final String DISPLAY_HREF = "somehref";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    protected BannersRule bannersRule = new TextBannersRule().
            overrideBannerTemplate(new Banner().withDisplayHref(getDisplayHrefToCreateBannerWith())).
            withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);


    protected Long campaignId;
    protected Long groupId;
    protected Long bannerId;

    @Before
    public void before() {

        campaignId = bannersRule.getCampaignId();
        groupId = bannersRule.getGroupId();
        bannerId = bannersRule.getBannerId();
    }

    protected void editDisplayHref() {
        if (getDisplayHrefToAddToCreatedBanner() == null) {
            return;
        }
        Group group = bannersRule.getGroup();
        group.setAdGroupID(groupId.toString());
        group.getBanners().get(0)
                .withBid(bannerId)
                .withDisplayHref(getDisplayHrefToAddToCreatedBanner())
                .withDomainSign(bannersRule.getCurrentGroup().getBanners().get(0).getDomainSign());

        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(GroupsParameters.forExistingCamp(
                CLIENT, campaignId, group));
    }

    protected String getDisplayHrefToCreateBannerWith() {
        return null;
    }

    protected String getDisplayHrefToAddToCreatedBanner() {
        return DISPLAY_HREF;
    }

    protected void makeAllModerated() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(campaignId);
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(groupId);
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerId);
    }

    protected final String getBannerStatusBsSynced() {
        BannersRecord banner = TestEnvironment.newDbSteps(CLIENT).bannersSteps().getBanner(bannersRule.getBannerId());
        return banner.getStatusbssynced().getLiteral();
    }

    protected String getDisplayHref() {
        return getShowCampMultiEdit().getCampaign().getGroups().get(0).
                getBanners().get(0).getDisplayHref();
    }

    protected String getDisplayHrefStatusModerate() {
        return getShowCampMultiEdit().getCampaign().getGroups().get(0).
                getBanners().get(0).getDisplayHrefStatusModerate();
    }

    protected String getBannerStatusModerate() {
        return getShowCampMultiEdit().getCampaign().getGroups().get(0).
                getBanners().get(0).getStatusModerate();
    }

    protected ShowCampMultiEditResponse getShowCampMultiEdit() {
        return cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(
                CLIENT, campaignId, groupId, bannerId);
    }
}

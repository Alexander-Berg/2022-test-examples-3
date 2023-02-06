package ru.yandex.autotests.direct.cmd.banners.greenurl;

import com.google.gson.Gson;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Копирование отображаемых ссылок в баннерах при копировании кампании")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class CopyCampDisplayHrefTest {

    private static final String CLIENT = "at-backend-display-href";
    private static final String DISPLAY_HREF = "somehref";
    private static final String BANNER_TITLE1 = "текст 1";
    private static final String BANNER_TITLE2 = "текст 2";

    private static BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.stepsClassRule().withRules(bannersRule);

    private static Banner sourceBannerWithoutHref;
    private static Banner sourceBannerWithHref;
    private static Banner copiedBannerWithoutHref;
    private static Banner copiedBannerWithHref;
    private static Long copiedCampaignId;

    @BeforeClass
    public static void beforeClass() {
        createBannerWithDisplayHref();
        copiedCampaignId = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT, bannersRule.getCampaignId());
        getCopiedBanners(copiedCampaignId);
    }

    @AfterClass
    public static void afterClass(){
            if (copiedCampaignId != null) {
                cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, copiedCampaignId);
            }
    }

    private static void createBannerWithDisplayHref() {
        Group group = bannersRule.getGroup();

        Banner bannerWithoutDisplayHref = group.getBanners().get(0);

        Banner bannerWithDisplayHref = copyBanner(bannerWithoutDisplayHref).withDisplayHref(DISPLAY_HREF);
        group.getBanners().add(bannerWithDisplayHref);

        group.setCampaignID(bannersRule.getCampaignId().toString());
        group.setAdGroupID(bannersRule.getGroupId().toString());
        group.getBanners().forEach(b -> b.withCid(bannersRule.getCampaignId()));

        bannerWithoutDisplayHref.setBid(bannersRule.getBannerId());
        bannerWithoutDisplayHref.setTitle(BANNER_TITLE1);
        bannerWithDisplayHref.setTitle(BANNER_TITLE2);

        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupRequest);

        Group receivedGroup = cmdRule.cmdSteps().groupsSteps().getAdGroup(CLIENT, bannersRule.getGroupId());
        sourceBannerWithoutHref = receivedGroup.getBanners().
                stream().filter(b -> b.getTitle().equals(BANNER_TITLE1)).findFirst().get();
        sourceBannerWithHref = receivedGroup.getBanners().
                stream().filter(b -> b.getTitle().equals(BANNER_TITLE2)).findFirst().get();
    }

    private static Banner copyBanner(Banner banner) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(banner), Banner.class);
    }

    private static void getCopiedBanners(Long copiedCampaignId) {
        ShowCampResponse showCampResponse =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, copiedCampaignId.toString());
        Long copiedGroupId = showCampResponse.getGroups().get(0).getAdGroupId();
        Group group = cmdRule.cmdSteps().groupsSteps().getAdGroup(CLIENT, copiedGroupId);
        copiedBannerWithoutHref = group.getBanners().
                stream().filter(b -> b.getTitle().equals(BANNER_TITLE1)).findFirst().get();
        copiedBannerWithHref = group.getBanners().
                stream().filter(b -> b.getTitle().equals(BANNER_TITLE2)).findFirst().get();
    }

    @Test
    @Description("Копирование отображаемых ссылок в баннерах при копировании кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9193")
    public void testExistingDisplayHrefAtCopyCamp() {
        assumeThat("в исходном баннере, который должен быть без ссылки, она отсутствует",
                sourceBannerWithoutHref.getDisplayHref(), nullValue());
        assumeThat("в исходном баннере, который должен быть со ссылкой, она присутствует",
                sourceBannerWithHref.getDisplayHref(), equalTo(DISPLAY_HREF));
        assertThat("в скопированном баннере, который должен быть без ссылки, она отсутствует",
                copiedBannerWithoutHref.getDisplayHref(), nullValue());
        assertThat("в скопированном баннере, который должен быть со ссылкой, она присутствует",
                copiedBannerWithHref.getDisplayHref(), equalTo(DISPLAY_HREF));
    }
}

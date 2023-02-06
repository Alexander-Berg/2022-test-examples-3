package ru.yandex.autotests.direct.cmd.campaigns.showcamp;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.VideoAddition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


@Aqua.Test
@Description("Проверка наличия медиаресурсов на страницах просмотра и редактирования кампании")
@Stories(TestFeatures.Banners.MEDIA_RESOURCES)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.AUTO_VIDEO)
@Tag(CampTypeTag.TEXT)
public class ShowCampBannerVideoTest {

    private static final String CLIENT = "at-direct-video-banner55";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule, videoAdditionCreativeRule);
    private Long videoAdditionCreativeId;

    @Before
    public void before() {
        videoAdditionCreativeId = videoAdditionCreativeRule.getCreativeId();
        bannersRule.updateCurrentGroupBy(g -> {
                    g.getBanners().get(0).addDefaultVideoAddition(videoAdditionCreativeId);
                    return g;
                }
        );
    }

    @After
    public void after() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannerResourcesSteps().deleteBannerResourceByBid(bannersRule.getBannerId());
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10672")
    public void showCampBannerVideoTest() {
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT,
                bannersRule.getCampaignId().toString()
        );
        Banner banner = response.getGroups().get(0);

        VideoAddition videoAddition = banner.getVideoResources();
        assumeThat("у баннера есть видео-креатив", videoAddition, notNullValue());
        assertThat("видео-креатив баннера соответствуeт ожиданию", videoAddition.getId(),
                equalTo(videoAdditionCreativeId));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10673")
    public void showCampMultieditBannerVideoTest() {
        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.forSingleBanner(
                CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId(), bannersRule.getBannerId());
        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(request);
        Banner banner = response.getCampaign().getGroups().get(0).getBanners().get(0);
        VideoAddition videoAddition = banner.getVideoResources();
        assumeThat("у баннера есть видео-креатив", videoAddition, notNullValue());
        assertThat("видео-креатив баннера соответствуeт ожиданию", videoAddition.getId(),
                equalTo(videoAdditionCreativeId));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10674")
    public void getAdGroupsBannerVideoTest() {
        Group group = cmdRule.cmdSteps().groupsSteps()
                .getAdGroup(CLIENT, bannersRule.getGroupId());
        Banner banner = group.getBanners().get(0);
        VideoAddition videoAddition = banner.getVideoResources();
        assumeThat("у баннера есть видео-креатив", videoAddition, notNullValue());
        assertThat("видео-креатив баннера соответствуeт ожиданию", videoAddition.getId(),
                equalTo(videoAdditionCreativeId));
    }
}

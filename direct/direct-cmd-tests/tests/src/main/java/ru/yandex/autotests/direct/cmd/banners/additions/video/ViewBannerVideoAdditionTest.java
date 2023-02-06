package ru.yandex.autotests.direct.cmd.banners.additions.video;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


@Aqua.Test
@Description("Проверка наличия видеодополнения на страницах просмотра и редактирования кампании/группы")
@Stories(TestFeatures.Banners.VIDEO_ADDITION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CmdTag.GET_AD_GROUP)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class ViewBannerVideoAdditionTest {
    private static final String CLIENT = "at-direct-video-addition-1";

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannerNoVideoRule = new TextBannersRule().withUlogin(CLIENT);
    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);
    private BannersRule bannerWithVideoRule =
            new TextBannersRule()
                    .withVideoAddition(videoAdditionCreativeRule)
                    .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(bannerNoVideoRule, videoAdditionCreativeRule, bannerWithVideoRule);

    @Test
    @Description("При просмотре кампании с баннером без видеодополнения, у баннера нет видеодополнения")
    @TestCaseId("10935")
    public void testShowCampBannerNoVideoAddition() {
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT,
                bannerNoVideoRule.getCampaignId().toString()
        );
        Banner banner = response.getGroups().get(0);
        assumeThat("video_resources всегда присутствует", banner.getVideoResources(), notNullValue());
        assertThat("у баннера нет видеодополнения", banner.getVideoResources().getId(),
                nullValue()
        );
    }

    @Test
    @Description("При просмотре кампании с баннером с видеодополнением, у баннера есть видеодополнение")
    @TestCaseId("10936")
    public void testShowCampBannerWithVideoAddition() {
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT,
                bannerWithVideoRule.getCampaignId().toString()
        );
        Banner banner = response.getGroups().get(0);
        assumeThat("video_resources всегда присутствует", banner.getVideoResources(), notNullValue());
        assertThat("у баннера есть видеодополнение", banner.getVideoResources().getId(),
                equalTo(bannerWithVideoRule.getBanner().getVideoResources().getId())
        );
    }

    @Test
    @Description("При просмотре группы с баннером без видеодополнения, у баннера нет видеодополнения")
    @TestCaseId("10937")
    public void testGetAdGroupBannerNoVideoAddition() {
        Group response = cmdRule.cmdSteps().groupsSteps().getAdGroup(CLIENT, bannerNoVideoRule.getGroupId());

        Banner banner = response.getBanners().get(0);
        assumeThat("video_resources всегда присутствует", banner.getVideoResources(), notNullValue());
        assertThat("у баннера нет видеодополнения", banner.getVideoResources().getId(),
                nullValue()
        );
    }

    @Test
    @Description("При просмотре группы с баннером с видеодополнением, у баннера есть видеодополнение")
    @TestCaseId("10938")
    public void testGetAdGroupBannerWithVideoAddition() {
        Group response = cmdRule.cmdSteps().groupsSteps().getAdGroup(CLIENT, bannerWithVideoRule.getGroupId());

        Banner banner = response.getBanners().get(0);
        assumeThat("video_resources всегда присутствует", banner.getVideoResources(), notNullValue());
        assertThat("у баннера есть видеодополнение", banner.getVideoResources().getId(),
                equalTo(bannerWithVideoRule.getBanner().getVideoResources().getId())
        );
    }

    @Test
    @Description("При редактировании группы с баннером без видеодополнения, у баннера нет видеодополнения")
    @TestCaseId("10939")
    public void testShowCampMultiEditBannerNoVideoAddition() {
        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps()
                .getShowCampMultiEdit(CLIENT,
                        bannerNoVideoRule.getCampaignId(),
                        bannerNoVideoRule.getGroupId(),
                        bannerNoVideoRule.getBannerId());

        Banner banner = response.getCampaign().getGroups().get(0).getBanners().get(0);
        assumeThat("video_resources всегда присутствует", banner.getVideoResources(), notNullValue());
        assertThat("у баннера нет видеодополнения", banner.getVideoResources().getId(),
                nullValue()
        );
    }

    @Test
    @Description("При редактировании группы с баннером с видеодополнением, у баннера есть видеодополнение")
    @TestCaseId("10940")
    public void testShowCampMultiEditBannerWithVideoAddition() {
        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps()
                .getShowCampMultiEdit(CLIENT,
                        bannerWithVideoRule.getCampaignId(),
                        bannerWithVideoRule.getGroupId(),
                        bannerWithVideoRule.getBannerId());

        Banner banner = response.getCampaign().getGroups().get(0).getBanners().get(0);
        assumeThat("video_resources всегда присутствует", banner.getVideoResources(), notNullValue());
        assertThat("у баннера есть видеодополнение", banner.getVideoResources().getId(),
                equalTo(bannerWithVideoRule.getBanner().getVideoResources().getId())
        );
    }
}

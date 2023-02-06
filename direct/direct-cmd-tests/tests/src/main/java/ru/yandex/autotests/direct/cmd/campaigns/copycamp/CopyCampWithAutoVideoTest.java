package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import com.yandex.direct.api.v5.campaigns.CampaignGetItem;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.VideoAddition;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.CampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
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
@Description("Проверка копирования текстовой кампании с auto video (copyCamp)")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.AUTO_VIDEO)
@Tag(CampTypeTag.TEXT)
public class CopyCampWithAutoVideoTest {
    private static final String CLIENT_1 = "at-direct-auto-video-1";
    private static final String CLIENT_2 = "at-direct-auto-video-2";
    private String newCidClient;
    private Long videoAdditionCreativeId;
    private Long newCid;

    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT_1);
    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT_1);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule, videoAdditionCreativeRule);

    @Before
    public void before() {
        cmdRule.apiSteps().campaignSteps().getCampaigns(CLIENT_1)
                .stream()
                .filter(camp -> camp.getName().contains("копия"))
                .map(CampaignGetItem::getId)
                .forEach(cid ->  cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT_1, cid));

        videoAdditionCreativeId = videoAdditionCreativeRule.getCreativeId();
        bannersRule.updateCurrentGroupBy(g -> {
                    g.getBanners().get(0).addDefaultVideoAddition(videoAdditionCreativeId);
                    return g;
                }
        );
    }

    @After
    public void after() {
        if (newCid != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(newCidClient, Long.parseLong(newCid.toString()));
        }
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT_1)
                .bannerResourcesSteps().deleteBannerResourceByBid(bannersRule.getBannerId());
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10669")
    public void copyCampWithAutoVideo() {
        newCidClient = CLIENT_1;
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT_1, bannersRule.getCampaignId());
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT_1, newCid.toString());
        Banner banner = response.getGroups().get(0);

        VideoAddition videoAddition = banner.getVideoResources();
        assumeThat("у баннера есть видео-креатив", videoAddition, notNullValue());
        assertThat("вместе с кампанией скопировалось видео дополнение баннера", videoAddition.getId(),
                equalTo(videoAdditionCreativeId));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10670")
    public void copyCampWithAutoVideoOtherClient() {
        newCidClient = CLIENT_2;
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCamp(
                CLIENT_1,
                CLIENT_2,
                bannersRule.getCampaignId(),
                StatusModerate.YES.toString()
        );
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT_2, newCid.toString());
        Banner banner = response.getGroups().get(0);
        assertThat("вместе с кампанией не скопировалось видео дополнение баннера",
                banner.getVideoResources().getId(), nullValue());
    }

}

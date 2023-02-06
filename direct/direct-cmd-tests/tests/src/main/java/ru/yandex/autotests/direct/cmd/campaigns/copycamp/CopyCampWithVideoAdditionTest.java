package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.VideoAddition;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка копирования текстовой кампании с видеодополнением (copyCamp/copyCampClient)")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.VIDEO_ADDITION)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@Tag("DIRECT-63700")
public class CopyCampWithVideoAdditionTest {
    private static final String CLIENT_1 = "at-direct-video-addition-1";
    private static final String CLIENT_2 = "at-direct-video-addition-2";

    private Long newCid;
    private String newLogin;

    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT_1);
    private TextBannersRule bannersRule = new TextBannersRule()
            .withVideoAddition(videoAdditionCreativeRule)
            .withUlogin(CLIENT_1);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(videoAdditionCreativeRule, bannersRule);

    @After
    public void after() {
        if (newCid != null && newLogin != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(newLogin, newCid);
        }
    }

    @Test
    @TestCaseId("10948")
    public void copyCampWithVideoAdditionWithinClient() {
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT_1, bannersRule.getCampaignId());
        newLogin = CLIENT_1;
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT_1, newCid.toString());
        VideoAddition videoAddition = response.getGroups().get(0).getVideoResources();

        assumeThat("в ответе showCamp есть video_resources", videoAddition, notNullValue());
        assertThat("вместе с кампанией скопировалось видеодополнение баннера",
                videoAddition.getId(),
                equalTo(videoAdditionCreativeRule.getCreativeId()));
    }

    @Test
    @TestCaseId("10949")
    public void copyCampWithVideoAdditionOtherClient() {
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCamp(
                CLIENT_1,
                CLIENT_2,
                bannersRule.getCampaignId(),
                StatusModerate.YES.toString()
        );
        newLogin = CLIENT_2;
        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT_2, newCid.toString());
        VideoAddition videoAddition = response.getGroups().get(0).getVideoResources();
        assertThat("видеодополнение не скопировалось на другого клиента",
                videoAddition.getId(),
                nullValue());
    }

}

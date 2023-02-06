package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedCampaign;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.stream.Collectors;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка не дублирования одной кампании в списке кампаний фида")
@Stories(TestFeatures.Feeds.AJAX_GET_FEEDS)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.AJAX_GET_FEEDS)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
public class FeedsCampaignListDuplicateTest {

    private static final String CLIENT = "at-direct-backend-feeds-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private PerformanceBannersRule campaignRule =
            (PerformanceBannersRule) new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);

    private Long campaignId;
    private Long feedId;

    @Before
    public void before() {
        campaignId = campaignRule.getCampaignId();
        feedId = campaignRule.getFeedId();
        createGroup();
    }

    @Test
    @Description("Проверяем отсутствия дублирования кампаний у фида")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9743")
    public void checkCampaignList() {
        AjaxFeedsResponse response = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(CLIENT);
        Feed feedResponse = response.getFeedsResult().getFeeds().stream()
                .filter(t -> t.getFeedId().equals(feedId.toString())).findFirst().get();
        assertThat("количество кампаний соотетстует ожидаемому", feedResponse.getCampaigns(), hasSize(1));
        assertThat("кампания связана с фидом", feedResponse.getCampaigns().stream()
                .map(FeedCampaign::getId).collect(Collectors.toList()), hasItem(campaignId));
    }

    private void createGroup() {
        Group group = campaignRule.getGroup().withCampaignID(campaignId.toString()).withFeedId(feedId.toString());
        group.getBanners().get(0)
                .getCreativeBanner().withCreativeId(campaignRule.getCreativeId());
        GroupsParameters groupRequest = GroupsParameters.forNewCamp(CLIENT, campaignId, group);
        cmdRule.cmdSteps().groupsSteps().postSavePerformanceAdGroups(groupRequest);
    }
}

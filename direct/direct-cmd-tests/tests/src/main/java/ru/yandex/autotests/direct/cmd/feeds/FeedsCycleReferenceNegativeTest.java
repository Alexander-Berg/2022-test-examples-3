package ru.yandex.autotests.direct.cmd.feeds;

import java.io.File;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.hamcrest.core.IsNot;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.feeds.Category;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedBusinessType;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSourceType;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.FileUtils;
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper.generateUniqueFeedStringPath;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка отображения категорий циклического фида")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(CmdTag.SHOW_CAMP)
@Tag(ObjectTag.FEED)
public class FeedsCycleReferenceNegativeTest {
    private static final String CLIENT = "at-direct-perf-groups3";
    private static final String FEED_CYCLE_REF_FILE = generateUniqueFeedStringPath("feeds/test_cycle.xml");
    private static final String FEED_NAME = "CycleFeed";
    private String feedId;
    private String campaignId;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public PerformanceBannersRule bannersRule = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        campaignId = String.valueOf(bannersRule.getCampaignId());
        createFeed();
    }

    @After
    public void after() {
        if (feedId != null) {
            FeedHelper.deleteFeed(cmdRule, CLIENT, Long.parseLong(feedId));
        }
        if (FEED_CYCLE_REF_FILE != null) {
            File file = new File(FEED_CYCLE_REF_FILE);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9748")
    public void checkSaveFeedCycleReferenceInFileAtSaveFeed() {
        EditAdGroupsPerformanceResponse response = cmdRule.cmdSteps().groupsSteps()
                .getAddAdGroupsPerformance(CLIENT, campaignId);
        assertThat("фид не отображается", response.getFeeds().stream().map(Feed::getFeedId)
                .collect(Collectors.toList()), IsNot.not(hasItem(feedId)));
    }

    private void createFeed() {
        FeedSaveRequest feedSaveRequest = new FeedSaveRequest()
                .withName(FEED_NAME)
                .withSource(FeedSourceType.FILE.getValue())
                .withBusinessType(FeedBusinessType.RETAIL)
                .withFeedFile(FileUtils.getFilePath(FEED_CYCLE_REF_FILE))
                .withUlogin(CLIENT);
        RedirectResponse response = cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, RedirectResponse.class);
        assumeThat("фид добавился", response.getLocationParam(LocationParam.CMD), equalTo(CMD.SHOW_FEEDS.toString()));

        feedId = getFeedId();
        PerformanceCampaignHelper.waitForFeedLoad(cmdRule.cmdSteps(), User.get(CLIENT).getClientID(), feedId);
    }

    private String getFeedId() {
        return cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(CLIENT).getFeedsResult()
                .getFeeds().stream()
                .max(Comparator.comparing(Feed::getLastChange)).get().getFeedId();
    }
}

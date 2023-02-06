package ru.yandex.autotests.direct.cmd.feeds.removeutm;


import java.io.File;
import java.util.Comparator;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.feeds.AjaxFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSourceType;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.cmd.util.FileUtils;

import static ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper.generateUniqueFeedStringPath;

public abstract class FeedsCheckBoxBaseTest {


    protected final static String FEED_URL = "http://direct-qa.s3.mds.yandex.net/feeds/valid.xml";
    protected final static String FEED_FILE = generateUniqueFeedStringPath("feeds/valid.xml");

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    protected Long feedId;
    protected Feed savedFeed;
    protected FeedSaveRequest feedSaveRequest;
    protected AjaxFeedsResponse ajaxFeedsResponse;

    @Before
    public void before() {
        ajaxFeedsResponse = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(getClient());
        deleteFeeds();
    }

    abstract FeedSourceType getSource();

    abstract String getFeedUrl();

    abstract String getFeedFile();

    abstract String getCheckBox();

    abstract String getClient();

    protected void createFeed() {
        feedSaveRequest = BeanLoadHelper.loadCmdBean("saveFeed.request.default", FeedSaveRequest.class);
        feedSaveRequest.setName(feedSaveRequest.getName() + " new");
        feedSaveRequest.setSource(getSource().getValue());
        feedSaveRequest.setUrl(getFeedUrl());
        String filePath = getFeedFile() == null ? null : FileUtils.getFilePath(getFeedFile());
        feedSaveRequest.setFeedFile(filePath);
        feedSaveRequest.setUlogin(getClient());
        feedSaveRequest.withRemoveUtm(getCheckBox());
        cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, RedirectResponse.class);
        ajaxFeedsResponse = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(getClient());

        savedFeed = ajaxFeedsResponse.getFeedsResult().getFeeds()
                .stream().max(Comparator.comparing(Feed::getLastChange)).orElse(null);
    }

    protected void deleteFeeds() {
        if (ajaxFeedsResponse != null) {
            ajaxFeedsResponse.getFeedsResult().getFeeds()
                    .stream().forEach((feed) -> {
                feedId = Long.valueOf(feed.getFeedId());
                FeedHelper.deleteFeed(cmdRule,getClient(), feedId);
            });
        }
    }

    @AfterClass
    public static void afterClass() {
        if (FEED_FILE != null) {
            File file = new File(FEED_FILE);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}

package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.*;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Comparator;

import static ru.yandex.autotests.direct.httpclient.util.beanmapper.BeanMapper.map;
import static ru.yandex.autotests.directapi.matchers.beandiffer2.Api5CompareStrategies.onlyExpectedFields;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public abstract class FeedsAtSaveFeedTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    protected FeedsRecord defaultFeed;
    protected FeedSaveRequest feedSaveRequest;
    private Long feedId;


    protected abstract String getClient();

    @Before
    public void before() {
        feedSaveRequest = getFeedSaveRequest();
    }

    @After
    public void deleteFeed() {
        if (feedId != null) {
            FeedHelper.deleteFeed(cmdRule, getClient(), feedId);
        }
    }

    @Description("Проверяем создание фида")
    public void checkSaveNewFeedAtSaveFeed() {
        cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, RedirectResponse.class);
        AjaxFeedsResponse ajaxFeedsResponse = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(getClient());

        Feed actualBean = ajaxFeedsResponse.getFeedsResult().getFeeds()
                .stream().max(Comparator.comparing(Feed::getLastChange)).orElse(null);

        assertThat("описание фида соответствует созданному",
                actualBean,
                beanDiffer(getExpectedFeed()).useCompareStrategy(onlyExpectedFields()));
        feedId = Long.valueOf(actualBean.getFeedId());
    }

    @Description("Проверяем сохранение фида")
    public void checkSaveFeedAtSaveFeed() {
        feedId = TestEnvironment.newDbSteps().useShardForLogin(getClient()).feedsSteps()
                .createFeed(defaultFeed, User.get(getClient()).getClientID());

        feedSaveRequest.setFeedId(String.valueOf(feedId));

        cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, RedirectResponse.class);
        AjaxFeedsResponse ajaxFeedsResponse = cmdRule.cmdSteps().ajaxGetFeedsSteps()
                .getFeeds(getClient());

        assertThat("описание фида соответствует сохраненному",
                ajaxFeedsResponse.getFeedsResult().getFeeds()
                        .stream().filter(t -> t.getFeedId().equals(String.valueOf(feedId))).findFirst().orElse(null),
                beanDiffer(getExpectedChangeFeed()).useCompareStrategy(onlyExpectedFields()));
    }

    protected FeedSaveRequest getFeedSaveRequest() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class)
                .withUlogin(getClient());
    }

    protected Feed getExpectedFeed() {
        return map(feedSaveRequest, Feed.class);
    }

    protected Feed getExpectedChangeFeed() {
        return getExpectedFeed();
    }
}

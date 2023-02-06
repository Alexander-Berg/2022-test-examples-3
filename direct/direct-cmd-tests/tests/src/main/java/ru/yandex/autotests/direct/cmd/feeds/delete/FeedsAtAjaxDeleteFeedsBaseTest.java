package ru.yandex.autotests.direct.cmd.feeds.delete;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxDeleteFeedsRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxDeleteFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.model.User;

public abstract class FeedsAtAjaxDeleteFeedsBaseTest {
    protected final static String SUPER = Logins.SUPER;
    protected final static String CLIENT = "at-direct-backend-feeds-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected AjaxDeleteFeedsResponse expectedBean;
    protected FeedSaveRequest feedSaveRequest;
    protected AjaxDeleteFeedsRequest deleteFeedsRequest;
    protected Long feedId;
    protected Long secondFeedId;
    protected FeedsRecord defaultFeed;

    @BeforeClass
    public static void beforeClass() {

    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(SUPER));

        defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(CLIENT).getClientID());
        feedId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeed(defaultFeed, User.get(CLIENT).getClientID());
        secondFeedId = TestEnvironment.newDbSteps().feedsSteps()
                .createFeed(defaultFeed, User.get(CLIENT).getClientID());

        feedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class);
        feedSaveRequest.setUlogin(CLIENT);

        deleteFeedsRequest = new AjaxDeleteFeedsRequest();
        deleteFeedsRequest.setFeedsIds(String.valueOf(feedId), String.valueOf(secondFeedId));
        deleteFeedsRequest.setUlogin(CLIENT);
    }

    @After
    public void deleteFeed() {
        if (feedId != null) {
            TestEnvironment.newDbSteps().feedsSteps()
                    .updateFeedsStatus(FeedsUpdateStatus.Done, User.get(CLIENT).getClientID(), feedId, secondFeedId);
            cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeed(deleteFeedsRequest);
        }
    }
}

package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxDeleteFeedsRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedsErrors;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;


import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка сохранения фида контроллером SaveFeed при превышении лимита на кол-во фидов")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
public class FeedsShowsNegativeCountAtSaveFeedTest {

    protected static final String SUPER = Logins.SUPER;
    protected static final String CLIENT = "at-direct-backend-feeds-c1";
    private static final String DELETE_FEEDS_COUNT_ERROR = FeedsErrors.FEEDS_SAVE_COUNT_ERROR.toString();

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected AjaxDeleteFeedsRequest deleteFeedsRequest;
    private FeedSaveRequest feedSaveRequest;


    @Before
    public void before() {
        FeedsRecord defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(CLIENT).getClientID());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeeds(User.get(CLIENT).getClientID(), defaultFeed, 50);

        feedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class);
        feedSaveRequest.setUlogin(CLIENT);

        deleteFeedsRequest = new AjaxDeleteFeedsRequest();
        deleteFeedsRequest.setUlogin(CLIENT);

        cmdRule.cmdSteps().authSteps().authenticate(User.get(SUPER));
    }

    @After
    public void deleteFeed() {
        FeedHelper.deleteAllFeeds(cmdRule, CLIENT);
    }

    @Test
    @Description("Проверяем появление ошибки при создании фида при кол-ве фидов = 50")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9754")
    public void checkSaveNewFeedAtSaveFeed() {
        ErrorResponse actualErrorResponse = cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, ErrorResponse.class);

        assertThat("Ошибка не соответствует ожиданиям", actualErrorResponse.getError(), equalTo(DELETE_FEEDS_COUNT_ERROR));
    }
}

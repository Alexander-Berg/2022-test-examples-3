package ru.yandex.autotests.direct.cmd.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CSRFToken;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CommonTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Реакция на неправильный csrf-токен")
@Stories(TestFeatures.Common.CSRF)
@Features(TestFeatures.COMMON)
@RunWith(Parameterized.class)
@Tag(CommonTag.YES)
@Tag(SmokeTag.YES)
public class InvalidCsrfTokenTest {

    private final static String SUPER = Logins.SUPER;
    private final static String CLIENT = "at-direct-cmd-csrf";
    private final static CSRFToken INVALID_CSRF_TOKEN = new CSRFToken("fn-rTLjEV-3DhPGk");  // lc-rTLjEV-3DhPGk
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter
    public CSRFToken csrfToken;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    private FeedSaveRequest newFeedSaveRequest;

    @Parameterized.Parameters(name = "csrf-токен = {0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {null},
                {CSRFToken.EMPTY},
                {INVALID_CSRF_TOKEN}
        });
    }

    @Before
    public void before() {
        newFeedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class);
        newFeedSaveRequest.setUlogin(CLIENT);

        cmdRule.cmdSteps().authSteps().authenticate(User.get(SUPER));
        deleteAllFeeds(CLIENT);
    }

    @Test
    @Description("Реакция на неправильный csrf-токен на ручке создания фида")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9594")
    public void checkInvalidCsrfToken() {
        cmdRule.cmdSteps().saveFeedsSteps().saveFeedWithCustomCsrfToken(newFeedSaveRequest, csrfToken, Void.class);
        AjaxFeedsResponse ajaxFeedsResponse2 = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(CLIENT);
        Feed savedFeed = ajaxFeedsResponse2.getFeedsResult().getFeeds()
                .stream().max(Comparator.comparing(Feed::getLastChange)).orElse(null);
        assertThat("фид не создался без csrf-токена", savedFeed, nullValue());
    }

    @Step("Удаление всех фидов клиента {0}")
    public void deleteAllFeeds(String login) {
        AjaxFeedsResponse ajaxFeedsResponse = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(login);
        List<Long> feedsIds = ajaxFeedsResponse.getFeedsResult().getFeeds()
                .stream()
                .map(feed -> Long.valueOf(feed.getFeedId()))
                .collect(Collectors.toList());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps().updateFeedsStatus(FeedsUpdateStatus.Done,
                User.get(CLIENT).getClientID(), feedsIds);
        cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeeds(login, feedsIds);
    }
}

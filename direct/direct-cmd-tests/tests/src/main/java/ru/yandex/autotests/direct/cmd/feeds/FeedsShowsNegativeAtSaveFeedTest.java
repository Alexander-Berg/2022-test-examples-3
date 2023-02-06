package ru.yandex.autotests.direct.cmd.feeds;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.cmd.util.FileUtils;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка получения ошибок при сохранении фида контроллером SaveFeed")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
public class FeedsShowsNegativeAtSaveFeedTest {

    protected static final String SUPER = Logins.SUPER;
    protected static final String CLIENT = "at-direct-backend-feeds-c2";
    private static final String FEED_PDF_FILE = "http://direct-qa.s3.mds.yandex.net/feeds/test.pdf";


    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private FeedSaveRequest feedSaveRequest;


    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(SUPER));
        deleteAllFeeds();
    }

    @After
    public void deleteFeed() {
        deleteAllFeeds();
    }

    private void deleteAllFeeds() {
        List<Long> existingIds = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(CLIENT).getFeedsResult()
                .getFeeds().stream().map(t -> Long.valueOf(t.getFeedId())).collect(Collectors.toList());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .updateFeedsStatus(FeedsUpdateStatus.Done, User.get(CLIENT).getClientID(), existingIds);
        cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeeds(CLIENT, existingIds);
    }

    private void checkFeed() {
        feedSaveRequest.setUlogin(CLIENT);
        cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, Void.class);
        AjaxFeedsResponse feedsResponseBean = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(CLIENT);
        assertThat("Фид не добавился", feedsResponseBean.getFeedsResult().getFeeds(), is(empty()));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9751")
    public void checkSaveFeedNullNameAtSaveFeed() {
        feedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class);
        feedSaveRequest.setName(null);
        checkFeed();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9752")
    public void checkSaveFeedNullURLAtSaveFeed() {
        feedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class);
        feedSaveRequest.setUrl(null);
        checkFeed();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9753")
    public void checkSaveFeedNullFileNameAtSaveFeed() {
        feedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class);
        feedSaveRequest.setUrl(null);
        feedSaveRequest.setFeedFile(null);
        checkFeed();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9750")
    public void checkSaveFeedInvalidFileTypeAtSaveFeed() {
        feedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class);
        feedSaveRequest.setSource("file");
        feedSaveRequest.setUrl(null);
        feedSaveRequest.setFeedFile(FileUtils.downloadToTempFile(FEED_PDF_FILE).getAbsolutePath());
        checkFeed();
    }
}

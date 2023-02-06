package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.clients.ModifyUserModel;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedsErrors;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.cmd.util.FileUtils;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка сохранения фида контроллером SaveFeed при превышении размера файла")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
public class FeedsShowsNegativeLargeFileAtSaveFeedTest {

    private final static String SUPER = Logins.SUPER;
    private final static String CLIENT = "at-direct-backend-feeds-c4";
    private final static String FEED_XML_FILE = "http://direct-qa.s3.mds.yandex.net/feeds/test.xml";
    private final static String FEEDS_SAVE_FILE_ABOVE_MAX_ERROR = FeedsErrors.FEEDS_SAVE_FILE_ABOVE_MAX_ERROR.toString();

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private FeedSaveRequest feedSaveRequest;


    @Before
    public void before() {
        feedSaveRequest = new FeedSaveRequest();

        cmdRule.cmdSteps().authSteps().authenticate(User.get(SUPER));
        FeedHelper.deleteAllFeeds(cmdRule, CLIENT);

        ModifyUserModel modifyUserRequest = BeanLoadHelper.loadCmdBean(
                "modifyUser.model.forFeedsTests", ModifyUserModel.class);
        modifyUserRequest.setUlogin(CLIENT);
        modifyUserRequest.setRulogin(CLIENT);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUser(modifyUserRequest);

        feedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class);
        feedSaveRequest.setUlogin(CLIENT);
        feedSaveRequest.setFeedFile(FileUtils.downloadToTempFile(FEED_XML_FILE).getAbsolutePath());
    }

    @After
    public void deleteFeed() {
        FeedHelper.deleteAllFeeds(cmdRule, CLIENT);
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Проверяем появление ошибки при сохранении фида с файлом большего размера, чем максимально допустимое")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9755")
    public void checkSaveNewFeedInvalidFileSizeAtSaveFeed() {
        ErrorResponse actualErrorResponse = cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, ErrorResponse.class);
        assertThat("Ошибка не соответствует ожиданиям", actualErrorResponse.getError(), equalTo(FEEDS_SAVE_FILE_ABOVE_MAX_ERROR));
    }
}

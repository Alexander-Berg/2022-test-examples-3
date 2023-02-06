package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedsErrors;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
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

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка невозможности изменения фида другого клиента контроллером SaveFeed")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@RunWith(Parameterized.class)
public class FeedsAtSaveFeedAnotherLoginNegativeTest {

    private static final String SUPER = Logins.SUPER;
    private static final String CLIENT = "at-direct-backend-feeds-save-c";
    private final static String FEED_URL = "http://ya.ru";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(value = 0)
    public String anotherUser;
    @Parameterized.Parameter(value = 1)
    public String feedError;

    private Long feedId;

    @Parameterized.Parameters(name = "Проверка невозможности отредактировать фида под {0} у другого клиента")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Logins.MANAGER, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()},
                {Logins.AGENCY, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString()},
                {Logins.DEFAULT_CLIENT, FeedsErrors.FEED_NOT_FOUND.toString()}
        });
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(anotherUser));
        FeedsRecord defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(CLIENT).getClientID());
        feedId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeed(defaultFeed, User.get(CLIENT).getClientID());
    }

    @After
    public void deleteFeed() {
        if (feedId != null) {
            FeedHelper.deleteFeed(cmdRule, CLIENT, feedId);
        }
    }

    @Test
    @Description("Проверяем сохранение фида")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9740")
    public void checkSaveFeedAtSaveFeed() {
        FeedSaveRequest feedSaveRequest =
                BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class);
        feedSaveRequest.setName(feedSaveRequest.getName() + " new");
        feedSaveRequest.setUrl(FEED_URL);
        feedSaveRequest.setFeedId(String.valueOf(feedId));

        ErrorResponse errorResponse = cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, ErrorResponse.class);
        assertThat("вернулась ошибка, при попытке отредактировать чужой фид",
                errorResponse.getError().replaceAll("!", ""), equalTo(feedError));
    }

}

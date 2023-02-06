package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedBusinessType;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Проверка сохранения типа бизнеса фида контроллером SaveFeed")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@Tag("TESTIRT-9903")
@RunWith(Parameterized.class)
public class FeedBusinessTypeAtSaveFeedTest extends FeedsAtSaveFeedTestBase {

    private final static String SUPER = Logins.SUPER;
    private final static String CLIENT = "at-direct-back-feeds-save-c2";

    @Parameterized.Parameter(0)
    public FeedBusinessType saveFeedBusinessType;

    @Parameterized.Parameter(1)
    public FeedBusinessType expectedFeedBusinessType;

    @Parameterized.Parameters(name = "Проверка сохранения типа бизнеса {0} фида")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {FeedBusinessType.RETAIL, FeedBusinessType.RETAIL},
                {FeedBusinessType.AUTO, FeedBusinessType.RETAIL},
                {FeedBusinessType.FLIGHTS, FeedBusinessType.RETAIL},
                {FeedBusinessType.HOTELS, FeedBusinessType.RETAIL},
                {FeedBusinessType.REALTY, FeedBusinessType.RETAIL}
        });
    }

    @Before
    public void before() {
        super.before();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(SUPER));
        defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(CLIENT).getClientID());
    }

    @Override
    protected FeedSaveRequest getFeedSaveRequest() {
        return super.getFeedSaveRequest()
                .withBusinessType(saveFeedBusinessType);
    }

    @Override
    protected Feed getExpectedChangeFeed() {
        return super.getExpectedChangeFeed()
                .withBusinessType(expectedFeedBusinessType);
    }

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10614")
    public void checkSaveNewFeedAtSaveFeed() {
        super.checkSaveNewFeedAtSaveFeed();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10615")
    public void checkSaveFeedAtSaveFeed() {
        super.checkSaveFeedAtSaveFeed();
    }
}

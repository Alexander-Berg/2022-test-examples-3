package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
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

@Aqua.Test
@Description("Проверка сохранения фида контроллером SaveFeed")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class FeedsAtSaveFeedTest extends FeedsAtSaveFeedTestBase {

    private final static String SUPER = Logins.SUPER;
    private final static String CLIENT = "at-direct-backend-feeds-save-c";

    @Before
    public void before() {
        super.before();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(SUPER));

        defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(CLIENT).getClientID());
    }

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9741")
    public void checkSaveNewFeedAtSaveFeed() {
        super.checkSaveNewFeedAtSaveFeed();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9742")
    public void checkSaveFeedAtSaveFeed() {
        super.checkSaveFeedAtSaveFeed();
    }
}

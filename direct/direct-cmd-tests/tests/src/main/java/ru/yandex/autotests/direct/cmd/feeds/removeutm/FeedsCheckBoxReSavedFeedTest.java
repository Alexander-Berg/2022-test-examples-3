package ru.yandex.autotests.direct.cmd.feeds.removeutm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSourceType;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

//DIRECT-51290
@Aqua.Test
@Description("Редактирование чекбокса удаления меток у фида")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class FeedsCheckBoxReSavedFeedTest extends FeedsCheckBoxBaseTest {

    private final static String CLIENT = "cli-smart-feed-2";

    @Parameterized.Parameter(value = 0)
    public String deleteLabelsCheckBox;

    @Parameterized.Parameter(value = 1)
    public FeedSourceType source;

    @Parameterized.Parameter(value = 2)
    public String urlFeed;

    @Parameterized.Parameter(value = 3)
    public String fileFeed;

    @Parameterized.Parameter(value = 4)
    public String changingCheckBox;

    @Parameterized.Parameter(value = 5)
    public String expChangedCheckBox;

    @Parameterized.Parameters(name = "Редактируем чекбокс удаления меток c {0} на {4} у фида по ссылке {2} или из файла {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"1", FeedSourceType.URL, FEED_URL, null, "0", "0"},
                {"1", FeedSourceType.URL, FEED_URL, null, null, "0"},
                {"1", FeedSourceType.URL, FEED_URL, null, "1", "1"},
                {"0", FeedSourceType.URL, FEED_URL, null, "1", "1"},
                {null, FeedSourceType.URL, FEED_URL, null, "1", "1"},
                {null, FeedSourceType.URL, FEED_URL, null, "0", "0"},
                {"1", FeedSourceType.FILE, null, FEED_FILE, "0", "0"},
                {"0", FeedSourceType.FILE, null, FEED_FILE, "1", "1"},
                {null, FeedSourceType.FILE, null, FEED_FILE, "0", "0"},
        });
    }

    @Before
    public void before() {
        super.before();
        createFeed();
    }

    @Test
    @Description("Редактируем чекбокс удаления меток фида")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9746")
    public void changeCheckBoxForFeed() {
        feedSaveRequest.setFeedId(savedFeed.getFeedId());
        feedSaveRequest.withRemoveUtm(changingCheckBox);
        cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, RedirectResponse.class);
        ajaxFeedsResponse = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(CLIENT);
        Feed reSavedFeed = ajaxFeedsResponse.getFeedsResult().getFeeds()
                .stream().max(Comparator.comparing(Feed::getLastChange)).orElse(null);

        assertThat("чекбокс удаления меток выставлен правильно",
                reSavedFeed.getIsRemoveUtm(), equalTo(expChangedCheckBox));
    }

    FeedSourceType getSource() {
        return source;
    }

    String getFeedUrl() {
        return urlFeed;
    }

    String getFeedFile() {
        return fileFeed;
    }

    String getCheckBox() {
        return deleteLabelsCheckBox;
    }

    String getClient() {
        return CLIENT;
    }
}

package ru.yandex.autotests.direct.cmd.feeds.removeutm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.feeds.Feed;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSourceType;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedStatusEnum;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

//DIRECT-51290
@Aqua.Test
@Description("Статус валидации фида после изменения чекбокса")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class FeedsCheckBoxValidateStatusTest extends FeedsCheckBoxBaseTest {

    private final static String CLIENT = "cli-smart-feed-3";

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
    public FeedStatusEnum[] expValidStatuses;

    @Parameterized.Parameters(name = "Меняем чекбокс удаления меток c {0} на {4} у фида по ссылке {2} или из файла {3}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"1", FeedSourceType.URL, FEED_URL, null, "0",
                        new FeedStatusEnum[]{FeedStatusEnum.NEW, FeedStatusEnum.UPDATING}},

                {"1", FeedSourceType.URL, FEED_URL, null, null,
                        new FeedStatusEnum[]{FeedStatusEnum.NEW, FeedStatusEnum.UPDATING}},

                {"1", FeedSourceType.URL, FEED_URL, null, "1",
                        new FeedStatusEnum[]{FeedStatusEnum.DONE}},

                {"0", FeedSourceType.URL, FEED_URL, null, "1",
                        new FeedStatusEnum[]{FeedStatusEnum.NEW, FeedStatusEnum.UPDATING}},

                {null, FeedSourceType.URL, FEED_URL, null, "1",
                        new FeedStatusEnum[]{FeedStatusEnum.NEW, FeedStatusEnum.UPDATING}},

                {null, FeedSourceType.URL, FEED_URL, null, "0",
                        new FeedStatusEnum[]{FeedStatusEnum.DONE}},

                {"1", FeedSourceType.FILE, null, FEED_FILE, "0",
                        new FeedStatusEnum[]{FeedStatusEnum.NEW, FeedStatusEnum.UPDATING}},

                {"0", FeedSourceType.FILE, null, FEED_FILE, "1",
                        new FeedStatusEnum[]{FeedStatusEnum.NEW, FeedStatusEnum.UPDATING}},

                {null, FeedSourceType.FILE, null, FEED_FILE, "1",
                        new FeedStatusEnum[]{FeedStatusEnum.NEW, FeedStatusEnum.UPDATING}},

                {null, FeedSourceType.FILE, null, FEED_FILE, "0",
                        new FeedStatusEnum[]{FeedStatusEnum.DONE}},
        });
    }

    @Before
    public void before() {
        super.before();
        createFeed();
    }

    @Test
    @Description("При изменении значения чекбокса фид должен изменять статус на новый и уходить в BL на валидацию")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9747")
    public void checkBoxValidateStatus() {
        PerformanceCampaignHelper.waitForFeedLoad(cmdRule.cmdSteps(), User.get(CLIENT).getClientID(),
                savedFeed.getFeedId());
        ajaxFeedsResponse = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(CLIENT);
        savedFeed = ajaxFeedsResponse.getFeedsResult().getFeeds()
                .stream().max(Comparator.comparing(Feed::getLastChange)).orElse(null);
        assumeThat("фид провалидирован в BannerLand", savedFeed.getUpdateStatus(), equalTo("Done"));

        feedSaveRequest.setFeedId(savedFeed.getFeedId());
        feedSaveRequest.withRemoveUtm(changingCheckBox);
        cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, RedirectResponse.class);
        ajaxFeedsResponse = cmdRule.cmdSteps().ajaxGetFeedsSteps().getFeeds(CLIENT);
        Feed reSavedFeed = ajaxFeedsResponse.getFeedsResult().getFeeds()
                .stream().max(Comparator.comparing(Feed::getLastChange)).orElse(null);

        String expectedStatusesStr = Stream.of(expValidStatuses)
                .map(FeedStatusEnum::toString)
                .collect(joining(", ", "[", "]"));

        boolean isValid = Stream.of(expValidStatuses)
                .anyMatch(expValidStatus -> expValidStatus.toString().equals(reSavedFeed.getUpdateStatus()));

        assertThat("ожидаемый статус валидации фида: " + expectedStatusesStr +
                        ", полученный: " + reSavedFeed.getUpdateStatus(),
                isValid,
                is(true));
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

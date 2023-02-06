package ru.yandex.autotests.direct.cmd.feeds.delete;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxFeedsResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.both;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка удаления фидов контроллером ajaxDeleteFeeds")
@Stories(TestFeatures.Feeds.AJAX_DELETE_FEEDS)
@Features(TestFeatures.FEEDS)
@RunWith(Parameterized.class)
@Tag(CmdTag.AJAX_DELETE_FEEDS)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class FeedsAtAjaxDeleteFeedsTest extends FeedsAtAjaxDeleteFeedsBaseTest {

    @Parameterized.Parameter(value = 0)
    public FeedsUpdateStatus feedStatus;

    @Parameterized.Parameters(name = "Удаление фида в статусе {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {FeedsUpdateStatus.Done},
                {FeedsUpdateStatus.Error}
        });
    }

    @Test
    @Description("Проверяем удаление фидов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9737")
    public void checkDeleteFeed() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .updateFeedsStatus(feedStatus, User.get(CLIENT).getClientID(), feedId, secondFeedId);
        cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeed(deleteFeedsRequest);

        AjaxFeedsResponse ajaxFeedsResponse = cmdRule.cmdSteps().ajaxGetFeedsSteps()
                .getFeeds(CLIENT);

        List<Long> actualFeedsIds = ajaxFeedsResponse.getFeedsResult().getFeeds().stream()
                .map(t -> Long.valueOf(t.getFeedId())).collect(Collectors.toList());
        assertThat("Идентификаторы фидов отсутствуют в списке фидов",
                actualFeedsIds,
                both(Matchers.not(hasItems(feedId)))
                        .and(Matchers.not(hasItems(secondFeedId))));
        feedId = null;
        secondFeedId = null;
    }
}

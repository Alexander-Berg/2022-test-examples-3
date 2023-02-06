package ru.yandex.autotests.direct.cmd.feeds.delete;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxDeleteFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedsErrors;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
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
import java.util.HashMap;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка ошибок удаления фидов контроллером ajaxDeleteFeeds")
@Stories(TestFeatures.Feeds.AJAX_DELETE_FEEDS)
@Features(TestFeatures.FEEDS)
@RunWith(Parameterized.class)
@Tag(CmdTag.AJAX_DELETE_FEEDS)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
public class FeedsAtAjaxDeleteFeedsShowsNegativeTest extends FeedsAtAjaxDeleteFeedsBaseTest {

    protected final static String DELETE_FEEDS_ERROR = FeedsErrors.FEEDS_DELETE_USED_ERROR.toString();

    @Parameterized.Parameter(value = 0)
    public FeedsUpdateStatus feedStatus;

    @Parameterized.Parameters(name = "Удаление фида в статусе {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {FeedsUpdateStatus.New},
                {FeedsUpdateStatus.Outdated},
                {FeedsUpdateStatus.Updating}
        });
    }

    @Test
    @Description("Проверяем невозможность удаления фидов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9736")
    public void checkInvalidDeleteFeed() {
        HashMap<Long, String> expectedHashMap = new HashMap<>();
        expectedHashMap.put(feedId, DELETE_FEEDS_ERROR);
        expectedHashMap.put(secondFeedId, DELETE_FEEDS_ERROR);
        expectedBean = new AjaxDeleteFeedsResponse().withMessage(expectedHashMap);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .updateFeedsStatus(feedStatus, User.get(CLIENT).getClientID(), feedId, secondFeedId);
        AjaxDeleteFeedsResponse actualBean = cmdRule.cmdSteps().ajaxDeleteFeedsSteps().deleteFeed(deleteFeedsRequest);

        assertThat("получена ошибка при удалении", actualBean, beanDiffer(expectedBean));
    }
}

package ru.yandex.autotests.direct.cmd.feeds;

import java.io.File;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxGetFeedHistoryResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedBusinessType;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSourceType;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.FileUtils;
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper.generateUniqueFeedStringPath;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка истории фида контроллером ajaxGetFeedHistory")
@Stories(TestFeatures.Feeds.AJAX_GET_FEED_HISTORY)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.AJAX_GET_FEED_HISTORY)
@Tag(ObjectTag.FEED)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class FeedHistoryTest {

    private static final String CLIENT = "at-direct-back-feedhistory";
    private static final String FEED_FILE = generateUniqueFeedStringPath("feeds/feed_invalid.xml");
    private static final String FEED_NAME = "feed with error";
    private static final String XML_PARSER_ERROR_CODE = "1204";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private String feedId;

    @Before
    public void before() {
        FeedHelper.deleteAllFeeds(cmdRule, CLIENT);
        saveFeedWithInvalidData();
        feedId = String.valueOf(TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .getFeeds(Long.valueOf(User.get(CLIENT).getClientID()))
                .stream().max(Comparator.comparing(FeedsRecord::getLastchange))
                .get().getFeedId());
        PerformanceCampaignHelper.waitForFeedLoad(cmdRule.cmdSteps(), User.get(CLIENT).getClientID(), feedId);
        PerformanceCampaignHelper.waitForFeedLoad(cmdRule.cmdSteps(), User.get(CLIENT).getClientID(), feedId);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9735")
    public void checkFeedHistory() {
        AjaxGetFeedHistoryResponse actualResponse = cmdRule.cmdSteps().ajaxGetFeedHistorySteps().getFeedHistory(feedId, CLIENT);
        assumeThat("есть история", actualResponse.getResult().getFeedHistory(), hasSize(greaterThan(0)));
        assumeThat("есть ошибки", actualResponse.getResult().getFeedHistory().get(0).getParseResults().getErrors(),
                hasSize(greaterThan(0)));

        assertThat("категории соответствуют ожидаемым", actualResponse.getResult()
                .getFeedHistory().get(0).getParseResults()
                .getErrors().get(0).getCode(), equalTo(XML_PARSER_ERROR_CODE));
    }

    private void saveFeedWithInvalidData() {
        RedirectResponse response = cmdRule.cmdSteps().saveFeedsSteps()
                .saveFeed(new FeedSaveRequest()
                        .withName(FEED_NAME)
                        .withBusinessType(FeedBusinessType.RETAIL)
                        .withSource(FeedSourceType.FILE.getValue())
                        .withFeedFile(FileUtils.getFilePath(FEED_FILE))
                        .withUlogin(CLIENT), RedirectResponse.class);
        assumeThat("фид добавился", response.getLocationParam(LocationParam.CMD), equalTo(CMD.SHOW_FEEDS.toString()));
    }

    @After
    public void after() {
        if (FEED_FILE != null) {
            File file = new File(FEED_FILE);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}

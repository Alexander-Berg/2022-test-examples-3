package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedBusinessType;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedsErrors;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка сохранения невалидного типа бизнеса фида контроллером SaveFeed")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@Tag("TESTIRT-9903")
public class FeedBusinessTypeAtSaveFeedValidationTest {

    private final static String CLIENT = "at-direct-back-feeds-save-c3";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private FeedSaveRequest feedSaveRequest;

    @Before
    public void before() {
        feedSaveRequest = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_FEED_DEFAULT, FeedSaveRequest.class)
                .withBusinessType(FeedBusinessType.WRONG_TYPE)
                .withUlogin(CLIENT);
    }

    @Test
    @Description("Проверяем создание фида с невалидным типом бизнеса")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10616")
    public void checkSaveFeedWrongBusinessType() {
        ErrorResponse actualResponse = cmdRule.cmdSteps().saveFeedsSteps().saveFeed(feedSaveRequest, ErrorResponse.class);

        assertThat("описание фида соответствует созданному",
                actualResponse.getError(),
                equalTo(FeedsErrors.WRONG_BUSINESS_TYPE.toString()));
    }

}

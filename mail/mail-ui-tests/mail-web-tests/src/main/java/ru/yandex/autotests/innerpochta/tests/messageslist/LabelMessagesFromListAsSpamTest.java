package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.ProxyParamsCheckFilter.proxyParamsCheckFilter;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.HANDLER_DO_MESSAGES;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_ACTION;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_ACTION_TO_SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_IDS;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_TIDS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 01.10.12
 * Time: 16:23
 */
@Aqua.Test
@Title("Тест на кнопку “Спам“ для списка писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class LabelMessagesFromListAsSpamTest extends BaseTest {

    private static final String THREAD_SUBJ = "thread";
    private static final int THREAD_SIZE = 2;

    private Message msg;
    private Message thread;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @ClassRule
    public static ProxyServerRule serverProxyRule = proxyServerRule(proxyParamsCheckFilter(HANDLER_DO_MESSAGES));

    @Override
    public DesiredCapabilities setCapabilities() {
        return serverProxyRule.getCapabilities();
    }

    @Before
    public void logIn() throws IOException {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-vertical",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), "");
        thread = user.apiMessagesSteps().sendThread(lock.firstAcc(), THREAD_SUBJ, THREAD_SIZE);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Помечаем как спам одно сообщение из инбокса")
    @TestCaseId("1523")
    public void testSpamButtonForOneMessageFromInbox() {
        user.messagesSteps().selectMessageWithSubject(msg.getSubject());
        user.defaultSteps().clicksOn(onMessagePage().toolbar().spamButton())
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES),
                of(
                    MESSAGES_PARAM_TIDS, msg.getTid(),
                    MESSAGES_PARAM_ACTION, MESSAGES_PARAM_ACTION_TO_SPAM
                )
            );
        user.defaultSteps().shouldSeeElementsCount(onMessagePage().displayedMessages().list(), 1);
    }

    @Test
    @Title("Помечаем как спам одно сообщение в треде из инбокса")
    @TestCaseId("1526")
    public void testSpamButtonForOneMessageInThreadFromInbox() {
        user.messagesSteps().expandsMessagesThread(THREAD_SUBJ)
            .selectMessagesInThreadCheckBoxWithNumber(0);
        String threadMsgMid = user.messagesSteps().getMidFromThreadByIndex(0);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().spamButton())
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES),
                of(
                    MESSAGES_PARAM_IDS, threadMsgMid,
                    MESSAGES_PARAM_ACTION, MESSAGES_PARAM_ACTION_TO_SPAM
                )
            );
        user.defaultSteps().shouldSeeElementsCount(onMessagePage().displayedMessages().list(), 4);
    }

    @Test
    @Title("Помечаем как спам одно сообщение и тред из инбокса")
    @TestCaseId("1528")
    public void testSpamButtonForMessageAndThreadFromInbox() {
        user.messagesSteps().selectMessageWithSubject(msg.getSubject())
            .selectMessageWithSubject(THREAD_SUBJ);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().spamButton())
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES),
                of(MESSAGES_PARAM_TIDS, msg.getTid())
            )
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES),
                of(
                    MESSAGES_PARAM_TIDS, thread.getTid(),
                    MESSAGES_PARAM_ACTION, MESSAGES_PARAM_ACTION_TO_SPAM
                )
            );
        user.messagesSteps().shouldNotSeeMessagesPresent();
    }
}

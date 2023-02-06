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
import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.parseParams;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.ProxyParamsCheckFilter.proxyParamsCheckFilter;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.HANDLER_DO_MESSAGES;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_IDS;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_TIDS;

/**
 * @author arttimofeev
 */
@Aqua.Test
@Title("Тест кнопки “Прочитано/Непрочитано“ для списка писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class ReadMessagesFromListTest extends BaseTest {

    private static final int MSG_COUNT = 3;
    private static final int THREAD_COUNT = 2;
    private static final String THREAD = "thread";

    private int unreadCounter;

    private Message msg1;
    private Message msg2;
    private Message threadMsg1;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);


    @ClassRule
    public static ProxyServerRule serverProxyRule = proxyServerRule(proxyParamsCheckFilter(HANDLER_DO_MESSAGES));

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Override
    public DesiredCapabilities setCapabilities() {
        return serverProxyRule.getCapabilities();
    }

    @Before
    public void logIn() throws IOException {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MSG_COUNT);
        threadMsg1 = user.apiMessagesSteps().sendThread(lock.firstAcc(), THREAD, THREAD_COUNT);
        List<Message> messages = user.apiMessagesSteps().getAllMessages();
        msg1 = messages.get(2);
        msg2 = messages.get(3);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(THREAD, msg1.getSubject(), msg2.getSubject());
        unreadCounter = user.leftColumnSteps().unreadCounter();
    }

    @Test
    @Title("Пометить прочитанными несколько сообщений.")
    @TestCaseId("1560")
    public void shouldMarkAsReadBtnMultipleMessages() {
        user.messagesSteps().selectMessageWithSubject(msg1.getSubject(), msg2.getSubject());
        user.defaultSteps().clicksOn(onMessagePage().toolbar().markAsReadButton());
        user.leftColumnSteps().shouldSeeUnreadCounterIs(unreadCounter - 2);
        user.defaultSteps().shouldBeParamsInRequest(
            parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_IDS, msg1.getMid())
        )
            .shouldBeParamsInRequest(
                parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_IDS, msg2.getMid())
            );
    }

    @Test
    @Title("Пометить сообщение и тред прочитанными.")
    @TestCaseId("1564")
    public void shouldMarkAsReadBtnMessageAndThread() {
        user.messagesSteps().selectMessageWithSubject(msg2.getSubject(), THREAD);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().markAsReadButton());
        user.leftColumnSteps().shouldSeeUnreadCounterIs(unreadCounter - 3);
        user.defaultSteps().shouldBeParamsInRequest(
            parseParams(HANDLER_DO_MESSAGES),
            of(
                MESSAGES_PARAM_TIDS, threadMsg1.getTid(),
                MESSAGES_PARAM_IDS, msg2.getMid()
            )
        );
    }

    @Test
    @Title("Прыщ: пометить 1 письмо прочитанным/непрочитанным.")
    @TestCaseId("1031")
    public void shouldMarkAsReadMessage() {
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(1).messageUnread());
        user.leftColumnSteps().shouldSeeUnreadCounterIs(unreadCounter - 1);
        user.defaultSteps().shouldBeParamsInRequest(
            parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_IDS, msg1.getMid())
        )
            .clicksOn(onMessagePage().displayedMessages().list().get(1).messageRead());
        user.leftColumnSteps().shouldSeeUnreadCounterIs(unreadCounter);
    }

    @Test
    @Title("Прыщ: пометить письмо в треде прочитанным/непрочитанным.")
    @TestCaseId("1032")
    public void shouldMarkAsReadMessageInThread() {
        user.messagesSteps().expandsMessagesThread(THREAD);
        String threadMsgMid = user.messagesSteps().getMidFromThreadByIndex(0);
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().messagesInThread().get(0).messageUnread());
        user.leftColumnSteps().shouldSeeUnreadCounterIs(unreadCounter - 1);
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages().list().get(0).messageUnread())
            .clicksOn(onMessagePage().displayedMessages().messagesInThread().get(0).messageRead());
        user.leftColumnSteps().shouldSeeUnreadCounterIs(unreadCounter);
        user.defaultSteps().shouldBeParamsInRequest(
            parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_IDS, threadMsgMid)
        );
    }

    @Test
    @Title("Прыщ: пометить тред прочитанным/непрочитанным.")
    @TestCaseId("1033")
    public void shouldMarkAsReadThread() {
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(0).messageUnread());
        user.leftColumnSteps().shouldSeeUnreadCounterIs(unreadCounter - 2);
        user.defaultSteps().shouldBeParamsInRequest(
            parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, threadMsg1.getTid())
        )
            .clicksOn(onMessagePage().displayedMessages().list().get(0).messageRead());
        user.leftColumnSteps().shouldSeeUnreadCounterIs(unreadCounter);
    }
}

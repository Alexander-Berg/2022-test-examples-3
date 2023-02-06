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
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.ProxyParamsCheckFilter.proxyParamsCheckFilter;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.HANDLER_DO_MESSAGES;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_IDS;
import static ru.yandex.autotests.innerpochta.util.handlers.MessagesConstants.MESSAGES_PARAM_TIDS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;

@Aqua.Test
@Title("Тест на удаление писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class ToolbarDeleteButtonMessagesFromListTest extends BaseTest {

    private static final String SUBJ_THREAD = "thread";
    private static final int MESSAGE_COUNT = 3;
    private static final int THREAD_COUNT = 2;
    private static final String AMOUNT_MESAAGES_IN_PAGE = "3";

    private List<Message> messages;
    private Message msg1;
    private Message msg2;
    private Message msg3;
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
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MESSAGE_COUNT);
        messages = user.apiMessagesSteps().getAllMessages();
        msg1 = messages.get(0);
        msg2 = messages.get(1);
        msg3 = messages.get(2);
        threadMsg1 = user.apiMessagesSteps().sendThread(lock.firstAcc(), SUBJ_THREAD, THREAD_COUNT);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем настройку показа 3х писем на странице",
            of(SETTINGS_PARAM_MESSAGES_PER_PAGE, AMOUNT_MESAAGES_IN_PAGE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Удаление одного письма из инбокса")
    @TestCaseId("1594")
    public void testDeleteButtonForMessageFromInbox() {
        user.messagesSteps().selectMessageWithSubject(msg1.getSubject());
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton())
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, msg1.getTid())
            );
        user.messagesSteps().shouldNotSeeMessageWithSubject(msg1.getSubject());
        user.leftColumnSteps().opensTrashFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(msg1.getSubject());
    }

    @Test
    @Title("Удаляем одно письмо в треде из инбокса")
    @TestCaseId("1598")
    public void testDeleteMultipleMessagesInThreadFromInbox() {
        user.messagesSteps().expandsMessagesThread(SUBJ_THREAD)
            .selectMessagesInThreadCheckBoxWithNumber(0);
        String threadMsgMid = user.messagesSteps().getMidFromThreadByIndex(0);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton())
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_IDS, threadMsgMid)
            );
    }

    @Test
    @Title("Удаляем одно письмо и целый тред из инбокса")
    @TestCaseId("1599")
    public void testDeleteMessageAndThreadFromInbox() {
        user.messagesSteps().clicksOnMultipleMessagesCheckBox(msg2.getSubject(), SUBJ_THREAD);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton())
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, threadMsg1.getTid())
            )
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, msg2.getTid())
            );
    }

    @Test
    @Title("Удаление всех писем отображаемых на странице")
    @TestCaseId("1601")
    public void testDeleteAllMessagesFromInbox() {
        user.messagesSteps().loadsMoreMessages()
            .selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton())
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, msg1.getTid())
            )
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, msg2.getTid())
            )
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, msg3.getTid())
            )
            .shouldBeParamsInRequest(
                serverProxyRule.parseParams(HANDLER_DO_MESSAGES), of(MESSAGES_PARAM_TIDS, threadMsg1.getTid())
            );
    }
}

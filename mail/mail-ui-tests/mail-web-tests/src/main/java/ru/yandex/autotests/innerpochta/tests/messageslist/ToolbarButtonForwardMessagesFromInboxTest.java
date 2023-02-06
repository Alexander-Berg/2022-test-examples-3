package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.FORWARD_PREFIX;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;


@Aqua.Test
@Title("Тест на кнопку “Переслать письмо“")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class ToolbarButtonForwardMessagesFromInboxTest extends BaseTest {

    private static final String SUBJ_THREAD = "thread";
    private static final String AMOUNT_MESAAGES_IN_PAGE = "3";
    private static final int MESSAGE_COUNT = 4;
    private static final int THREAD_COUNT = 2;

    private Message msg1;
    private Message msg2;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));


    @Before
    public void logIn() {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MESSAGE_COUNT);
        user.apiMessagesSteps().sendThread(lock.firstAcc(), SUBJ_THREAD, THREAD_COUNT);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 2pane и показ 3-х писем на странице",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                SETTINGS_PARAM_MESSAGES_PER_PAGE, AMOUNT_MESAAGES_IN_PAGE
            )
        );
        List<Message> messages = user.apiMessagesSteps().getAllMessages();
        msg1 = messages.get(3);
        msg2 = messages.get(4);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(SUBJ_THREAD, msg1.getSubject(), msg2.getSubject());
    }

    @Test
    @Title("Пересылка одного из папки «Входящие»")
    @TestCaseId("1584")
    public void testForwardButtonForMessageFromInboxFolder() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(msg1.getSubject());
        user.defaultSteps().clicksOn(onMessagePage().toolbar().forwardButton());
        user.composeSteps().clicksOnAddEmlBtn()
            .shouldSeeSubject(FORWARD_PREFIX + msg1.getSubject())
            .shouldSeeMessageAsAttachment(0, msg1.getSubject())
            .shouldSeeTextAreaContains(msg1.getFirstline());
    }

    @Test
    @Title("Пересылка одного письма из треда из папки «Входящие»")
    @TestCaseId("1587")
    public void testForwardOneMessageInThreadFromInbox() {
        user.messagesSteps().expandsMessagesThread(SUBJ_THREAD)
            .selectMessagesInThreadCheckBoxWithNumber(0);
        String threadMsg = onMessagePage().displayedMessages().messagesInThread().get(0).firstLine().getText();
        user.defaultSteps().clicksOn(onMessagePage().toolbar().forwardButton())
            .shouldSee(onComposePopup().expandedPopup().composeAddEmlBtn())
            .shouldNotSee(onComposePopup().expandedPopup().attachPanel());
        user.composeSteps().shouldSeeSubject(FORWARD_PREFIX + SUBJ_THREAD)
            .shouldSeeTextAreaContains(threadMsg);
    }

    @Test
    @Title("Пересылаем одно письмо и тред из папки «Входящие»")
    @TestCaseId("1589")
    public void testForwardMessageAndThreadFromInbox() {
        user.messagesSteps().clicksOnMultipleMessagesCheckBox(msg1.getSubject(), SUBJ_THREAD);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().forwardButton());
        user.composeSteps().shouldSeeSubject(FORWARD_PREFIX)
            .shouldSeeEmptyTextArea()
            .shouldSeeMessageAsAttachment(0, msg1.getSubject())
            .shouldSeeMessageAsAttachment(1, SUBJ_THREAD)
            .shouldSeeMessageAsAttachment(2, SUBJ_THREAD);
    }

    @Test
    @Title("Пересылка письма со второй страницы")
    @TestCaseId("1593")
    public void testForwardMessageInSecondPageFromInbox() {
        user.defaultSteps().shouldSee(onMessagePage().loadMoreMessagesButton());
        user.messagesSteps().loadsMoreMessages()
            .clicksOnMessageCheckBoxWithSubject(msg2.getSubject());
        user.defaultSteps().clicksOn(onMessagePage().toolbar().forwardButton());
        user.composeSteps().clicksOnAddEmlBtn()
            .shouldSeeSubject(FORWARD_PREFIX + msg2.getSubject())
            .shouldSeeTextAreaContains(msg2.getFirstline())
            .shouldSeeMessageAsAttachment(0, msg2.getSubject());
    }
}

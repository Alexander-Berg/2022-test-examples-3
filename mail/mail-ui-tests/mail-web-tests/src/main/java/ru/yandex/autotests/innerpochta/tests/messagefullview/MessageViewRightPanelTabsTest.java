package ru.yandex.autotests.innerpochta.tests.messagefullview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тест на вкладки в правой колонке")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.RIGHT_PANEL)
public class MessageViewRightPanelTabsTest extends BaseTest {

    private static final String SUBJECT = Utils.getRandomName();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Message msg;
    private Message threadMsg;
    private String draft_body;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        draft_body = Utils.getRandomString();
        threadMsg = user.apiMessagesSteps().sendThread(lock.firstAcc(), Utils.getRandomName(), 2);
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), SUBJECT, "");
        user.apiMessagesSteps().prepareDraftToThread("", SUBJECT, draft_body);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), SUBJECT + SUBJECT, "");
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, EMPTY_STR)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(SUBJECT, threadMsg.getSubject(), SUBJECT + SUBJECT);
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
    }

    @Test
    @Title("Вкладка «Письма на тему» по умолчанию развернута")
    @TestCaseId("2184")
    public void shouldSeeDefaultOpenTab() {
        user.defaultSteps().shouldSee(onMessageView().messageViewSideBar().messagesBySubjList());
    }

    @Test
    @Title("Запоминание развернутой вкладки")
    @TestCaseId("2185")
    public void shouldRememberCustomOpenTab() {
        user.defaultSteps().clicksOn(onMessageView().messageViewSideBar().messagesBySenderLink())
            .shouldSee(onMessageView().messageViewSideBar().messagesBySenderList())
            .shouldNotSee(onMessageView().messageViewSideBar().messagesBySubjList())
            .clicksOn(onMessageView().messageViewSideBar().prevBtn());
        user.messageViewSteps().shouldSeeMessageSubject(SUBJECT + SUBJECT);
        user.defaultSteps().shouldSee(onMessageView().messageViewSideBar().messagesBySenderList());
    }

    @Test
    @Title("Не запоминаем вкладку после выхода в инбокс")
    @TestCaseId("2186")
    public void shouldNotRememberTabAfterExit() {
        user.defaultSteps().clicksOn(onMessageView().messageViewSideBar().messagesBySenderLink())
            .shouldSee(onMessageView().messageViewSideBar().messagesBySenderList())
            .shouldNotSee(onMessageView().messageViewSideBar().messagesBySubjList())
            .opensFragment(INBOX)
            .opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .shouldSee(onMessageView().messageViewSideBar().messagesBySubjList())
            .shouldNotSee(onMessageView().messageViewSideBar().messagesBySenderList());
    }

    @Test
    @Title("Открытие черновика из вкладки")
    @TestCaseId("2188")
    public void shouldOpenDraftInTab() {
        user.defaultSteps().shouldSee(onMessageView().messageViewSideBar().messagesBySubjList())
            .clicksOn(onMessageView().messageViewSideBar().messagesBySubjList().relatedMsgList().get(0));
        user.messageViewSteps().shouldSeeMessageSubject(SUBJECT)
            .shouldSeeCorrectMessageText(draft_body);
        user.defaultSteps().clicksOn(onMessageView().toolbar().editDraftBtn())
            .shouldSee(onComposePopup().expandedPopup());
    }

    @Test
    @Title("В правой колонке нет удаленных")
    @TestCaseId("2285")
    public void shouldNotSeeDeletedMessages() {
        Message msg2 = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.defaultSteps().opensFragment(INBOX);
        user.messagesSteps().selectMessageWithSubject(SUBJECT + SUBJECT)
            .movesMessageToFolder(TRASH_RU)
            .shouldNotSeeMessageWithSubject(SUBJECT + SUBJECT)
            .selectMessageWithSubject(msg2.getSubject())
            .movesMessageToFolder(SPAM_RU)
            .shouldNotSeeMessageWithSubject(msg2.getSubject());
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessageView().messageViewSideBar().messagesBySenderLink())
            .shouldNotSeeElementInList(
                onMessageView().messageViewSideBar().messagesBySenderList().relatedMsgList(),
                msg2.getSubject()
            )
            .shouldNotSeeElementInList(
                onMessageView().messageViewSideBar().messagesBySenderList().relatedMsgList(),
                SUBJECT + SUBJECT
            );
    }

    @Test
    @Title("Переход на другое письмо во вкладке «Письма на тему»")
    @TestCaseId("2197")
    public void shouldOpenNextMessage() {
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(threadMsg.getMid()))
            .shouldSee(onMessageView().messageViewSideBar().messagesBySubjList())
            .clicksOn(onMessageView().messageViewSideBar().messagesBySubjList().relatedMsgList().get(1));
        user.messageViewSteps().shouldSeeMessageSubject(threadMsg.getSubject());
        user.defaultSteps().shouldSee(onMessageView().messageViewSideBar().messagesBySubjList());
        assertEquals(
            onMessageView().messageTextBlock().text().getText(),
            onMessageView().messageViewSideBar().messagesBySubjList().selectedMsgInList().getText()
        );
    }
}

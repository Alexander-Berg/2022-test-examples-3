package ru.yandex.autotests.innerpochta.tests.contextmenu;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

@Aqua.Test
@Title("Проверяем пункт “Спам/Не спам“ для писем/тредов")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextSpamTest extends BaseTest {

    private static final int TREAD_SIZE = 2;

    private String subject = getRandomName();
    private String template;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        template = user.apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        user.apiMessagesSteps().createDraftMessage();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Помечаем спамом/не спамом одиночное письмо")
    @TestCaseId("1264")
    public void shouldMarkMessageAsSpam() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, getRandomString());
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .rightClickOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markSpam())
            .opensFragment(QuickFragments.SPAM);
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .rightClickOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).unSpam())
            .clicksIfCanOn(onMessagePage().rightSubmitActionBtn());
        user.messagesSteps().shouldNotSeeContextMenu();
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Помечаем спамом все сообщения на странице")
    @TestCaseId("5322")
    public void shouldMarkAllMessageAsSpam() {
        String threadSbj = getRandomName();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, getRandomString());
        user.apiMessagesSteps().sendThread(lock.firstAcc(), threadSbj, TREAD_SIZE);
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeMessageWithSubject(subject, threadSbj)
            .selectsAllDisplayedMessagesInFolder()
            .rightClickOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markSpam());
        user.messagesSteps().shouldNotSeeMessagesPresent();
        user.defaultSteps().opensFragment(QuickFragments.SPAM);
        user.messagesSteps().shouldSeeMsgCount(TREAD_SIZE + 1);
    }

    @Test
    @Title("Помечаем спамом тред, не спамом одно письмо из треда")
    @TestCaseId("1265")
    public void shouldMarkThreadAsSpam() {
        user.apiMessagesSteps().sendThread(lock.firstAcc(), subject, TREAD_SIZE);
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .shouldSeeThreadCounter(subject, TREAD_SIZE)
            .rightClickOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).markSpam())
            .clicksIfCanOn(onMessagePage().leftSubmitActionBtn())
            .opensFragment(QuickFragments.SPAM);
        user.messagesSteps().shouldSeeMessageWithSubjectCount(subject, TREAD_SIZE)
            .shouldSeeCorrectNumberOfMessages(TREAD_SIZE);
        user.defaultSteps().rightClick(user.pages().MessagePage().displayedMessages().list().get(0).subject())
            .clicksOn(onMessagePage().allMenuListInMsgList().get(0).unSpam())
            .clicksIfCanOn(onMessagePage().rightSubmitActionBtn());
        user.messagesSteps().shouldNotSeeContextMenu()
            .shouldSeeCorrectNumberOfMessages(TREAD_SIZE - 1);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Отсутствие пункта спам/не спам в Черновиках")
    @TestCaseId("1266")
    public void shouldNotSeeSpamInDraftFolder() {
        user.defaultSteps().opensFragment(QuickFragments.DRAFT)
            .rightClick(user.pages().MessagePage().displayedMessages().list().get(0).subject())
            .shouldNotSee(
                onMessagePage().allMenuListInMsgList().get(0).markSpam(),
                onMessagePage().allMenuListInMsgList().get(0).unSpam()
            );
    }

    @Test
    @Title("Отсутствие пункта спам/не спам в Шаблонах")
    @TestCaseId("1267")
    public void shouldNotSeeSpamInTemplateFolder() {
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE);
        user.messagesSteps().shouldSeeMessageWithSubject(template);
        user.defaultSteps().rightClick(user.pages().MessagePage().displayedMessages().list().get(0).subject())
            .shouldNotSee(
                onMessagePage().allMenuListInMsgList().get(0).markSpam(),
                onMessagePage().allMenuListInMsgList().get(0).unSpam()
            );
    }

    @Test
    @Title("Отсутствие пункта спам/не спам в Отправленных")
    @TestCaseId("1268")
    public void shouldNotSeeSpamInSendFolder() {
        user.apiMessagesSteps().sendMail(lock.firstAcc(), subject, "");
        user.defaultSteps().opensFragment(QuickFragments.SENT);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
        user.defaultSteps().rightClick(user.pages().MessagePage().displayedMessages().list().get(0).subject())
            .shouldNotSee(
                onMessagePage().allMenuListInMsgList().get(0).markSpam(),
                onMessagePage().allMenuListInMsgList().get(0).unSpam()
            );
    }

    @Test
    @Title("Отсутствие пункта спам/не спам в Исходящие")
    @TestCaseId("1269")
    public void shouldNotSeeSpamInOutboxFolder() {
        user.apiMessagesSteps().sendMailWithSentTime(lock.firstAcc(), subject, "");
        user.defaultSteps().opensFragment(QuickFragments.OUTBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
        user.defaultSteps().rightClick(user.pages().MessagePage().displayedMessages().list().get(0).subject())
            .shouldNotSee(
                onMessagePage().allMenuListInMsgList().get(0).markSpam(),
                onMessagePage().allMenuListInMsgList().get(0).unSpam()
            );
    }
}

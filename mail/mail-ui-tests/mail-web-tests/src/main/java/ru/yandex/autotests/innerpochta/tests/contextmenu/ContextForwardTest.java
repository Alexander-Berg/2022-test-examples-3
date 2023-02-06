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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

@Aqua.Test
@Title("Проверяем пункт “Переслать“ для писем/тредов")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextForwardTest extends BaseTest {

    private static final int THREAD_COUNTER = 2;
    private String subject;
    private String template;
    private String text;
    private String thread_subject = getRandomName();

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        text = Utils.getRandomString();
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), text).getSubject();
        template = user.apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Пересылаем одно сообщение")
    @TestCaseId("1236")
    public void forwardMessage() {
        user.apiSettingsSteps().callWithListAndParams(
            "Отключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).forward())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        user.composeSteps().shouldSeeEmptySendFieldTo()
            .shouldSeeSubject("Fwd: " + subject)
            .clicksOnAddEmlBtn()
            .shouldSeeTextAreaContains(text)
            .shouldSeeMessageAsAttachment(0, subject);
    }

    @Test
    @Title("Пересылаем тред")
    @TestCaseId("1237")
    public void forwardThread() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, true)
        );
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subject, lock.firstAcc(), text);
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeThreadCounter(subject, THREAD_COUNTER)
            .rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).forward())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        user.composeSteps().shouldSeeEmptySendFieldTo()
            .shouldSeeSubject("Fwd: " + subject)
            .shouldSeeEmptyTextArea()
            .shouldSeeMessageAsAttachment(0, subject)
            .shouldSeeMessageAsAttachment(1, subject);
    }

    @Test
    @Title("Пересылаем несколько сообщений")
    @TestCaseId("1238")
    public void forwardSeveralMessages() {
        String secondSubject = user.apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), getRandomName(), text).getSubject();
        user.apiSettingsSteps().callWithListAndParams(
            "Отключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectMessageWithSubject(subject, secondSubject)
            .rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).forward())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        user.composeSteps().shouldSeeEmptySendFieldTo()
            .shouldSeeSubject("Fwd: ")
            .shouldSeeEmptyTextArea()
            .shouldSeeMessageAsAttachment(0, subject)
            .shouldSeeMessageAsAttachment(1, secondSubject);
    }

    @Test
    @Title("Пересылаем шаблон")
    @TestCaseId("1239")
    public void forwardTemplate() {
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE)
            .rightClick(user.pages().MessagePage().displayedMessages().list().get(0).subject());
        user.messagesSteps().shouldSeeContextMenuInMsgList();
        user.defaultSteps().shouldNotSee(onMessagePage().allMenuListInMsgList().get(0).forward());
    }

    @Test
    @Title("Пересылаем письмо из треда и одиночное")
    @TestCaseId("5278")
    public void shouldForwardSingleAndOneFromThread() {
        user.apiMessagesSteps().sendThread(lock.firstAcc(), thread_subject, 3);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().expandsMessagesThread(thread_subject)
            .selectMessagesInThreadCheckBoxWithNumber(1)
            .selectMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().forwardButton())
            .shouldSee(onComposePopup().expandedPopup());
        user.composeSteps().shouldSeeMessageAsAttachment(1, thread_subject)
            .shouldSeeMessageAsAttachment(0, subject);
    }
}
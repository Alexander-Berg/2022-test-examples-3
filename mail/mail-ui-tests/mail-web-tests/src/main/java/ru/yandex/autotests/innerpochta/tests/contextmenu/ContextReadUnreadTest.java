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

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Проверяем пункт “Прочитано/Не прочитано“ для писем/тредов")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextReadUnreadTest extends BaseTest {

    private String subject;
    private String template;

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
    public void login() {
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        template = user.apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Прочитано/Не прочитано одно сообщение")
    @TestCaseId("1261")
    public void readUnreadMessage() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
        .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).read());
        user.messagesSteps().shouldNotSeeContextMenu()
            .shouldSeeThatMessagesAreNotSelected()
            .selectMessageWithSubject(subject)
            .shouldSeeThatMessageIsRead()
            .rightClickOnMessageWithSubject(subject)
        .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).unread());
        user.messagesSteps().shouldNotSeeContextMenu()
            .shouldSeeThatMessagesAreNotSelected()
            .selectMessageWithSubject(subject)
            .shouldSeeThatMessageIsNotRead();
    }

    @Test
    @Title("Прочитано/Не прочитано несколько сообщений")
    @TestCaseId("1262")
    public void readUnreadSeveralMessage() {
        String secondSubject = user.apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.messagesSteps().shouldSeeMessageWithSubject(secondSubject)
            .selectMessageWithSubject(subject, secondSubject)
            .rightClickOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).read());
        user.messagesSteps().shouldNotSeeContextMenu()
            .shouldSeeThatMessagesAreNotSelected()
            .selectMessageWithSubject(subject)
            .shouldSeeThatMessageIsRead()
            .selectMessageWithSubject(secondSubject)
            .shouldSeeThatMessageIsRead()
            .rightClickOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).unread());
        user.messagesSteps().shouldSeeThatMessagesAreNotSelected()
            .selectMessageWithSubject(subject)
            .shouldSeeThatMessageIsNotRead()
            .deselectMessageCheckBoxWithSubject(subject)
            .selectMessageWithSubject(secondSubject)
            .shouldSeeThatMessageIsNotRead();
    }

    @Test
    @Title("Отсутствие Прочитано/Не прочитано в шаблоне")
    @TestCaseId("1263")
    public void readUnreadTemplate() {
        user.defaultSteps().opensFragment(QuickFragments.TEMPLATE)
            .rightClick(user.pages().MessagePage().displayedMessages().list().get(0).subject())
            .shouldNotSee(
                onMessagePage().allMenuListInMsgList().get(0).read(),
                onMessagePage().allMenuListInMsgList().get(0).unread()
            );
    }
}

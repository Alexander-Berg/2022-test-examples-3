package ru.yandex.autotests.innerpochta.tests.contextmenu;

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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_USER_NAME;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Пункт “Ответить“ в КМ")
@Features("ContextMenu")
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextReplyTest extends BaseTest {

    private final static String YANDEX_RU = "@yandex.ru";
    private final static String CC_ADDRESS = MailConst.DEV_NULL_EMAIL;
    private String subject, body;

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        user.apiSettingsSteps().callWithListAndParams("Меняем имя юзера", of(SETTINGS_USER_NAME, ""));
        subject = Utils.getRandomName();
        body = Utils.getRandomString();
        user.apiMessagesSteps().addCcEmails(CC_ADDRESS)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), subject, body);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Пункт “Ответить всем“ в КМ для одиночного письма открывает страницу ответа для письма")
    @TestCaseId("641")
    public void shouldOpenReplyAllPageForMessage() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).replyAll())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        shouldSeeToAreaContainsOnReplyPage(lock.firstAcc().getSelfEmail());
        shouldSeeSubjectContainsOnReplyPage(subject);
        user.composeSteps().revealQuotes()
            .shouldSeeCCAreaContains(CC_ADDRESS)
            .shouldSeeTextAreaContains(body);
    }

    @Test
    @Title("Пункт “Ответить“ в КМ для одиночного письма открывает страницу ответа для письма")
    @TestCaseId("6125")
    public void shouldOpenReplyPageForMessage() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).reply())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        shouldSeeToAreaContainsOnReplyPage(lock.firstAcc().getSelfEmail());
        shouldSeeSubjectContainsOnReplyPage(subject);
        user.composeSteps().revealQuotes()
            .shouldSeeTextAreaContains(body)
            .expandCcBcc();
        user.defaultSteps().shouldNotSee(user.pages().ComposePopup().yabbleCc().yabbleText());
    }

    @Test
    @Title("Пункт “Ответить всем“ в КМ для треда открывает страницу ответа для треда")
    @TestCaseId("6234")
    public void shouldOpenReplyAllPageForThread() {
        String threadBody = Utils.getRandomString();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, TRUE)
        );
        user.apiMessagesSteps().addCcEmails(CC_ADDRESS)
            .sendMessageToThreadWithCcAndBcc(lock.firstAcc().getSelfEmail(), subject, threadBody);

        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeThreadCounter(subject, 2)
            .rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).replyAll())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        shouldSeeToAreaContainsOnReplyPage(lock.firstAcc().getSelfEmail());
        shouldSeeSubjectContainsOnReplyPage(subject);
        user.composeSteps().shouldSeeCCAreaContains(CC_ADDRESS)
            .revealQuotes()
            .shouldSeeTextAreaContains(threadBody);
    }

    @Test
    @Title("Пункт “Ответить“ в КМ для треда открывает страницу ответа для треда")
    @TestCaseId("1109")
    public void shouldOpenReplyPageForThread() {
        String threadBody = Utils.getRandomString();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, TRUE)
        );
        user.apiMessagesSteps().addCcEmails(CC_ADDRESS)
            .sendMessageToThreadWithCcAndBcc(lock.firstAcc().getSelfEmail(), subject, threadBody);

        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeThreadCounter(subject, 2)
            .rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).reply())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        shouldSeeToAreaContainsOnReplyPage(lock.firstAcc().getSelfEmail());
        shouldSeeSubjectContainsOnReplyPage(subject);
        user.defaultSteps().shouldNotSee(user.pages().ComposePopup().expandedPopup().popupCc());
        user.composeSteps().revealQuotes()
            .shouldSeeTextAreaContains(threadBody);
    }

    @Step("Поле «Кому» должно содержать текст: {0} на странице ответа на письмо")
    private void shouldSeeToAreaContainsOnReplyPage(String address) {
        address = address.replace(YANDEX_RU, "");
        assertThat(
            "Неверный адрес в поле “Кому“ письма",
            onComposePopup().yabbleToList().get(0),
            hasText(containsString(address))
        );
    }

    @Step("Поле «Тема» должно содержать текст: {0} на странице ответа на письмо")
    private void shouldSeeSubjectContainsOnReplyPage(String subject) {
        subject = "Re: " + subject;
        assertThat(
            "Неверный текст в поле subject",
            onComposePopup().expandedPopup().sbjInput(),
            withWaitFor(hasValue(subject))
        );
    }
}

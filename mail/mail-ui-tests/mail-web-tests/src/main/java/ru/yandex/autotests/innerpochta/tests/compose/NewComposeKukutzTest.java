package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static jersey.repackaged.com.google.common.collect.ImmutableMap.of;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TEMPLATE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_HEAD_FULL_EDITION;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Kukutz")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
@RunWith(DataProviderRunner.class)
public class NewComposeKukutzTest extends BaseTest {
    private static final String CC_EMAIL = "testbot2@yandex.ru";
    private static final String CC_EMAIL_2 = "testbot3@yandex.ru";
    private static final String COM_DOMAIN = "@yandex.com";
    private static final String ALL_IN_KUKUTZ = "-%sвсе";
    private String msgSubject;
    private String msgTo;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] layouts() {
        return new Object[][]{
            {LAYOUT_2PANE},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Before
    public void setUp() {
        msgTo = lock.firstAcc().getSelfEmail();
        msgSubject = getRandomString();
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды, разворачиваем получателей письма",
            of(
                SETTINGS_FOLDER_THREAD_VIEW, FALSE,
                SETTINGS_HEAD_FULL_EDITION, TRUE
            )
        );
        user.apiMessagesSteps().addCcEmails(CC_EMAIL, CC_EMAIL_2)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), msgSubject, "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().clicksOnMessageWithSubject(msgSubject);
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyToAllButton())
            .shouldSee(onComposePopup().expandedPopup().bodyInput());
    }

    @Test
    @Title("Показываем кукутц при добавлении и удалении контактов")
    @TestCaseId("5820")
    public void shouldSeeKukutzInCompose() {
        addContactAndCheckKukutz();
        deleteContactAndCheckKukutz(1);
        sendMsgAndOpenHis();
        user.defaultSteps()
            .shouldSeeThatElementHasText(
                onMessageView().messageTextBlock(),
                String.format("+ %s@", substringBefore(DEV_NULL_EMAIL, "@"))
            )
            .shouldSeeThatElementHasText(
                onMessageView().messageTextBlock(),
                String.format("- %s@", substringBefore(CC_EMAIL_2, "@"))
            );
    }

    @Test
    @Title("Показываем кукутц при добавлении контактов")
    @TestCaseId("5830")
    public void shouldSeeKukutzAfterAddContact() {
        addContactAndCheckKukutz();
        sendMsgAndOpenHis();
        user.defaultSteps().shouldSeeThatElementHasText(
            onMessageView().messageTextBlock(),
            String.format("+ %s@", substringBefore(DEV_NULL_EMAIL, "@"))
        );
    }

    @Test
    @Title("Показываем кукутц при удалении контактов")
    @TestCaseId("5832")
    public void shouldSeeKukutzAfterDeleteContact() {
        deleteContactAndCheckKukutz(0);
        sendMsgAndOpenHis();
        user.defaultSteps().shouldSeeThatElementHasText(
            onMessageView().messageTextBlock(),
            String.format("- %s@", substringBefore(CC_EMAIL_2, "@"))
        );
    }

    @Test
    @Title("Показываем кукутц «-все» при удалении контактов")
    @TestCaseId("5833")
    public void shouldSeeKukutzAfterDeleteAllContacts() {
        user.defaultSteps().clicksOn(onComposePopup().yabbleCcList().get(1).yabbleDeleteBtn())
            .clicksOn(onComposePopup().yabbleCcList().get(0).yabbleDeleteBtn())
            .shouldSeeThatElementHasText(
                onComposePopup().composeKukutz().diffList().get(0),
                String.format(ALL_IN_KUKUTZ, "\n")
            );
        sendMsgAndOpenHis();
        user.defaultSteps()
            .shouldSeeThatElementHasText(onMessageView().messageTextBlock(), String.format(ALL_IN_KUKUTZ, " "));
    }

    @Test
    @Title("Показываем кукутц c доменом при одинаковых контактах")
    @TestCaseId("5824")
    public void shouldSeeKukutzWithDomain() {
        deleteContactAndCheckKukutz(0);
        user.defaultSteps().appendTextInElement(
            onComposePopup().expandedPopup().popupTo(),
            String.format("%s%s", substringBefore(CC_EMAIL_2, "@"), COM_DOMAIN)
        )
            .clicksOn(onComposePopup().expandedPopup().bodyInput())
            .shouldSeeThatElementHasText(
                onComposePopup().composeKukutz().diffList().waitUntil(not(empty())).get(0),
                String.format("+\n%s%s", substringBefore(CC_EMAIL_2, "@"), COM_DOMAIN)
            )
            .shouldSeeThatElementHasText(
                onComposePopup().composeKukutz().diffList().waitUntil(not(empty())).get(1),
                String.format("-\n%s", CC_EMAIL_2)
            );
        sendMsgAndOpenHis();
        user.defaultSteps().shouldSeeThatElementHasText(
            onMessageView().messageTextBlock(),
            String.format("+ %s%s", substringBefore(CC_EMAIL_2, "@"), COM_DOMAIN)
        )
            .shouldSeeThatElementHasText(
                onMessageView().messageTextBlock(),
                String.format("- %s", CC_EMAIL_2)
            );
    }

    @Test
    @Title("Не показываем кукутц после выключения")
    @TestCaseId("5821")
    public void shouldNotSeeKukutzAfterSwitchOff() {
        addContactAndCheckKukutz();
        user.defaultSteps().onMouseHoverAndClick(onComposePopup().composeKukutz().diffList().get(0))
            .shouldNotSee(onComposePopup().composeKukutz().addBtn())
            .onMouseHoverAndClick(onComposePopup().composeKukutz().dontShowBtn())
            .shouldNotSee(onComposePopup().composeKukutz().diffList())
            .shouldSee(onComposePopup().composeKukutz().addBtn());
        sendMsgAndOpenHis();
        user.defaultSteps().shouldNotHasText(
            onMessageView().messageTextBlock(),
            String.format("+ %s@", substringBefore(DEV_NULL_EMAIL, "@"))
        );
    }

    @Test
    @Title("Показываем кукутц после включения")
    @TestCaseId("5822")
    public void shouldSeeKukutzAfterSwitchOn() {
        addContactAndCheckKukutz();
        user.defaultSteps().onMouseHoverAndClick(onComposePopup().composeKukutz().dontShowBtn())
            .shouldNotSee(onComposePopup().composeKukutz().diffList())
            .clicksOn(onComposePopup().composeKukutz().addBtn())
            .shouldSee(onComposePopup().composeKukutz().diffList())
            .shouldNotSee(onComposePopup().composeKukutz().addBtn());
        sendMsgAndOpenHis();
        user.defaultSteps().shouldSeeThatElementHasText(
            onMessageView().messageTextBlock(),
            String.format("+ %s@", substringBefore(DEV_NULL_EMAIL, "@"))
        );
    }

    @Test
    @Title("Не показываем кукутц в новых письмах")
    @TestCaseId("5823")
    public void shouldNotSeeKukutzInNewMsg() {
        user.defaultSteps().clicksOn(
            onComposePopup().expandedPopup().closeBtn(),
            onMessagePage().composeButton()
        )
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject);
        addContactAndShouldNotSeeKukutz(DEV_NULL_EMAIL);
    }

    @Test
    @Title("Не показываем кукутц в черновиках")
    @TestCaseId("5823")
    public void shouldNotSeeKukutzInDraftMsg() {
        msgSubject = getRandomString();
        user.apiMessagesSteps().createDraftWithSubject(msgSubject);
        user.defaultSteps().opensFragment(DRAFT)
            .refreshPage();
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().firstMessageWithSubject(msgSubject).subject());
        addContactAndShouldNotSeeKukutz(CC_EMAIL);
    }

    @Test
    @Title("Не показываем кукутц в шаблонах")
    @TestCaseId("5823")
    public void shouldNotSeeKukutzInTemplates() {
        String sbj = user.apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        user.defaultSteps().opensFragment(TEMPLATE)
            .refreshPage()
            .clicksOn(onMessagePage().displayedMessages().firstMessageWithSubject(sbj).subject());
        addContactAndShouldNotSeeKukutz(CC_EMAIL);
    }

    @Step("Добавляем контакт и проверяем кукутц")
    private void addContactAndCheckKukutz() {
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().popupTo(), DEV_NULL_EMAIL)
            .clicksOn(onComposePopup().expandedPopup().bodyInput())
            .shouldSeeThatElementHasText(
                onComposePopup().composeKukutz().diffList().waitUntil(not(empty())).get(0),
                String.format("+\n%s@", substringBefore(DEV_NULL_EMAIL, "@"))
            );
    }

    @Step("Удаляем контакт и проверяем кукутц")
    private void deleteContactAndCheckKukutz(int position) {
        user.defaultSteps().clicksOn(onComposePopup().yabbleCcList().get(1).yabbleDeleteBtn())
            .shouldSeeThatElementHasText(
                onComposePopup().composeKukutz().diffList().waitUntil(not(empty())).get(position),
                String.format("-\n%s@", substringBefore(CC_EMAIL_2, "@"))
            );
    }

    @Step("Отправляем сообщение и открываем его на просмотр")
    private void sendMsgAndOpenHis() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .clicksOn(onComposePopup().doneScreenInboxLink());
        user.messagesSteps().clicksOnMessageWithSubject("Re: " + msgSubject);
    }

    @Step("Добавляем получателя и проверяем, что нет кукутца")
    private void addContactAndShouldNotSeeKukutz(String contact) {
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().popupTo(), contact)
            .clicksOn(onComposePopup().expandedPopup().bodyInput())
            .shouldNotSee(onComposePopup().composeKukutz());
    }

}

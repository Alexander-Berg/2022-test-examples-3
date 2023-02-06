package ru.yandex.autotests.innerpochta.tests.compose;

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
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

@Aqua.Test
@Title("Новый композ - Тесты на перевод письма")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.TRANSLATE)
public class ComposeActionsTranslateLetterTest extends BaseTest {

    private static final String TEXT = "уважай\n";
    private static final String TEXT2 = "автотест";
    private static final String TRANSLATE_TEXT = "respect\n ";
    private static final String TRANSLATE_TEXT2 = "respect\nautotest";
    private static final String TRANSLATE_TO_AR = "հարգիր\nավտոթեստ";
    private static final int LANG_AR = 2 + 99;
    private static final String HELP_URL = "https://yandex.ru/support/mail/web/letter" +
        "/translation.html#translation-outgoing";
    private static final String SEND_BTN_TEXT = "Отправить перевод";
    private static final String SUBJECT = Utils.getRandomName();
    private static final String INITIAL_TO_LANG = "Перевод наАнглийский";

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
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
        user.composeSteps().inputsSendText(TEXT);
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().translateBtn())
            .clicksOn(onComposePopup().expandedPopup().translateBtn())
            .shouldSee(
                onComposePopup().expandedPopup().translateText(),
                onComposePopup().expandedPopup().translateHeader()
            );
    }

    @Test
    @Title("Проверка перевода письма на английский")
    @TestCaseId("1199")
    public void shouldTranslateToEnText() {
        user.defaultSteps()
            .shouldSeeThatElementTextEquals(onComposePopup().expandedPopup().translateText(), TRANSLATE_TEXT)
            .appendTextInElement(onComposePopup().expandedPopup().bodyInput(), TEXT2)
            .shouldSeeThatElementTextEquals(onComposePopup().expandedPopup().translateText(), TRANSLATE_TEXT2);
    }

    @Test
    @Title("Проверка изменения языка перевода")
    @TestCaseId("2289")
    public void shouldTranslateToArText() {
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().bodyInput(), TEXT2)
            .shouldSeeThatElementTextEquals(onComposePopup().expandedPopup().translateText(), TRANSLATE_TEXT2)
            .shouldHasText(onComposePopup().expandedPopup().translateHeader().translateToLink().get(1), INITIAL_TO_LANG)
            .clicksOn(onComposePopup().expandedPopup().translateHeader().translateToLink().get(1))
            .onMouseHoverAndClick(
                onComposePopup().expandedPopup().changeLangList().waitUntil(not(empty())).get(LANG_AR))
            .shouldSeeThatElementTextEquals(onComposePopup().expandedPopup().translateText(), TRANSLATE_TO_AR);
    }

    @Test
    @Title("Тест на кнопку “Редактировать“")
    @TestCaseId("1200")
    public void shouldEditTranslateText() {
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().bodyInput(), TEXT2)
            .shouldSeeThatElementTextEquals(onComposePopup().expandedPopup().translateText(), TRANSLATE_TEXT2)
            .clicksOn(onComposePopup().expandedPopup().translateHeader().editTranslateBtn())
            .shouldNotSee(onComposePopup().expandedPopup().translateHeader());
        user.composeSteps().shouldSeeTextAreaContains(TRANSLATE_TEXT2);
    }

    @Test
    @Title("Тест на кнопку “Отменить“")
    @TestCaseId("2290")
    public void shouldCancelTranslateText() {
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().bodyInput(), TEXT2)
            .shouldSeeThatElementTextEquals(onComposePopup().expandedPopup().translateText(), TRANSLATE_TEXT2)
            .clicksOn(onComposePopup().expandedPopup().translateBtn())
            .shouldNotSee(onComposePopup().expandedPopup().translateHeader());
        user.composeSteps().shouldSeeTextAreaContains(TEXT2);
    }

    @Test
    @Title("Проверяем, куда ведёт линк “Справка“")
    @TestCaseId("1201")
    public void shouldOpenTranslateHelpLink() {
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().translateHeader().translateHelp())
            .clicksOn(onComposePopup().expandedPopup().translateHeader().translateHelp())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(HELP_URL);
    }

    @Test
    @Title("Тест кнопку “Отправить с переводом“")
    @TestCaseId("2291")
    public void shouldSendTranslateText() {
        user.defaultSteps().appendTextInElement(onComposePopup().expandedPopup().bodyInput(), TEXT2)
            .shouldSeeThatElementTextEquals(onComposePopup().expandedPopup().translateText(), TRANSLATE_TEXT2);
        user.composeSteps().inputsSubject(SUBJECT)
            .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail());
        user.defaultSteps().shouldHasText(onComposePopup().expandedPopup().sendBtn(), SEND_BTN_TEXT)
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageWithSubject(SUBJECT);
        user.messageViewSteps().shouldSeeCorrectMessageText(TRANSLATE_TEXT2);
    }
}

package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_ENABLE_AUTOSAVE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_HEAD_FULL_EDITION;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Открытие / Отправка")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
@RunWith(DataProviderRunner.class)
public class NewComposeMainTest extends BaseTest {
    String msgSubject;
    String msgTo;

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
        msgSubject = getRandomString();
        msgTo = lock.firstAcc().getSelfEmail();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем автосохранение, 2pane и сбрасываем свёрнутые композы",
            of(
                SETTINGS_ENABLE_AUTOSAVE, STATUS_ON,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                SETTINGS_FOLDER_THREAD_VIEW, FALSE,
                SETTINGS_HEAD_FULL_EDITION, TRUE
            )
        );
        user.apiMessagesSteps().createTemplateMessage(lock.firstAcc());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
    }

    @Test
    @Title("Открываем новый композ")
    @TestCaseId("5502")
    @UseDataProvider("layouts")
    public void shouldSeeComposePopup(String layout) {
        user.composeSteps().switchLayoutAndOpenCompose(layout);
        user.defaultSteps().shouldSee(
            onComposePopup().expandedPopup().sbj(),
            onComposePopup().expandedPopup().bodyInput(),
            onComposePopup().expandedPopup().hideBtn(),
            onComposePopup().expandedPopup().closeBtn(),
            onComposePopup().expandedPopup().sendBtn()
        );
    }

    @Test
    @Title("Проверяем, что все поля сохраняются при отправке письма")
    @TestCaseId("5526")
    public void shouldSendTheLetter() {
        String msgBody = getRandomString();
        user.composeSteps().openAndFillComposePopup(msgTo, msgSubject, msgBody);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject)
            .clicksOnMessageWithSubject(msgSubject);
        user.messageViewSteps().shouldSeeCorrectMessageText(msgBody);
        user.defaultSteps().clicksOn(onMessageView().messageHead().recipientsCount())
            .shouldSeeThatElementHasText(onMessageView().messageHead().contactsInTo().get(0), msgTo);
    }

    @Test
    @Title("Проверяем, что все поля сохраняются при отправке письма после сворачивания/разворачивания окна композа")
    @TestCaseId("5526")
    public void shouldSendTheLetterAfterHiding() {
        String msgBody = getRandomString();
        user.composeSteps().openAndFillComposePopup(msgTo, msgSubject, msgBody);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().hideBtn());
        shouldSeeCollapsedComposeAndDraft();
        user.defaultSteps().clicksOn(onComposePopup().composeThumb().get(0).theme())
            .appendTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .appendTextInElement(onComposePopup().expandedPopup().bodyInput(), msgBody)
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .clicksOn(onComposePopup().doneScreenInboxLink());
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject + msgSubject)
            .clicksOnMessageWithSubject(msgSubject + msgSubject);
        user.messageViewSteps().shouldSeeCorrectMessageText(msgBody + msgBody);
        user.defaultSteps().clicksOn(onMessageView().messageHead().recipientsCount())
            .shouldSeeThatElementHasText(onMessageView().messageHead().contactsInTo().get(0), msgTo);
    }

    @Test
    @Title("Отправляем письмо из стека после отправки не свернутого")
    @TestCaseId("6053")
    public void shouldSendLetterFromStackAfterSendingNonStack(){
        String msgBody1 = getRandomString();
        String msgBody2 = getRandomString();
        String msgSbj1 = getRandomString();
        String msgSbj2 = getRandomString();
        user.composeSteps().openAndFillComposePopup(msgTo, msgSbj1, msgBody1);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().hideBtn())
            .shouldSee(onComposePopup().composeThumb().get(0));
        user.composeSteps().openAndFillComposePopup(msgTo, msgSbj2, msgBody2);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .shouldNotSee(onComposePopup().expandedPopup())
            .clicksOn(onComposePopup().composeThumb().get(0).theme())
            .shouldSee(onComposePopup().expandedPopup())
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .shouldNotSee(onComposePopup().expandedPopup());
        openAndCheckLetter(msgSbj1, msgBody1);
        user.defaultSteps().opensFragment(INBOX);
        openAndCheckLetter(msgSbj2, msgBody2);
    }

    @Test
    @Title("Открываем композ горячей клавишей «w»")
    @TestCaseId("5513")
    public void shouldOpenComposeByW() {
        user.hotkeySteps().pressSimpleHotKey("w");
        user.defaultSteps().shouldSee(onComposePopup().composePopup());
    }

    @Test
    @Title("Открываем композ горячей клавишей «c»")
    @TestCaseId("5513")
    public void shouldOpenComposeByC() {
        user.hotkeySteps().pressSimpleHotKey("c");
        user.defaultSteps().shouldSee(onComposePopup().composePopup());
    }

    @Test
    @Title("Закрываем композ горячей клавишей «ESC»")
    @TestCaseId("5513")
    public void shouldCloseComposeByESC() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton());
        user.hotkeySteps().pressSimpleHotKey(onComposePopup().expandedPopup().bodyInput(), key(Keys.ESCAPE));
        user.defaultSteps().shouldNotSee(onComposePopup().composePopup());
    }

    @Test
    @Title("Отправляем письмо комбинацией горячих клавиш: «CTRL + ENTER»")
    @TestCaseId("5513")
    public void shouldSendLetterByCtrlEnter() {
        user.composeSteps().openAndFillComposePopup(msgTo, msgSubject, getRandomString());
        user.hotkeySteps().
            pressCombinationOfHotKeys(
                onComposePopup().expandedPopup().bodyInput(),
                key(Keys.CONTROL),
                key(Keys.ENTER)
            );
        user.defaultSteps().shouldSee(onComposePopup().doneScreen());
    }

    @Test
    @Title("Сохраняем черновик с получателем комбинацией горячих клавиш: «CTRL + S»")
    @TestCaseId("5513")
    public void shouldSaveDraftWithToByCtrlS() {
        user.defaultSteps().clicksOn(onMessagePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), DEV_NULL_EMAIL);
        user.hotkeySteps()
            .pressCombinationOfHotKeys(
                onComposePopup().expandedPopup().bodyInput(),
                key(Keys.CONTROL),
                "s"
            );
        checkSavedAtAndOpenDrafts();
        user.messagesSteps().shouldSeeAddressOnMessageWithSubject(DEV_NULL_EMAIL, "(Без темы)");
    }

    @Test
    @Title("Сохраняем черновик с темой комбинацией горячих клавиш: «CTRL + S»")
    @TestCaseId("5513")
    public void shouldSaveDraftWithSubjectByCtrlS() {
        user.defaultSteps().clicksOn(onMessagePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject);
        user.hotkeySteps()
            .pressCombinationOfHotKeys(
                onComposePopup().expandedPopup().bodyInput(),
                key(Keys.CONTROL),
                "s"
            );
        checkSavedAtAndOpenDrafts();
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
    }

    @Test
    @Title("Сохраняем черновик с телом письма комбинацией горячих клавиш: «CTRL + S»")
    @TestCaseId("5513")
    public void shouldSaveDraftWithBodyByCtrlS() {
        String msgBody = getRandomString();
        user.defaultSteps().clicksOn(onMessagePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), msgBody);
        user.hotkeySteps()
            .pressCombinationOfHotKeys(
                onComposePopup().expandedPopup().bodyInput(),
                key(Keys.CONTROL),
                "s"
            );
        checkSavedAtAndOpenDrafts();
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().shouldSeeThatElementHasText(onComposePopup().expandedPopup().bodyInput(), msgBody);
    }

    @Test
    @Title("Копируем и вставляем выделенный текст в теле письма горячими клавишами: «CTRL + С» и «CTRL + V»")
    @TestCaseId("4491")
    public void shouldCopyAndPasteTextInComposeBodyByHotKeys() {
        String msgBody = getRandomString();
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
        inputAndCopyByHotKey(onComposePopup().expandedPopup().bodyInput(), msgBody);
        pasteFromBufferAndCheck(onComposePopup().expandedPopup().bodyInput(), msgBody + msgBody);
    }

    @Test
    @Title("Вставляем в тело письма данные из буфера обмена после нажатия «CTRL + С», ничего не выделяя")
    @TestCaseId("4491")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-63101")
    public void shouldPasteTextFromBufferAfterNoHighlightCopyByHotKeys() {
        String msgBody = getRandomString();
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
        inputAndCopyByHotKey(onComposePopup().expandedPopup().bodyInput(), msgBody);
        user.hotkeySteps().pressCombinationOfHotKeys(
            onComposePopup().expandedPopup().bodyInput(),
            key(Keys.CONTROL),
            "c"
        );
        pasteFromBufferAndCheck(onComposePopup().expandedPopup().bodyInput(), msgBody + "\n" + msgBody);
    }

    @Test
    @Title("Пересохраняем шаблон горячими клавишами: «CTRL + S»")
    @TestCaseId("974")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-69052")
    public void testResaveTemplateHotkey() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().templatesBtn())
            .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0))
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .shouldSee(onComposePopup().expandedPopup().templatesNotif());
        user.hotkeySteps().pressCombinationOfHotKeys(
            onComposePopup().expandedPopup().bodyInput(),
            key(Keys.CONTROL),
            "s"
        );
        user.defaultSteps().shouldNotSee(onComposePopup().expandedPopup().templatesNotif())
            .clicksOn(onComposePopup().expandedPopup().templatesBtn())
            .shouldContainText(
                onComposePopup().expandedPopup().templatePopup().templateList().get(0).templateElementText(),
                msgSubject
            );
    }

    @Test
    @Title("Удаляем текст в поле «Кому» горячей клавишей «Backspace»")
    @TestCaseId("4492")
    public void shoudlDeleteAllTextFromToFieldByBackspace() {
        String data = getRandomString();
        inputStringAndHighlight(onComposePopup().expandedPopup().popupTo(), data);
        user.hotkeySteps().pressSimpleHotKey(onComposePopup().expandedPopup().popupTo(), key(Keys.BACK_SPACE));
        user.defaultSteps().shouldNotContainText(onComposePopup().expandedPopup().popupTo(), data);
    }

    @Test
    @Title("Удаляем текст в поле «Кому» горячей клавишей «Delete»")
    @TestCaseId("4492")
    public void shoudlDeleteAllTextFromToFieldByDelete() {
        String data = getRandomString();
        inputStringAndHighlight(onComposePopup().expandedPopup().popupTo(), data);
        user.hotkeySteps().pressSimpleHotKey(onComposePopup().expandedPopup().popupTo(), key(Keys.DELETE));
        user.defaultSteps().shouldNotContainText(onComposePopup().expandedPopup().popupTo(), data);
    }

    @Step("Проверяем что композ свернулся и черновик сохранился")
    private void shouldSeeCollapsedComposeAndDraft() {
        user.defaultSteps().shouldNotSee(onComposePopup().composePopup())
            .shouldSee(
                onComposePopup().composeThumb().get(0),
                onComposePopup().composeThumb().get(0).avatar(),
                onComposePopup().composeThumb().get(0).theme(),
                onComposePopup().composeThumb().get(0).expandBtn(),
                onComposePopup().composeThumb().get(0).closeBtn()
            )
            .opensFragment(QuickFragments.DRAFT);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
    }

    @Step("Проверяем надпись о сохранении и переходим в папку с черновиками")
    private void checkSavedAtAndOpenDrafts() {
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().savedAt())
            .clicksOn(onComposePopup().expandedPopup().closeBtn())
            .opensFragment(QuickFragments.DRAFT);
    }

    @Step("Вводим данные в поле и копируем их через «CTRL + С»")
    private void inputAndCopyByHotKey(MailElement element, String data) {
        user.defaultSteps().inputsTextInElement(element, data);
        user.hotkeySteps().pressCombinationOfHotKeys(element, key(Keys.CONTROL), "a")
            .pressCombinationOfHotKeys(key(Keys.CONTROL), "c");
        user.defaultSteps().clicksOn(element);
    }

    @Step("Вставляем данные из буфера обмена и проверяем результат")
    private void pasteFromBufferAndCheck(MailElement element, String data) {
        user.hotkeySteps().pressCombinationOfHotKeys(element, key(Keys.CONTROL), "v");
        user.defaultSteps().shouldSeeThatElementHasText(element, data);
    }

    @Step("Вводим строку в поле и выделяем ее через горячие клавиши «CTRL + A»")
    private void inputStringAndHighlight(WebElement field, String data) {
        user.defaultSteps().appendTextInElement(field, data);
        user.hotkeySteps().pressCombinationOfHotKeys(
            onComposePopup().expandedPopup().popupTo(),
            key(Keys.CONTROL),
            "a"
        );
    }

    @Step("Открываем письмо и проверяем тело")
    private void openAndCheckLetter(String sbj, String body) {
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.messageViewSteps().shouldSeeCorrectMessageText(body);
    }
}

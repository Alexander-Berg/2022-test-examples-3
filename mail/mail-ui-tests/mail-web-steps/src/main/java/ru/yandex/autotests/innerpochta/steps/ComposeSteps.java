package ru.yandex.autotests.innerpochta.steps;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.annotations.SkipIfFailed;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.AttachElementsBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.qatools.allure.annotations.Step;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Pattern;

import static ch.lambdaj.Lambda.extract;
import static com.google.common.collect.ImmutableMap.of;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.matchers.compose.ComposeMatcher.hasAddresses;
import static ru.yandex.autotests.innerpochta.matchers.compose.ComposeMatcher.hasClearAddress;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.DISABLE_COMPOSE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.FREEZE_DONE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.SkipStep.SkipStepMethods.assumeStepCanContinue;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_STORED_COMPOSE_STATES;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@SuppressWarnings({"UnusedReturnValue", "unchecked"})
public class ComposeSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    private static final String COMPOSE_PARAMS = "{\"miniData\":{\"avatar\":null,\"subject\":\"%s\",\"lastSaved\":0}," +
        "\"params\":{}}";
    private static final String FIRST_COMPOSE_PREFIX = "%5B";
    private static final String OTHER_COMPOSE_PREFIX = "%2C";
    private static final String LAST_COMPOSE_PREFIX = "%5D";

    ComposeSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    //-------------------------------------------------------
    // DRAFT SENDING
    //-------------------------------------------------------

    @Step("Отправляем черновик с To: “{1}“")
    public ComposeSteps prepareDraftFor(String to, String subject, String send) {
        String mid = user.apiMessagesSteps().prepareDraft(to, subject, send);
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE_MSG_FRAGMENT.fragment(mid));
        return this;
    }

    @Step("Отправляем черновик «{0}» с темой «{1}» и текстом «{2}»")
    public String sendDraftWithContentFor(String to, String subj, String send) {
        String mid = user.apiMessagesSteps().prepareDraft(to, subj, send);
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE_MSG_FRAGMENT.fragment(mid));
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        waitForMessageToBeSend();
        user.defaultSteps().clicksIfCanOn(user.pages().ComposePopup().doneScreenInboxLink())
            .opensFragment(QuickFragments.INBOX);
        return subj;
    }

    //--------------------------------------------------------
    // COMPOSE PAGE STEPS
    //-------------------------------------------------------
    @Step("Вводим адрес (кому): «{0}»")
    public ComposeSteps inputsAddressInFieldTo(String to) {
        user.defaultSteps().appendTextInElement(user.pages().ComposePopup().expandedPopup().popupTo(), to);
        return this;
    }

    @Step("Удаляем адрес в поле «Кому»")
    public ComposeSteps clearAddressFieldTo() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().yabbleTo().yabbleDeleteBtn());
        return this;
    }

    @Step("Добавляем еще одного получателя: «{0}» в поле «Кому»")
    public ComposeSteps addAnotherRecipient(String to) {
        user.pages().ComposePopup().expandedPopup().popupTo().sendKeys(", " + to);
        return this;
    }

    @Step("Вводим адрес в поле копия: «{0}»")
    public ComposeSteps inputsAddressInFieldCc(String address) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().popupCc(), address);
        return this;
    }

    @Step("Вводим адрес в поле BCC: «{0}»")
    public ComposeSteps inputsAddressInFieldBcc(String address) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().popupBcc(), address);
        return this;
    }

    @Step("Вводим тему письма: «{0}»")
    public ComposeSteps inputsSubject(String subject) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().sbjInput(), subject);
        return this;
    }

    @Step("Вводим номер телефона: «{0}»")
    public ComposeSteps inputsPhoneNumber(String phoneNumber) {
        user.defaultSteps().inputsTextInElement(
            user.pages().ComposePage().composeFieldsBlock().phoneInput(),
            phoneNumber
        );
        return this;
    }

    @Step("Очищаем поле «Тема»")
    public ComposeSteps clearInputsSubjectField() {
        user.defaultSteps().clearTextInput(user.pages().ComposePopup().expandedPopup().sbjInput());
        return this;
    }

    @Step("Вводим текст письма: «{0}»")
    public ComposeSteps inputsSendText(String sendText) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().bodyInput(), sendText);
        return this;
    }

    @Step("Раскрываем цитирование в композе")
    public ComposeSteps revealQuotes() {
        user.defaultSteps().clicksIfCanOn(user.pages().ComposePopup().showQuote())
            .shouldNotSee(user.pages().ComposePopup().showQuote());
        return this;
    }

    @Step("Вводим текст письма «{0}» при включенном форматировании")
    public ComposeSteps inputsSendTextWithFormatting(String sendText) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().bodyInput(), sendText);
        return this;
    }

    @Step("Очищаем текст письма")
    public ComposeSteps clearInputsSendTextField() {
        user.defaultSteps().clearTextInput(user.pages().ComposePopup().expandedPopup().bodyInput());
        return this;
    }

    @Step("Кликаем на кнопку “Отправить“ в шапке композа.")
    public ComposeSteps clicksOnSendButtonInHeader() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        return this;
    }

    @Step("Отправляем письмо To: «{0}», Тема: «{1}», Текст: «{2}»")
    public ComposeSteps inputsAndSendMail(String to, String subject, String text) {
        inputsToAndSendMail(to, subject, text);
        waitForMessageToBeSend();
        return this;
    }

    @Step("Заполняем поля Кому, Тема и тело письма")
    public ComposeSteps inputsMailContents(String to, String subject, String text) {
        inputsAddressInFieldTo(to);
        inputsSendTextWithFormatting(text);
        inputsSubject(subject);
        return this;
    }

    @Step("Отправляем письмо ещё раз, если адрес оказался невалидным")
    private ComposeSteps composeMessageAgainIfAddressIsInvalid(String to, String subject, String text) {
        if (isPresent().matches(user.pages().ComposePage().notificationAddressRequired())) {
            user.defaultSteps().opensFragment(QuickFragments.INBOX);
            if (isPresent().matches(user.pages().ComposePage().composePageSaveToDraftBlock().doNotSaveButton())) {
                user.defaultSteps()
                    .clicksOn(user.pages().ComposePage().composePageSaveToDraftBlock().doNotSaveButton());
            }
            user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton());
            inputsToAndSendMail(to, subject, text);
        }
        return this;
    }

    @Step("Заполняем поля Кому, Тема и тело письма и отправляем письмо")
    private ComposeSteps inputsToAndSendMail(String to, String subject, String text) {
        inputsAddressInFieldTo(to + " ");
        inputsSubject(subject);
        inputsSendText(text);
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        composeMessageAgainIfAddressIsInvalid(to, subject, text);
        return this;
    }

    @Step("Должны видеть попап сохранения письма в черновики")
    public ComposeSteps shouldSeeSaveToDraftPopUp() {
        assertThat(
            "Попап сохранения письма в черновики не появился",
            user.pages().ComposePage().composePageSaveToDraftBlock().doNotSaveButton(),
            isDisplayed()
        );
        return this;
    }

    @Step("Поле для ввода текста должно быть пустым")
    public ComposeSteps shouldSeeEmptyTextArea() {
        user.defaultSteps()
            .clicksIfCanOn(user.pages().ComposePopup().expandedPopup().toolbarBlock().turnOnFormattingBtn());
        assertThat(
            "Поле для ввода текста должно быть пустым",
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            hasText("")
        );
        return this;
    }

    @Step("Не должно быть адресов в поле кому")
    public ComposeSteps shouldSeeEmptySendFieldTo() {
        assertThat("Поле «Кому» должно быть пустым", webDriverRule, hasClearAddress());
        return this;
    }

    @Step("Тело письма должно содержать текст: «{0}»")
    public ComposeSteps shouldSeeTextAreaContains(String value) {
        shouldSeeFormattedTextAreaContains(value);
        return this;
    }

    @Step("Тело письма должно содержать текст: «{0}»")
    public ComposeSteps shouldSeeFormattedTextAreaContains(String value) {
        assertThat(
            "Поле для ввода должно содержать: " + value,
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            allOf((withWaitFor(isPresent())), hasText(containsString(value)))
        );
        return this;
    }

    @Step("Тело письма не должно содержать текст: «{0}»")
    public ComposeSteps shouldNotSeeTextAreaContains(String value) {
        assertThat(
            "Поле для ввода не должно содержать: " + value,
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            allOf((withWaitFor(isPresent())), not(hasText(containsString(value))))
        );
        return this;
    }

    @Step("Поле «Кому» должно содержать текст: «{0}»")
    public ComposeSteps shouldSeeSendToAreaContains(String value) {
        assertThat(
            "Неверный адрес для отправления письма",
            user.pages().ComposePopup().yabbleTo().yabbleText(),
            withWaitFor(hasText(value))
        );
        return this;
    }

    @Step("Поле «Кому» должно содержать текст: «{0}»")
    public ComposeSteps shouldSeeSendToAreaHas(String... addresses) {
        assertThat("Неверный адрес для отправления письма", webDriverRule, withWaitFor(hasAddresses(addresses)));
        return this;
    }

    @Step("Поле «Кому» не должно содержать текст: «{0}»")
    public ComposeSteps shouldNotSeeSendToAreaHas(String... addresses) {
        assertThat("Поле “Кому“ содержит адрес для отправки писама", webDriverRule, not(hasAddresses(addresses)));
        return this;
    }

    @SkipIfFailed
    @Step("Выключаем оформление письма")
    public ComposeSteps clicksOnTurnOffHtmlView() {
        assumeStepCanContinue(user.pages().ComposePage().composeToolbarBlock().turnOffFormattingBtn(), isPresent());
        user.defaultSteps().clicksOn(user.pages().ComposePage().composeToolbarBlock().turnOffFormattingBtn());
        return this;
    }

    @Step("Должны видеть кнопку отправить")
    public ComposeSteps shouldSeeSendButton() {
        user.defaultSteps().shouldSee(user.pages().ComposePopup().expandedPopup().sendBtn());
        return this;
    }

    @Step("Поле «Тема» должно содержать текст: {0}")
    public ComposeSteps shouldSeeSubject(String subject) {
        assertThat(
            "Неверный текст в поле subject",
            user.pages().ComposePopup().expandedPopup().sbjInput(),
            withWaitFor(hasValue(subject))
        );
        return this;
    }

    @Step("Должны видеть сообщение {1} в виде аттача")
    public ComposeSteps shouldSeeMessageAsAttachment(int index, String subject) {
        String subjectCut = (subject.length() < 10) ? subject : subject.substring(0, 10);
        user.defaultSteps().shouldSee(user.pages().ComposePopup().expandedPopup().attachPanel().linkedAttach().get(index).attachEml());
        assertThat(
            "Неверное имя приложенного письма",
            user.pages().ComposePopup().expandedPopup().attachPanel().linkedAttach().get(index).attachName(),
            hasText(containsString(subjectCut))
        );
        return this;
    }

    @Step("Должны видеть сообщение о том, что ответ получат не все")
    public ComposeSteps shouldSeeNotificationAboutOtherAddresses() {
        assertThat(
            "Неверный текст предупреждения!",
            user.pages().MessageViewPage().notificationAboutReply(),
            allOf(withWaitFor(isPresent()), hasText(containsString("Ваш ответ получат не все участники переписки")))
        );
        return this;
    }

    @Step("Поле «CC» должно содержать текст: {0}")
    public ComposeSteps shouldSeeCCAreaContains(String text) {
        assertThat(
            "Неверный адрес в СС письма",
            user.pages().ComposePopup().yabbleCc().yabbleText(),
            hasText(text)
        );
        return this;
    }

    @Step("Должны видеть попап «Больше не спрашивать» о том, что получат не все")
    public ComposeSteps shouldSeeNotificationTab() {
        user.defaultSteps().shouldSee(
            user.pages().MessageViewPage().replyNotification(),
            user.pages().MessageViewPage().replyNotification().replyLink(),
            user.pages().MessageViewPage().replyNotification().getReplyToAllLink()
        );
        return this;
    }

    @Step("Не должны видеть попап «Больше не спрашивать»")
    public ComposeSteps shouldNotSeeNotificationTab() {
        assertThat(
            "Блока уведомления про ответить всем быть не должно",
            user.pages().MessageViewPage().replyNotification(),
            not(isPresent())
        );
        return this;
    }

    @Step("Не должно быть предупреждения о других адресах")
    public ComposeSteps shouldNotSeeNotificationAboutOtherAddresses() {
        assertThat(
            "Предупреждения о других адресах быть не должно!",
            user.pages().MessageViewPage().notificationAboutReply(),
            not(isPresent())
        );
        return this;
    }

    @Step("Должны видеть сообщение о сохранении в черновик")
    public ComposeSteps shouldSeeThatMessageIsSavedToDraft() {
        assertThat(
            "Не появилось сообщения о том, что письмо сохранено в черновики",
            user.pages().ComposePopup().expandedPopup().savedAt(),
            withWaitFor(isPresent(), SECONDS.toMillis(20))
        );
        assertThat(
            "Неверный текст сообщения о том, что письмо сохранено в черновики",
            user.pages().ComposePopup().expandedPopup().savedAt(),
            hasText(containsString("сохранено в"))
        );
        return this;
    }

    @Step("Ждём пока отправится сообщение")
    public ComposeSteps waitForMessageToBeSend() {
        user.defaultSteps().shouldNotSee(user.pages().ComposePopup().composePopup());
        return this;
    }

    @Step("Должны видеть текст с подписью «{0}»")
    public ComposeSteps shouldSeeTextWithSignature(String signature) {
        shouldSeeTextIn(signature);
        return this;
    }

    @Step("Должны видеть текст подписи «{0}»")
    private ComposeSteps shouldSeeTextIn(String signature) {
        assertThat(
            "Ожидалась другая подпись",
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            hasText(signature)
        );
        return this;
    }

    @Step("Текст {0} должен быть выделен жирным")
    public ComposeSteps shouldSeeBoldText(String text) {
        assertThat(
            "Неверное форматирование текста",
            user.pages().ComposePage().textareaBlock().formattedText().boldText(),
            allOf(isPresent(), hasText(containsString(text)))
        );
        return this;
    }

    @Step("Должны видеть подпись с форматированием")
    public ComposeSteps shouldSeeSignatureWithFormatting(String text) {
        user.defaultSteps().shouldSee(user.pages().ComposePopup().expandedPopup());
        assertThat(
            "Текст должен быть курсивным",
            user.pages().ComposePopup().expandedPopup().bodyInput().italicText(),
            allOf(isPresent(), hasText(text))
        );
        return this;
    }

    @Step("Имя и email должны совпадать с «{0}» и «{1}» соответственно")
    public ComposeSteps shouldSeeSuggestedNameAndEmail(String name, String email) {
        assertThat(
            "Саджест не появился",
            user.pages().ComposePage().suggestList(),
            withWaitFor(hasSize(greaterThan(0)))
        );
        assertThat(
            "Неверное имя в саджесте",
            user.pages().ComposePage().suggestList().get(0).contactName(),
            hasText(name)
        );
        assertThat(
            "Неверный адрес в саджесте",
            user.pages().ComposePage().suggestList().get(0).contactEmail(),
            hasText(email)
        );
        return this;
    }

    @Step("Название группы в саджесте должно совпадать с «{0}»")
    public ComposeSteps shouldSeeSuggestedGroup(String name) {
        user.defaultSteps().waitInSeconds(1);
        assertThat(
            "Саджест не появился",
            user.pages().ComposePopup().suggestList(),
            hasSize(greaterThan(0))
        );
        assertThat(
            "Неверное имя группы в саджесте",
            user.pages().ComposePopup().suggestList().get(0).contactName().getText(),
            containsString(name)
        );
        return this;
    }

    @Step("Проверяем правильность аттачей")
    public ComposeSteps checkMessageAttachments(String... texts) {
        for (int i = 0; i < texts.length; i++) {
            user.defaultSteps().clicksOn(user.pages().ComposePage().forwardedMsgAttachBlock().get(i).messageLink())
                .switchOnWindow(i + 1);
            user.messageViewSteps().shouldSeeCorrectMessageText(texts[i]);
            user.defaultSteps().switchOnWindow(0);
        }
        return this;
    }

    @Step("Проверяем, что кнопки «Отправить» и «Сохранить Шаблон» идут в нужном порядке")
    public ComposeSteps shouldSeeChangedOrderTemplatesButtons() {
        assertThat(
            "Не нашли нужных кнопок Отправить и Сохранить Шаблон",
            user.pages().ComposePage().composeHead().buttonsList(),
            hasSize(2)
        );
        assertThat(
            "Должен быть другой порядок (или цвет) кнопок",
            user.pages().ComposePage().composeHead().buttonsList().get(1),
            allOf(hasAttribute("class", containsString("js-save-button")))
        );
        return this;
    }

    @Step("Должны видеть подпись с текстом: “{0}“")
    public ComposeSteps shouldSeeSignatureInList(String text) {
        List<String> signatures = extract(
            user.pages().ComposePopup().signaturesPopup().signaturesList(),
            ch.lambdaj.Lambda.on(MailElement.class).getText()
        );
        assertThat("Должна быть одна подпись с нужным текстом", signatures, hasItem(text));
        return this;
    }

    @Step("Выбираем в календаре завтрашнюю дату")
    public ComposeSteps selectDateFromComposeCalendar() {
        user.defaultSteps().shouldSee(user.pages().ComposePopup().expandedPopup().calendar());
        assertThat(
            "Нет завтрашней даты",
            user.pages().ComposePopup().expandedPopup().calendar().calendarDates(),
            hasSize(greaterThan(0))
        );
        user.defaultSteps()
            .clicksOn(user.pages().ComposePopup().expandedPopup().calendar().calendarDates().get(0))
            .clicksOn(user.pages().ComposePopup().expandedPopup().calendar().saveBtn());
        return this;
    }

    @Step("Добавляем {0} файл с диска")
    public void addAttachFromDisk(int index) {
        user.defaultSteps()
            .clicksOn(user.pages().ComposePopup().expandedPopup().diskAttachBtn())
            .shouldSee(user.pages().ComposePopup().addDiskAttachPopup())
            .clicksOn(user.pages().ComposePopup().addDiskAttachPopup().attachList().waitUntil(not(empty())).get(index))
            .clicksOn(user.pages().ComposePopup().addDiskAttachPopup().addAttachBtn())
            .shouldNotSee(
                user.pages().ComposePopup().addDiskAttachPopup(),
                user.pages().ComposePopup().expandedPopup().attachPanel().loadingAttach()
            );
    }

    @Step("Отправляем письмо и переходим на Done")
    public ComposeSteps goToDone(String email) {
        user.defaultSteps().opensFragment(COMPOSE)
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        inputsAddressInFieldTo(email);
        inputsSubject(getRandomString());
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn())
            .shouldSee(user.pages().ComposePopup().doneScreen());
        freezeDone();
        return this;
    }

    @Step("Отключаем переход с Done в список писем")
    private ComposeSteps freezeDone() {
        user.defaultSteps().executesJavaScript(FREEZE_DONE_SCRIPT);
        return this;
    }

    @Step("Выключааем алерт композа")
    public ComposeSteps disableComposeAlert() {
        user.defaultSteps().executesJavaScript(DISABLE_COMPOSE_SCRIPT);
        return this;
    }

    @Step("Имя прикрепленного аттача под номером {1}  должно быть {2}")
    public ComposeSteps shouldSeeAttachmentName(List<AttachElementsBlock> attachmentBlocks, int index,
                                                String expectedName) {
        String actualName = attachmentBlocks.get(index).attachName().getText();
        assertThat("Фактическое имя аттача не соответствует ожидаемому", actualName, is(expectedName));
        return this;
    }

    @Step("Загружаем локальный файл в композ")
    public ComposeSteps uploadLocalFile(WebElement element, String attachName) {
        element.sendKeys(user.defaultSteps().getAttachPath(attachName));
        return this;
    }

    @Step("Делаем текст в визивиге c {0} до {1} жирным")
    public ComposeSteps setTextBold(int from, int to) {
        user.defaultSteps().selectText(user.pages().ComposePage().textareaBlock().formattedText(), from, to)
            .clicksOn(user.pages().ComposePage().composeToolbarBlock().bold());
        return this;
    }

    @Step("Делаем текст в визивиге c {0} до {1} курсивным")
    public ComposeSteps setTextItalic(int from, int to) {
        user.defaultSteps().selectText(user.pages().ComposePage().textareaBlock().formattedText(), from, to)
            .clicksOn(user.pages().ComposePage().composeToolbarBlock().italic());
        return this;
    }

    @Step("Очищаем форматирование текста в визивиге c {0} до {1}")
    public ComposeSteps clearTextFormatting(int from, int to) {
        user.defaultSteps().selectText(user.pages().ComposePage().textareaBlock().formattedText(), from, to)
            .clicksOn(user.pages().ComposePage().composeToolbarBlock().removeFormatting());
        return this;
    }

    @Step("Дописываем текст в тело письма по индексу {0}")
    public ComposeSteps appendTextToIndex(int index, String text) {
        Pattern nonBMP = Pattern.compile("[^\u0000-\uFFFF]"); //chromedriver не умеет вводить символы больше \uFFFF
        if (nonBMP.matcher(text).find()) {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            user.defaultSteps().setCaretToIndexAction(
                user.pages().ComposePage().textareaBlock().formattedText(),
                index
            ).sendKeys(Keys.chord(Keys.CONTROL, "v")).perform();
        } else {
            user.defaultSteps().setCaretToIndexAction(
                user.pages().ComposePage().textareaBlock().formattedText(),
                index
            ).sendKeys(text).perform();
        }
        return this;
    }

    @Step("Проверяем открытие и закрытие дискового попапа")
    public ComposeSteps checkCloseDiskPopup(WebElement button) {
        user.defaultSteps().clicksOn(button)
            .shouldSee(user.pages().ComposePage().addDiskAttachPopup())
            .clicksOn(user.pages().ComposePage().addDiskAttachPopup().closePopupBtn())
            .shouldNotSee(user.pages().ComposePage().addDiskAttachPopup());
        return this;
    }

    //--------------------------------------------------------
    // COMPOSE POPUP STEPS
    //-------------------------------------------------------

    @Step("Переключаем вид интерфейса и открываем композ")
    public void switchLayoutAndOpenCompose(String layout) {
        user.apiSettingsSteps()
            .callWithListAndParams("Включаем " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        user.defaultSteps().refreshPage()
            .clicksOn(user.pages().MessagePage().composeButton());
    }

    @Step("Открываем композ и заполняем поля To, Subject и Body")
    public void openAndFillComposePopup(String msgTo, String msgSubject, String msgBody) {
        user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton())
            .inputsTextInElement(user.pages().ComposePopup().expandedPopup().popupTo(), msgTo)
            .inputsTextInElement(user.pages().ComposePopup().expandedPopup().sbjInput(), msgSubject)
            .inputsTextInElement(user.pages().ComposePopup().expandedPopup().bodyInput(), msgBody);
    }

    @Step("Формируем стек свернутых композов")
    public void fillComposeStack(int count) {
        String composeParamsPack = String.format(COMPOSE_PARAMS, '0');
        for (int i = 1; i <= count; i++) {
            composeParamsPack = String.join(
                ",",
                composeParamsPack,
                String.format(COMPOSE_PARAMS, String.valueOf(i))
            );
        }
        try {
            String composeParams = URLEncoder.encode(
                "[" + composeParamsPack + "]",
                "UTF-8"
            );
            user.apiSettingsSteps().callWithListAndParams(
                of(SETTINGS_STORED_COMPOSE_STATES, composeParams)
            );
        } catch (Exception UnsupportedEncodingException) {
        }
        user.defaultSteps().refreshPage();
    }

    @Step("Закрываем композ")
    public ComposeSteps closeComposePopup() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().closeBtn())
            .shouldNotSee(user.pages().ComposePopup().expandedPopup());
        return this;
    }

    @Step("Кликаем на кнопку «Отправить» в шапке композа.")
    public ComposeSteps clicksOnSendBtn() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        return this;
    }

    @Step("Кликаем на кнопку «Добавить в виде вложения» при пересылке письма")
    public ComposeSteps clicksOnAddEmlBtn() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().composeAddEmlBtn());
        return this;
    }

    @Step("Отправляем письмо себе с аттачем")
    public ComposeSteps sendMsgWithAttach(String to, String subject) {
        user.defaultSteps().clicksOn(user.pages().HomePage().composeButton())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        inputsAddressInFieldTo(to);
        inputsSubject(subject);
        uploadLocalFile(
            user.pages().ComposePopup().expandedPopup().localAttachInput(),
            IMAGE_ATTACHMENT
        );
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        waitForMessageToBeSend();
        user.defaultSteps().refreshPage()
            .shouldSeeElementsCount(
                user.pages().MessagePage().displayedMessages().list().get(0).attachments().list(),
                1
            );
        return this;
    }

    @Step("Открываем композ")
    public ComposeSteps openComposePopup() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        return this;
    }

    @Step("Раскрываем поле cc/bcc")
    public ComposeSteps expandCcBcc() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().expandCollapseBtn());
        return this;
    }
}

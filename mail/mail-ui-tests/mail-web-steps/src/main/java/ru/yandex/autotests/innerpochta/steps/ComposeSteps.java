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

    @Step("???????????????????? ???????????????? ?? To: ???{1}???")
    public ComposeSteps prepareDraftFor(String to, String subject, String send) {
        String mid = user.apiMessagesSteps().prepareDraft(to, subject, send);
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE_MSG_FRAGMENT.fragment(mid));
        return this;
    }

    @Step("???????????????????? ???????????????? ??{0}?? ?? ?????????? ??{1}?? ?? ?????????????? ??{2}??")
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
    @Step("???????????? ?????????? (????????): ??{0}??")
    public ComposeSteps inputsAddressInFieldTo(String to) {
        user.defaultSteps().appendTextInElement(user.pages().ComposePopup().expandedPopup().popupTo(), to);
        return this;
    }

    @Step("?????????????? ?????????? ?? ???????? ????????????")
    public ComposeSteps clearAddressFieldTo() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().yabbleTo().yabbleDeleteBtn());
        return this;
    }

    @Step("?????????????????? ?????? ???????????? ????????????????????: ??{0}?? ?? ???????? ????????????")
    public ComposeSteps addAnotherRecipient(String to) {
        user.pages().ComposePopup().expandedPopup().popupTo().sendKeys(", " + to);
        return this;
    }

    @Step("???????????? ?????????? ?? ???????? ??????????: ??{0}??")
    public ComposeSteps inputsAddressInFieldCc(String address) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().popupCc(), address);
        return this;
    }

    @Step("???????????? ?????????? ?? ???????? BCC: ??{0}??")
    public ComposeSteps inputsAddressInFieldBcc(String address) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().popupBcc(), address);
        return this;
    }

    @Step("???????????? ???????? ????????????: ??{0}??")
    public ComposeSteps inputsSubject(String subject) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().sbjInput(), subject);
        return this;
    }

    @Step("???????????? ?????????? ????????????????: ??{0}??")
    public ComposeSteps inputsPhoneNumber(String phoneNumber) {
        user.defaultSteps().inputsTextInElement(
            user.pages().ComposePage().composeFieldsBlock().phoneInput(),
            phoneNumber
        );
        return this;
    }

    @Step("?????????????? ???????? ????????????")
    public ComposeSteps clearInputsSubjectField() {
        user.defaultSteps().clearTextInput(user.pages().ComposePopup().expandedPopup().sbjInput());
        return this;
    }

    @Step("???????????? ?????????? ????????????: ??{0}??")
    public ComposeSteps inputsSendText(String sendText) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().bodyInput(), sendText);
        return this;
    }

    @Step("???????????????????? ?????????????????????? ?? ??????????????")
    public ComposeSteps revealQuotes() {
        user.defaultSteps().clicksIfCanOn(user.pages().ComposePopup().showQuote())
            .shouldNotSee(user.pages().ComposePopup().showQuote());
        return this;
    }

    @Step("???????????? ?????????? ???????????? ??{0}?? ?????? ???????????????????? ????????????????????????????")
    public ComposeSteps inputsSendTextWithFormatting(String sendText) {
        user.defaultSteps().inputsTextInElement(user.pages().ComposePopup().expandedPopup().bodyInput(), sendText);
        return this;
    }

    @Step("?????????????? ?????????? ????????????")
    public ComposeSteps clearInputsSendTextField() {
        user.defaultSteps().clearTextInput(user.pages().ComposePopup().expandedPopup().bodyInput());
        return this;
    }

    @Step("?????????????? ???? ???????????? ???????????????????????? ?? ?????????? ??????????????.")
    public ComposeSteps clicksOnSendButtonInHeader() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        return this;
    }

    @Step("???????????????????? ???????????? To: ??{0}??, ????????: ??{1}??, ??????????: ??{2}??")
    public ComposeSteps inputsAndSendMail(String to, String subject, String text) {
        inputsToAndSendMail(to, subject, text);
        waitForMessageToBeSend();
        return this;
    }

    @Step("?????????????????? ???????? ????????, ???????? ?? ???????? ????????????")
    public ComposeSteps inputsMailContents(String to, String subject, String text) {
        inputsAddressInFieldTo(to);
        inputsSendTextWithFormatting(text);
        inputsSubject(subject);
        return this;
    }

    @Step("???????????????????? ???????????? ?????? ??????, ???????? ?????????? ???????????????? ????????????????????")
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

    @Step("?????????????????? ???????? ????????, ???????? ?? ???????? ???????????? ?? ???????????????????? ????????????")
    private ComposeSteps inputsToAndSendMail(String to, String subject, String text) {
        inputsAddressInFieldTo(to + " ");
        inputsSubject(subject);
        inputsSendText(text);
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        composeMessageAgainIfAddressIsInvalid(to, subject, text);
        return this;
    }

    @Step("???????????? ???????????? ?????????? ???????????????????? ???????????? ?? ??????????????????")
    public ComposeSteps shouldSeeSaveToDraftPopUp() {
        assertThat(
            "?????????? ???????????????????? ???????????? ?? ?????????????????? ???? ????????????????",
            user.pages().ComposePage().composePageSaveToDraftBlock().doNotSaveButton(),
            isDisplayed()
        );
        return this;
    }

    @Step("???????? ?????? ?????????? ???????????? ???????????? ???????? ????????????")
    public ComposeSteps shouldSeeEmptyTextArea() {
        user.defaultSteps()
            .clicksIfCanOn(user.pages().ComposePopup().expandedPopup().toolbarBlock().turnOnFormattingBtn());
        assertThat(
            "???????? ?????? ?????????? ???????????? ???????????? ???????? ????????????",
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            hasText("")
        );
        return this;
    }

    @Step("???? ???????????? ???????? ?????????????? ?? ???????? ????????")
    public ComposeSteps shouldSeeEmptySendFieldTo() {
        assertThat("???????? ???????????? ???????????? ???????? ????????????", webDriverRule, hasClearAddress());
        return this;
    }

    @Step("???????? ???????????? ???????????? ?????????????????? ??????????: ??{0}??")
    public ComposeSteps shouldSeeTextAreaContains(String value) {
        shouldSeeFormattedTextAreaContains(value);
        return this;
    }

    @Step("???????? ???????????? ???????????? ?????????????????? ??????????: ??{0}??")
    public ComposeSteps shouldSeeFormattedTextAreaContains(String value) {
        assertThat(
            "???????? ?????? ?????????? ???????????? ??????????????????: " + value,
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            allOf((withWaitFor(isPresent())), hasText(containsString(value)))
        );
        return this;
    }

    @Step("???????? ???????????? ???? ???????????? ?????????????????? ??????????: ??{0}??")
    public ComposeSteps shouldNotSeeTextAreaContains(String value) {
        assertThat(
            "???????? ?????? ?????????? ???? ???????????? ??????????????????: " + value,
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            allOf((withWaitFor(isPresent())), not(hasText(containsString(value))))
        );
        return this;
    }

    @Step("???????? ???????????? ???????????? ?????????????????? ??????????: ??{0}??")
    public ComposeSteps shouldSeeSendToAreaContains(String value) {
        assertThat(
            "???????????????? ?????????? ?????? ?????????????????????? ????????????",
            user.pages().ComposePopup().yabbleTo().yabbleText(),
            withWaitFor(hasText(value))
        );
        return this;
    }

    @Step("???????? ???????????? ???????????? ?????????????????? ??????????: ??{0}??")
    public ComposeSteps shouldSeeSendToAreaHas(String... addresses) {
        assertThat("???????????????? ?????????? ?????? ?????????????????????? ????????????", webDriverRule, withWaitFor(hasAddresses(addresses)));
        return this;
    }

    @Step("???????? ???????????? ???? ???????????? ?????????????????? ??????????: ??{0}??")
    public ComposeSteps shouldNotSeeSendToAreaHas(String... addresses) {
        assertThat("???????? ?????????????? ???????????????? ?????????? ?????? ???????????????? ????????????", webDriverRule, not(hasAddresses(addresses)));
        return this;
    }

    @SkipIfFailed
    @Step("?????????????????? ???????????????????? ????????????")
    public ComposeSteps clicksOnTurnOffHtmlView() {
        assumeStepCanContinue(user.pages().ComposePage().composeToolbarBlock().turnOffFormattingBtn(), isPresent());
        user.defaultSteps().clicksOn(user.pages().ComposePage().composeToolbarBlock().turnOffFormattingBtn());
        return this;
    }

    @Step("???????????? ???????????? ???????????? ??????????????????")
    public ComposeSteps shouldSeeSendButton() {
        user.defaultSteps().shouldSee(user.pages().ComposePopup().expandedPopup().sendBtn());
        return this;
    }

    @Step("???????? ???????????? ???????????? ?????????????????? ??????????: {0}")
    public ComposeSteps shouldSeeSubject(String subject) {
        assertThat(
            "???????????????? ?????????? ?? ???????? subject",
            user.pages().ComposePopup().expandedPopup().sbjInput(),
            withWaitFor(hasValue(subject))
        );
        return this;
    }

    @Step("???????????? ???????????? ?????????????????? {1} ?? ???????? ????????????")
    public ComposeSteps shouldSeeMessageAsAttachment(int index, String subject) {
        String subjectCut = (subject.length() < 10) ? subject : subject.substring(0, 10);
        user.defaultSteps().shouldSee(user.pages().ComposePopup().expandedPopup().attachPanel().linkedAttach().get(index).attachEml());
        assertThat(
            "???????????????? ?????? ???????????????????????? ????????????",
            user.pages().ComposePopup().expandedPopup().attachPanel().linkedAttach().get(index).attachName(),
            hasText(containsString(subjectCut))
        );
        return this;
    }

    @Step("???????????? ???????????? ?????????????????? ?? ??????, ?????? ?????????? ?????????????? ???? ??????")
    public ComposeSteps shouldSeeNotificationAboutOtherAddresses() {
        assertThat(
            "???????????????? ?????????? ????????????????????????????!",
            user.pages().MessageViewPage().notificationAboutReply(),
            allOf(withWaitFor(isPresent()), hasText(containsString("?????? ?????????? ?????????????? ???? ?????? ?????????????????? ??????????????????")))
        );
        return this;
    }

    @Step("???????? ??CC?? ???????????? ?????????????????? ??????????: {0}")
    public ComposeSteps shouldSeeCCAreaContains(String text) {
        assertThat(
            "???????????????? ?????????? ?? ???? ????????????",
            user.pages().ComposePopup().yabbleCc().yabbleText(),
            hasText(text)
        );
        return this;
    }

    @Step("???????????? ???????????? ?????????? ?????????????? ???? ?????????????????????? ?? ??????, ?????? ?????????????? ???? ??????")
    public ComposeSteps shouldSeeNotificationTab() {
        user.defaultSteps().shouldSee(
            user.pages().MessageViewPage().replyNotification(),
            user.pages().MessageViewPage().replyNotification().replyLink(),
            user.pages().MessageViewPage().replyNotification().getReplyToAllLink()
        );
        return this;
    }

    @Step("???? ???????????? ???????????? ?????????? ?????????????? ???? ??????????????????????")
    public ComposeSteps shouldNotSeeNotificationTab() {
        assertThat(
            "?????????? ?????????????????????? ?????? ???????????????? ???????? ???????? ???? ????????????",
            user.pages().MessageViewPage().replyNotification(),
            not(isPresent())
        );
        return this;
    }

    @Step("???? ???????????? ???????? ???????????????????????????? ?? ???????????? ??????????????")
    public ComposeSteps shouldNotSeeNotificationAboutOtherAddresses() {
        assertThat(
            "???????????????????????????? ?? ???????????? ?????????????? ???????? ???? ????????????!",
            user.pages().MessageViewPage().notificationAboutReply(),
            not(isPresent())
        );
        return this;
    }

    @Step("???????????? ???????????? ?????????????????? ?? ???????????????????? ?? ????????????????")
    public ComposeSteps shouldSeeThatMessageIsSavedToDraft() {
        assertThat(
            "???? ?????????????????? ?????????????????? ?? ??????, ?????? ???????????? ?????????????????? ?? ??????????????????",
            user.pages().ComposePopup().expandedPopup().savedAt(),
            withWaitFor(isPresent(), SECONDS.toMillis(20))
        );
        assertThat(
            "???????????????? ?????????? ?????????????????? ?? ??????, ?????? ???????????? ?????????????????? ?? ??????????????????",
            user.pages().ComposePopup().expandedPopup().savedAt(),
            hasText(containsString("?????????????????? ??"))
        );
        return this;
    }

    @Step("???????? ???????? ???????????????????? ??????????????????")
    public ComposeSteps waitForMessageToBeSend() {
        user.defaultSteps().shouldNotSee(user.pages().ComposePopup().composePopup());
        return this;
    }

    @Step("???????????? ???????????? ?????????? ?? ???????????????? ??{0}??")
    public ComposeSteps shouldSeeTextWithSignature(String signature) {
        shouldSeeTextIn(signature);
        return this;
    }

    @Step("???????????? ???????????? ?????????? ?????????????? ??{0}??")
    private ComposeSteps shouldSeeTextIn(String signature) {
        assertThat(
            "?????????????????? ???????????? ??????????????",
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            hasText(signature)
        );
        return this;
    }

    @Step("?????????? {0} ???????????? ???????? ?????????????? ????????????")
    public ComposeSteps shouldSeeBoldText(String text) {
        assertThat(
            "???????????????? ???????????????????????????? ????????????",
            user.pages().ComposePage().textareaBlock().formattedText().boldText(),
            allOf(isPresent(), hasText(containsString(text)))
        );
        return this;
    }

    @Step("???????????? ???????????? ?????????????? ?? ??????????????????????????????")
    public ComposeSteps shouldSeeSignatureWithFormatting(String text) {
        user.defaultSteps().shouldSee(user.pages().ComposePopup().expandedPopup());
        assertThat(
            "?????????? ???????????? ???????? ??????????????????",
            user.pages().ComposePopup().expandedPopup().bodyInput().italicText(),
            allOf(isPresent(), hasText(text))
        );
        return this;
    }

    @Step("?????? ?? email ???????????? ?????????????????? ?? ??{0}?? ?? ??{1}?? ????????????????????????????")
    public ComposeSteps shouldSeeSuggestedNameAndEmail(String name, String email) {
        assertThat(
            "?????????????? ???? ????????????????",
            user.pages().ComposePage().suggestList(),
            withWaitFor(hasSize(greaterThan(0)))
        );
        assertThat(
            "???????????????? ?????? ?? ????????????????",
            user.pages().ComposePage().suggestList().get(0).contactName(),
            hasText(name)
        );
        assertThat(
            "???????????????? ?????????? ?? ????????????????",
            user.pages().ComposePage().suggestList().get(0).contactEmail(),
            hasText(email)
        );
        return this;
    }

    @Step("???????????????? ???????????? ?? ???????????????? ???????????? ?????????????????? ?? ??{0}??")
    public ComposeSteps shouldSeeSuggestedGroup(String name) {
        user.defaultSteps().waitInSeconds(1);
        assertThat(
            "?????????????? ???? ????????????????",
            user.pages().ComposePopup().suggestList(),
            hasSize(greaterThan(0))
        );
        assertThat(
            "???????????????? ?????? ???????????? ?? ????????????????",
            user.pages().ComposePopup().suggestList().get(0).contactName().getText(),
            containsString(name)
        );
        return this;
    }

    @Step("?????????????????? ???????????????????????? ??????????????")
    public ComposeSteps checkMessageAttachments(String... texts) {
        for (int i = 0; i < texts.length; i++) {
            user.defaultSteps().clicksOn(user.pages().ComposePage().forwardedMsgAttachBlock().get(i).messageLink())
                .switchOnWindow(i + 1);
            user.messageViewSteps().shouldSeeCorrectMessageText(texts[i]);
            user.defaultSteps().switchOnWindow(0);
        }
        return this;
    }

    @Step("??????????????????, ?????? ???????????? ?????????????????????? ?? ???????????????????? ?????????????? ???????? ?? ???????????? ??????????????")
    public ComposeSteps shouldSeeChangedOrderTemplatesButtons() {
        assertThat(
            "???? ?????????? ???????????? ???????????? ?????????????????? ?? ?????????????????? ????????????",
            user.pages().ComposePage().composeHead().buttonsList(),
            hasSize(2)
        );
        assertThat(
            "???????????? ???????? ???????????? ?????????????? (?????? ????????) ????????????",
            user.pages().ComposePage().composeHead().buttonsList().get(1),
            allOf(hasAttribute("class", containsString("js-save-button")))
        );
        return this;
    }

    @Step("???????????? ???????????? ?????????????? ?? ??????????????: ???{0}???")
    public ComposeSteps shouldSeeSignatureInList(String text) {
        List<String> signatures = extract(
            user.pages().ComposePopup().signaturesPopup().signaturesList(),
            ch.lambdaj.Lambda.on(MailElement.class).getText()
        );
        assertThat("???????????? ???????? ???????? ?????????????? ?? ???????????? ??????????????", signatures, hasItem(text));
        return this;
    }

    @Step("???????????????? ?? ?????????????????? ???????????????????? ????????")
    public ComposeSteps selectDateFromComposeCalendar() {
        user.defaultSteps().shouldSee(user.pages().ComposePopup().expandedPopup().calendar());
        assertThat(
            "?????? ???????????????????? ????????",
            user.pages().ComposePopup().expandedPopup().calendar().calendarDates(),
            hasSize(greaterThan(0))
        );
        user.defaultSteps()
            .clicksOn(user.pages().ComposePopup().expandedPopup().calendar().calendarDates().get(0))
            .clicksOn(user.pages().ComposePopup().expandedPopup().calendar().saveBtn());
        return this;
    }

    @Step("?????????????????? {0} ???????? ?? ??????????")
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

    @Step("???????????????????? ???????????? ?? ?????????????????? ???? Done")
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

    @Step("?????????????????? ?????????????? ?? Done ?? ???????????? ??????????")
    private ComposeSteps freezeDone() {
        user.defaultSteps().executesJavaScript(FREEZE_DONE_SCRIPT);
        return this;
    }

    @Step("???????????????????? ?????????? ??????????????")
    public ComposeSteps disableComposeAlert() {
        user.defaultSteps().executesJavaScript(DISABLE_COMPOSE_SCRIPT);
        return this;
    }

    @Step("?????? ???????????????????????????? ???????????? ?????? ?????????????? {1}  ???????????? ???????? {2}")
    public ComposeSteps shouldSeeAttachmentName(List<AttachElementsBlock> attachmentBlocks, int index,
                                                String expectedName) {
        String actualName = attachmentBlocks.get(index).attachName().getText();
        assertThat("?????????????????????? ?????? ???????????? ???? ?????????????????????????? ????????????????????", actualName, is(expectedName));
        return this;
    }

    @Step("?????????????????? ?????????????????? ???????? ?? ????????????")
    public ComposeSteps uploadLocalFile(WebElement element, String attachName) {
        element.sendKeys(user.defaultSteps().getAttachPath(attachName));
        return this;
    }

    @Step("???????????? ?????????? ?? ???????????????? c {0} ???? {1} ????????????")
    public ComposeSteps setTextBold(int from, int to) {
        user.defaultSteps().selectText(user.pages().ComposePage().textareaBlock().formattedText(), from, to)
            .clicksOn(user.pages().ComposePage().composeToolbarBlock().bold());
        return this;
    }

    @Step("???????????? ?????????? ?? ???????????????? c {0} ???? {1} ??????????????????")
    public ComposeSteps setTextItalic(int from, int to) {
        user.defaultSteps().selectText(user.pages().ComposePage().textareaBlock().formattedText(), from, to)
            .clicksOn(user.pages().ComposePage().composeToolbarBlock().italic());
        return this;
    }

    @Step("?????????????? ???????????????????????????? ???????????? ?? ???????????????? c {0} ???? {1}")
    public ComposeSteps clearTextFormatting(int from, int to) {
        user.defaultSteps().selectText(user.pages().ComposePage().textareaBlock().formattedText(), from, to)
            .clicksOn(user.pages().ComposePage().composeToolbarBlock().removeFormatting());
        return this;
    }

    @Step("???????????????????? ?????????? ?? ???????? ???????????? ???? ?????????????? {0}")
    public ComposeSteps appendTextToIndex(int index, String text) {
        Pattern nonBMP = Pattern.compile("[^\u0000-\uFFFF]"); //chromedriver ???? ?????????? ?????????????? ?????????????? ???????????? \uFFFF
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

    @Step("?????????????????? ???????????????? ?? ???????????????? ?????????????????? ????????????")
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

    @Step("?????????????????????? ?????? ???????????????????? ?? ?????????????????? ????????????")
    public void switchLayoutAndOpenCompose(String layout) {
        user.apiSettingsSteps()
            .callWithListAndParams("???????????????? " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        user.defaultSteps().refreshPage()
            .clicksOn(user.pages().MessagePage().composeButton());
    }

    @Step("?????????????????? ???????????? ?? ?????????????????? ???????? To, Subject ?? Body")
    public void openAndFillComposePopup(String msgTo, String msgSubject, String msgBody) {
        user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton())
            .inputsTextInElement(user.pages().ComposePopup().expandedPopup().popupTo(), msgTo)
            .inputsTextInElement(user.pages().ComposePopup().expandedPopup().sbjInput(), msgSubject)
            .inputsTextInElement(user.pages().ComposePopup().expandedPopup().bodyInput(), msgBody);
    }

    @Step("?????????????????? ???????? ?????????????????? ????????????????")
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

    @Step("?????????????????? ????????????")
    public ComposeSteps closeComposePopup() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().closeBtn())
            .shouldNotSee(user.pages().ComposePopup().expandedPopup());
        return this;
    }

    @Step("?????????????? ???? ???????????? ?????????????????????? ?? ?????????? ??????????????.")
    public ComposeSteps clicksOnSendBtn() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        return this;
    }

    @Step("?????????????? ???? ???????????? ?????????????????? ?? ???????? ?????????????????? ?????? ?????????????????? ????????????")
    public ComposeSteps clicksOnAddEmlBtn() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().composeAddEmlBtn());
        return this;
    }

    @Step("???????????????????? ???????????? ???????? ?? ??????????????")
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

    @Step("?????????????????? ????????????")
    public ComposeSteps openComposePopup() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().composeButton())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        return this;
    }

    @Step("???????????????????? ???????? cc/bcc")
    public ComposeSteps expandCcBcc() {
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().expandCollapseBtn());
        return this;
    }
}

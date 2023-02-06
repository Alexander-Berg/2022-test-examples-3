package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.CLEAR_SIGNS_AMOUNT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author mariya-murm
 */

@Aqua.Test
@Title("Новый комопз - Тесты на цитирование")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class ComposeQuotesTest extends BaseTest {

    private static final String MSG_TXT = "Compose_Quotes_Test";
    private static final String LONG_BODY = StringUtils.repeat("hello ", 2000);
    private String subj;

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
        subj = getRandomString();
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды у пользователя",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(lock.firstAcc().getSelfEmail(), subj, LONG_BODY);
        user.apiSettingsSteps().changeSignsAmountTo(CLEAR_SIGNS_AMOUNT);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Отмена действия не влияет на раскрытие цитирования")
    @TestCaseId("5132")
    public void shouldNotUndoRevealQuotes() {
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyButton())
            .shouldSee(onComposePopup().expandedPopup());
        user.composeSteps().revealQuotes();
        user.defaultSteps().appendTextInElement(
            user.pages().ComposePopup().expandedPopup().bodyInput(),
            MSG_TXT
        )
            .clicksOn(onComposePopup().expandedPopup().toolbarBlock().undo())
            .shouldNotSee(user.pages().ComposePopup().showQuote());
    }

    @Test
    @Title("Цитаты раскрыты в режиме Без оформления")
    @TestCaseId("5131")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68153")
    public void shouldNotSeeQuotesWithoutFormatting() {
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyButton())
            .shouldSee(onComposePopup().expandedPopup())
            .clicksOn(user.pages().ComposePage().composeToolbarBlock().turnOffFormattingBtn())
            .clicksOn(onComposePage().htmlFormattingOffPopup().continueButton())
            .shouldNotSee(user.pages().ComposePage().textareaBlock().showQuote());
    }

    @Test
    @Title("Ответить на письмо, не раскрывая цитаты в композе")
    @TestCaseId("5129")
    public void shouldSeeMessageTextWhenReplyWithClosedQuotes() {
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyButton())
            .shouldSee(onComposePopup().expandedPopup())
            .shouldSee(user.pages().ComposePopup().showQuote())
            .appendTextInElement(
                user.pages().ComposePopup().expandedPopup().bodyInput(),
                MSG_TXT
            );
        user.composeSteps().clearAddressFieldTo()
            .inputsAddressInFieldTo(DEV_NULL_EMAIL)
            .clicksOnSendButtonInHeader();
        user.leftColumnSteps().opensSentFolder();
        user.messagesSteps().clicksOnMessageWithSubject("Re: " + subj);
        user.messageViewSteps().shouldSeeCorrectMessageText(MSG_TXT);
        user.defaultSteps().shouldSee(user.pages().MessageViewPage().messageTextBlock().quotes().get(0));
    }

    @Test
    @Title("Ответить на письмо, раскрыв цитаты в композе")
    @TestCaseId("5129")
    public void shouldSeeMessageTextWhenReplyWithOpenedQuotes() {
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyButton())
            .shouldSee(onComposePopup().expandedPopup())
            .appendTextInElement(user.pages().ComposePopup().expandedPopup().bodyInput(), MSG_TXT);
        user.composeSteps().revealQuotes()
            .clearAddressFieldTo()
            .inputsAddressInFieldTo(DEV_NULL_EMAIL)
            .clicksOnSendButtonInHeader();
        user.leftColumnSteps().opensSentFolder();
        user.messagesSteps().clicksOnMessageWithSubject("Re: " + subj);
        user.messageViewSteps().shouldSeeCorrectMessageText(MSG_TXT);
        user.defaultSteps().shouldSee(user.pages().MessageViewPage().messageTextBlock().quotes().get(0));
    }

    @Test
    @Title("Цитирование в переводчике раскрывается при раскрытии цитаты в композе")
    @TestCaseId("5127")
    public void shouldSeeOpenedQuotesInCompose() {
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyButton())
            .shouldSee(onComposePopup().expandedPopup())
            .clicksOn(onComposePopup().expandedPopup().translateBtn())
            .clicksOn(user.pages().ComposePopup().showQuote())
            .shouldNotSee(user.pages().ComposePopup().showQuoteTranslate());
    }

    @Test
    @Title("Цитирование в композе раскрывается при раскрытии цитаты в переводчике")
    @TestCaseId("5127")
    public void shouldSeeOpenedQuotesInTranslate() {
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyButton())
            .shouldSee(onComposePopup().expandedPopup())
            .clicksOn(onComposePopup().expandedPopup().translateBtn())
            .clicksOn(user.pages().ComposePopup().showQuoteTranslate())
            .shouldNotSee(user.pages().ComposePopup().showQuote());
    }
}

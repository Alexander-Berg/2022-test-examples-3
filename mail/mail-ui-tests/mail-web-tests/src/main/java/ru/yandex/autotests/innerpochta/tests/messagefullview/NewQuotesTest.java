package ru.yandex.autotests.innerpochta.tests.messagefullview;

import io.qameta.allure.Step;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.QuoteBlock;
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

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.AVATARED_AUTHOR_EMAIL;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.FIRST_LEVEL_QUOTE_TEXT;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.FORTH_LEVEL_QUOTE_TEXT;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.MULTILEVEL_AVATAR_QUOTE;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.MULTILEVEL_EMPTY_FIRST_LEVEL_QUOTE;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.QUOTES_ORDER;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.QUOTE_AUTHOR_MONOGRAM_COLOR;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.QUOTE_FILE_NAME;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.QUOTE_MESSAGE_SUBJECT;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.SECOND_LEVEL_QUOTE_TEXT;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCROLL_PAGE_UP_SCRIPT_LIGHT;

@Aqua.Test
@Title("Цитаты")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.FULL_VIEW)
public class NewQuotesTest extends BaseTest {
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.imapSteps()
            .connectByImap()
            .addMessage(QUOTE_FILE_NAME)
            .closeConnection();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Цвет монограммной цитаты")
    @TestCaseId("6158")
    public void shouldSeeCorrectColor() {
        QuoteBlock quote = openMessageAndScrollToQuoteWithText(MULTILEVEL_AVATAR_QUOTE);
        user.defaultSteps().clicksOn(quote.showQuoteEarlier());
        assertEquals(
            QUOTE_AUTHOR_MONOGRAM_COLOR,
            quote.quoteAuthorAvatar().get(1).getCssValue("background-color")
        );
    }

    @Test
    @Title("Сворачивание цитаты кликом на аватарку")
    @TestCaseId("6163")
    public void shouldWrapQuoteByClickOnAuthorAvatar() {
        QuoteBlock quote = openMessageAndScrollToQuoteWithText(MULTILEVEL_AVATAR_QUOTE);
        user.defaultSteps()
            .clicksOn(quote.showQuoteEarlier())
            .onMouseHoverAndClick(quote.quoteAuthorAvatar().get(1))
            .shouldContainText(
                quote,
                FIRST_LEVEL_QUOTE_TEXT
            )
            .shouldNotContainText(
                quote,
                SECOND_LEVEL_QUOTE_TEXT
            );

    }

    @Test
    @Title("Запоминание состояния вложенных цитат при сворачивании")
    @TestCaseId("6164")
    public void shouldRememberWrappedQuotes() {
        QuoteBlock quote = openMessageAndScrollToQuoteWithText(MULTILEVEL_AVATAR_QUOTE);
        user.defaultSteps().clicksOn(quote.showFullQuote())
            .onMouseHoverAndClick(quote.quoteAuthorAvatar().get(3))
            .onMouseHoverAndClick(quote.quoteAuthorAvatar().get(1))
            .clicksOn(quote.showQuoteEarlier())
            .shouldContainText(
                quote,
                SECOND_LEVEL_QUOTE_TEXT
            )
            .shouldNotContainText(
                quote,
                FORTH_LEVEL_QUOTE_TEXT
            );
    }

    @Test
    @Title("Открываем предзаполненный композ по клику в имя автора цитаты")
    @TestCaseId("6168")
    public void shouldOpenComposeOnAuthorClick() {
        QuoteBlock quote = openMessageAndScrollToQuoteWithText(MULTILEVEL_AVATAR_QUOTE);
        user.defaultSteps().clicksOn(quote.quotesAuthors().get(0))
            .shouldSee(onComposePopup().expandedPopup())
            .shouldContainText(onComposePopup().expandedPopup().popupTo(), AVATARED_AUTHOR_EMAIL);
    }

    @Test
    @Title("По умолчанию разворачиваем первый непустой уровень цитаты")
    @TestCaseId("6170")
    public void shouldSeeMeaningfulLevelOfQuote() {
        QuoteBlock quote = openMessageAndScrollToQuoteWithText(MULTILEVEL_EMPTY_FIRST_LEVEL_QUOTE);
        user.defaultSteps().shouldContainText(quote, SECOND_LEVEL_QUOTE_TEXT);
    }

    @Step("Открываем письмо с цитатами, скролим до цитаты «{text}» и возвращаем элемент с ней")
    private QuoteBlock openMessageAndScrollToQuoteWithText(String text) {
        user.messagesSteps().clicksOnMessageWithSubject(QUOTE_MESSAGE_SUBJECT);
        user.defaultSteps()
            .scrollElementToTopOfView(onMessageView().messageTextBlock().certainText(text))
            .waitInSeconds(2)
            .executesJavaScript(SCROLL_PAGE_UP_SCRIPT_LIGHT);
        return onMessageView().messageTextBlock().quotes().get(Arrays.asList(QUOTES_ORDER).indexOf(text));
    }
}

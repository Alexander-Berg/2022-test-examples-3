package ru.yandex.autotests.innerpochta.tests.messageView;

import io.qameta.allure.Step;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.QuoteBlock;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.EMPTY_LINES_QUOTE;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.FORMATTED_QUOTE;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.MULTILEVEL_AVATAR_QUOTE;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.MULTILEVEL_NO_AUTHOR_QUOTE;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.ONE_LEVEL_NO_AUTHOR_QUOTE;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.QUOTES_ORDER;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.QUOTE_FILE_NAME;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.QUOTE_MESSAGE_SUBJECT;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.SUPER_MULTILEVEL_QUOTE;
import static ru.yandex.autotests.innerpochta.util.QuotesFileConsts.TWO_LEVEL_QUOTE;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCROLL_PAGE_SCRIPT_HIGH;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCROLL_PAGE_SCRIPT_MEDIUM;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCROLL_PAGE_UP_SCRIPT_LIGHT;

@Aqua.Test
@Title("Цитаты")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.FULL_VIEW)
public class QuotesTest {

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    @Rule
    public RuleChain chain = rules.createRuleChain();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AccLockRule lock = rules.getLock().useTusAccount();

    @Before
    public void setUp() {
        stepsProd.user().imapSteps()
            .connectByImap()
            .addMessage(QUOTE_FILE_NAME)
            .closeConnection();
    }

    @Test
    @Title("[Вёрстка] Многоуровневая цитата")
    @TestCaseId("6157")
    public void shouldSeeMultiLevelQuote() {
        Consumer<InitStepsRule> actions = st -> {
            openMessageAndScrollToQuoteWithText(st, MULTILEVEL_AVATAR_QUOTE);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("«Ранее в переписке» для одного уровня вложенности")
    @TestCaseId("6159")
    public void shouldSeePreviousQuoteInTwoLevelQuote() {
        Consumer<InitStepsRule> actions = st -> {
            openMessageAndScrollToQuoteWithText(st, TWO_LEVEL_QUOTE);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("[Вёрстка] Одноуровневая цитата без автора")
    @TestCaseId("6160")
    public void shouldSeeNoAuthorOneLevelQuote() {
        Consumer<InitStepsRule> actions = st -> {
            openMessageAndScrollToQuoteWithText(st, ONE_LEVEL_NO_AUTHOR_QUOTE);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("[Вёрстка] Многоуровневая цитата без автора")
    @TestCaseId("6161")
    public void shouldSeeNoAuthorMultiLevelQuote() {
        Consumer<InitStepsRule> actions = st -> {
            openMessageAndScrollToQuoteWithText(st, MULTILEVEL_NO_AUTHOR_QUOTE);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Подскролл к верхушке цитаты по клику в камыш")
    @TestCaseId("6165")
    public void shouldScrollToTopOfQuote() {
        Consumer<InitStepsRule> actions = st -> {
            QuoteBlock quote = openMessageAndScrollToQuoteWithText(st, MULTILEVEL_AVATAR_QUOTE);
            st.user().defaultSteps()
                .clicksOn(quote.showFullQuote())
                .executesJavaScript(SCROLL_PAGE_SCRIPT_MEDIUM)
                .clicksOn(quote.quoteLine().get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Горизонтальный скролл при большом количестве развернутых цитат")
    @TestCaseId("6166")
    public void shouldSeeScroll() {
        Consumer<InitStepsRule> actions = st -> {
            final int MINIMUM_QUOTE_WIDTH = 500;
            QuoteBlock quote = openMessageAndScrollToQuoteWithText(st, SUPER_MULTILEVEL_QUOTE);
            st.user().defaultSteps().executesJavaScript(SCROLL_PAGE_SCRIPT_HIGH);
            assertTrue(
                "Вложенная цитата ужата сильнее 500 пикселей!",
                quote.nestedQuote().get(quote.nestedQuote().size() - 1).getSize().width > MINIMUM_QUOTE_WIDTH
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Форматирование внутри цитаты сохраняется")
    @TestCaseId("6167")
    public void shouldSeeFormattedQuote() {
        Consumer<InitStepsRule> actions = st -> {
            openMessageAndScrollToQuoteWithText(st, FORMATTED_QUOTE);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Не показываем пустые строки в начале и конце цитаты")
    @TestCaseId("6169")
    public void shouldNotSeeEmptyLinesQuote() {
        Consumer<InitStepsRule> actions = st -> {
            openMessageAndScrollToQuoteWithText(st, EMPTY_LINES_QUOTE);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем письмо с цитатами, скролим до цитаты {text} и возвращаем элемент с ней")
    private QuoteBlock openMessageAndScrollToQuoteWithText(InitStepsRule st, String text) {
        st.user().messagesSteps().clicksOnMessageWithSubject(QUOTE_MESSAGE_SUBJECT);
        st.user().defaultSteps()
            .scrollElementToTopOfView(st.pages().mail().msgView().messageTextBlock().certainText(text))
            .waitInSeconds(2)
            .executesJavaScript(SCROLL_PAGE_UP_SCRIPT_LIGHT);
        return st.pages().mail().msgView().messageTextBlock().quotes()
            .get(Arrays.asList(QUOTES_ORDER).indexOf(text));
    }
}

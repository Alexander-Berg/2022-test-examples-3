package ru.yandex.autotests.innerpochta.tests.screentests.Search;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на поисковый саджест")
@Features(FeaturesConst.SEARCH)
@Stories(FeaturesConst.SUGGEST)
@RunWith(DataProviderRunner.class)
public class SearchSuggestScreenTest {

    private static final String SEARCH_REQUEST = "Яндекс";
    private static final String SYMBOL_AT = "@";
    private static final String REQUEST_WITH_DIFF_CASE = "яНДЕКс";
    private static final String SUBJ = "Ещё один яндекс";
    private static final String NO_SUBJECT = "No subject";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule accLock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), NO_SUBJECT, "");
        stepsProd.user().apiMessagesSteps().sendThread(accLock.firstAcc(), SEARCH_REQUEST, 3);
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), SUBJ, Utils.getRandomString());
    }

    @Test
    @Title("Должны увидеть саджест прошлых запросов при входе в поиск")
    @TestCaseId("795")
    public void shouldSeeZeroSuggest() {
        Consumer<InitStepsRule> actions = st -> {
            prepareSuggest(st, SEARCH_REQUEST);
            prepareSuggest(st, SUBJ);
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть поисковую выдачу по прошлому запросу")
    @TestCaseId("796")
    public void shouldSeeLastRequestResult() {
        Consumer<InitStepsRule> actions = st -> {
            prepareSuggest(st, SEARCH_REQUEST);
            st.user().defaultSteps().clicksOnElementWithText(
                st.pages().touch().search().searchSuggestItems().waitUntil(not(empty())),
                SEARCH_REQUEST
            )
                .shouldSee(st.pages().touch().search().messageBlock());
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны видеть прошлые запросы при фокусе в пустом поле поиска")
    @TestCaseId("751")
    public void shouldSeeLastRequestsBeforeInput() {
        Consumer<InitStepsRule> actions = st -> {
            prepareSuggest(st, SEARCH_REQUEST);
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().header().input())
                .shouldSee(st.pages().touch().search().suggest());
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть саджест для запроса нормального/другим регистром/неверным языке/при вводе спецсимвола")
    @TestCaseId("800")
    @DataProvider({SEARCH_REQUEST, REQUEST_WITH_DIFF_CASE, /*SYMBOL_AT - из-за QUINN-6512*/})
    public void shouldSeeSuggestForErrorRequest(String rqst) {
        Consumer<InitStepsRule> actions = st -> {
            inputSomethingIntoSuggest(st, rqst);
            st.user().defaultSteps().shouldSee(st.pages().touch().search().searchHighlights());
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("При выборе контакта/темы из саджеста поиск осуществляется по адресу/теме")
    @TestCaseId("809")
    public void shouldSeeSearchByContactFromSuggest() {
        Consumer<InitStepsRule> actions = st ->
            findSomethingFromSuggest(
                st,
                accLock.firstAcc().getLogin(),
                0
            );

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Не должны увидеть саджест при отсутствии прошлых поисковых запросов")
    @TestCaseId("752")
    public void shouldSeeEmptyZeroSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().search().header())
                .shouldNotSee(st.pages().touch().search().suggest());

        stepsProd.user().apiSearchSteps().cleanSuggestHistory();
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Step("Тыкаем в саджест, видим поисковую выдачу")
    private void findSomethingFromSuggest(InitStepsRule st, String rqst, int num) {
        inputSomethingIntoSuggest(st, rqst);
        st.user().defaultSteps()
            .clicksOn(st.pages().touch().search().searchSuggestItems().waitUntil(not(empty())).get(num))
            .shouldSee(st.pages().touch().search().messageBlock());
    }

    @Step("Вводим поисковый запрос")
    private void inputSomethingIntoSuggest(InitStepsRule st, String rqst) {
        st.user().defaultSteps().shouldNotSee(st.pages().touch().messageList().bootPage())
            .clicksAndInputsText(st.pages().touch().search().header().input(), rqst)
            .shouldSee(st.pages().touch().search().groupSuggestTitle().waitUntil(not(empty())));
    }

    @Step("Подготавливаем юзеру историю запросов")
    private void prepareSuggest(InitStepsRule st, String rqst) {
        st.user().defaultSteps().clicksAndInputsText(st.pages().touch().search().header().input(), rqst)
            .doubleClick(st.pages().touch().search().header().find())
            .opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart())
            .shouldContainText(st.pages().touch().search().suggest(), rqst);
    }
}

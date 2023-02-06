package ru.yandex.autotests.innerpochta.tests.autotests.Search;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на поисковый саджест")
@Features(FeaturesConst.SEARCH)
@Stories(FeaturesConst.SUGGEST)
public class SearchSuggestTest {

    private static final String SEARCH_REQUEST = "Яндекс";
    private static final String SEARCH_REQUEST_LONG = "ЯндексTest";
    private static final int SUGGEST_SIZE = 6;
    private static final String REQUEST_WITH_DIFF_CASE = "яНДЕКс";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps()
            .sendMailWithNoSave(accLock.firstAcc(), SEARCH_REQUEST, Utils.getRandomString());
        steps.user().apiMessagesSteps()
            .sendMailWithNoSave(accLock.firstAcc(), SEARCH_REQUEST_LONG, Utils.getRandomString());
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart());
        inputRqstAndFind(SEARCH_REQUEST);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().search().header().clean());
        inputRqstAndFind(SEARCH_REQUEST_LONG);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart());
    }

    @Test
    @Title("Последний выбранный запрос перемещается наверх списка саджеста")
    @TestCaseId("807")
    public void shouldSeeMsgLastRequestOnTheTop() {
        cleanInputAndCheckFirstSuggest(SEARCH_REQUEST_LONG);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().search().searchSuggestItems().get(1))
            .clicksOn(steps.pages().touch().search().header().clean());
        cleanInputAndCheckFirstSuggest(SEARCH_REQUEST);
    }

    @Test
    @Title("Запросы, по которым ничего не найдено, не добавляются в саджест")
    @TestCaseId("805")
    public void shouldNotSeeInSuggestEmptyRequests() {
        String noResultRqst = Utils.getRandomString();
        steps.user().defaultSteps()
            .clicksAndInputsText(steps.pages().touch().search().header().input(), noResultRqst)
            .clicksOn(steps.pages().touch().search().header().find())
            .shouldSee(steps.pages().touch().search().emptySearchResultImg())
            .clicksOn(steps.pages().touch().search().header().clean())
            .shouldNotContainText(steps.pages().touch().search().searchSuggestItems().get(0), noResultRqst);
    }

    @Test
    @Title("Должны вернуться от поискового саджеста к списку писем")
    @TestCaseId("797")
    public void shouldBackFromSuggestToMsgList() {
        steps.user().defaultSteps()
            .clicksAndInputsText(steps.pages().touch().search().header().input(), SEARCH_REQUEST)
            .clicksOn(steps.pages().touch().search().header().back())
            .shouldSee(steps.pages().touch().messageList().headerBlock());
    }

    @Test
    @Title("Одинаковые запросы, введённые разным регистром, не должны попадать в саджест")
    @TestCaseId("806")
    public void shouldNotAddToSuggestSameRqsts() {
        steps.user().defaultSteps()
            .shouldContainText(steps.pages().touch().search().suggest(), SEARCH_REQUEST)
            .clicksAndInputsText(steps.pages().touch().search().header().input(), REQUEST_WITH_DIFF_CASE)
            .shouldSee(steps.pages().touch().search().groupSuggestTitle())
            .clicksOn(steps.pages().touch().search().searchSuggestItems().waitUntil(not(empty())).get(0))
            .shouldSee(steps.pages().touch().search().messageBlock())
            .clicksOn(steps.pages().touch().search().header().clean())
            .waitInSeconds(1) //для обновления саджеста
            .shouldNotContainText(steps.pages().touch().search().suggest(), REQUEST_WITH_DIFF_CASE);
    }

    @Test
    @Title("Саджет меняется при вводе запроса")
    @TestCaseId("753")
    public void shouldChangeSuggest() {
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().bootPage())
            .clicksAndInputsText(steps.pages().touch().search().header().input(), SEARCH_REQUEST)
            .shouldSeeElementsCount(steps.pages().touch().search().searchSuggestItems(), 2)
            .clicksAndInputsText(steps.pages().touch().search().header().input(), "T")
            .shouldSeeElementsCount(steps.pages().touch().search().searchSuggestItems(), 1);
    }

    @Test
    @Title("Проверяем, что появляется саджест поиска")
    @TestCaseId("381")
    public void shouldSuggestPrevious() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart())
            .clicksOn(steps.pages().touch().search().header().input())
            .shouldSee(steps.pages().touch().search().suggest());
    }

    @Step("Очищаем поля ввода, проверяем первую строку в саджесте")
    private void cleanInputAndCheckFirstSuggest(String rqst) {
        steps.user().defaultSteps()
            .waitInSeconds(1)
            .shouldContainText(
                steps.pages().touch().search().searchSuggestItems().get(0),
                rqst
            );
    }

    @Step("Очищаем поля ввода, проверяем первую строку в саджесте")
    private void inputRqstAndFind(String rqst) {
        steps.user().defaultSteps().clicksAndInputsText(steps.pages().touch().search().header().input(), rqst)
            .doubleClick(steps.pages().touch().search().header().find())
            .shouldSee(steps.pages().touch().search().messageBlock());
    }
}

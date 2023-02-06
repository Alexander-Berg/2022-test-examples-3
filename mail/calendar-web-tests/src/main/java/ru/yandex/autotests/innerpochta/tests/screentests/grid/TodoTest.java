package ru.yandex.autotests.innerpochta.tests.screentests.grid;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.COLUMN_CENTER;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TIME_11AM;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на todo")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.TODO_BLOCK)
@RunWith(DataProviderRunner.class)
@Description("У юзера подготовлено очень много различных дел")
public class TodoTest {

    private static final String LAST_WEEK = "/week?show_date=2017-04-10";
    private static final String TODO_ITEMS_WEEK = "/week?show_date=2017-05-01";

    private static final String MAIN_URL = "/day?sidebar=todo";
    private static final String COMPLETED_URL = "/month?sidebar=todo&sidebarTab=expired";
    private static final String EXPIRED_URL = "/week?sidebar=todo&sidebarTab=completed";

    private static final String CREDS = "EmptyUser";
    private static final String CREDS_2 = "TodoTest";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().names(CREDS, CREDS_2));
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain();

    @Test
    @Title("Открываем пустую тудушку")
    @TestCaseId("478")
    public void shouldSeeEmptyTodo() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().openTodoBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(LAST_WEEK).run();
    }

    @Test
    @Title("Открываем туду с просроченными делами")
    @TestCaseId("500")
    public void shouldSeeExpiredListsInTodo() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().openTodoBtn());

        parallelRun.withActions(actions).withAcc(lock.acc(CREDS_2)).withUrlPath(LAST_WEEK).run();
    }

    @Test
    @Title("Сортировка просроченных дел")
    @TestCaseId("494")
    public void shouldSeeCorrectSortExpiredLists() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().openTodoBtn())
            .clicksOn(st.pages().cal().home().todo().expiredListBtn());

        parallelRun.withActions(actions).withAcc(lock.acc(CREDS_2)).withUrlPath(LAST_WEEK).run();
    }

    @Test
    @Title("Сортировка выполненных дел")
    @TestCaseId("494")
    public void shouldSeeCorrectSortCompletedLists() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().openTodoBtn())
            .clicksOn(st.pages().cal().home().todo().completedListBtn());

        parallelRun.withActions(actions).withAcc(lock.acc(CREDS_2)).withUrlPath(LAST_WEEK).run();
    }

    @Test
    @Title("Должны видеть попап под паранджой, сайдбар закрывается")
    @TestCaseId("477")
    public void shouldSeeOnlyTodo() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .offsetClick(st.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .clicksOn(st.pages().cal().home().leftPanel().addCal())
            .shouldSee(
                st.pages().cal().home().newEventPopup(),
                st.pages().cal().home().addCalSideBar())
            .clicksOn(st.pages().cal().home().openTodoBtn())
            .shouldNotSee(st.pages().cal().home().addCalSideBar());

        parallelRun.withActions(actions).withAcc(lock.acc(CREDS_2)).run();
    }

    @Test
    @Title("Попап дел в сетке")
    @TestCaseId("581")
    public void shouldSeeTodoListPopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().todoItemsList().get(0))
            .shouldSee(st.pages().cal().home().todoItemPopup());

        parallelRun.withActions(actions).withAcc(lock.acc(CREDS_2)).withUrlPath(TODO_ITEMS_WEEK).run();
    }

    @Test
    @Title("Открываем тудушку по урлу")
    @DataProvider({MAIN_URL, COMPLETED_URL, EXPIRED_URL})
    @TestCaseId("596")
    public void shouldSeeTodo(String urlPath) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().cal().home().todo());

        parallelRun.withActions(actions).withAcc(lock.acc(CREDS_2)).withUrlPath(urlPath).run();
    }
}

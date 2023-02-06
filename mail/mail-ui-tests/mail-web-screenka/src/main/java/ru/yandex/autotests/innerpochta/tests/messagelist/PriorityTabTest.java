package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL360_PAID;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.PRIORITY_TAB;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Почта 360 - Таб «Главное»")
@Features(FeaturesConst.LEFT_PANEL)
@Tag(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.TABS)
@RunWith(DataProviderRunner.class)
public class PriorityTabTest {

    private String sbj = getRandomString();

    private static final String DARK_THEME_QUERY_PARAM = "?theme=lamp";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(MAIL360_PAID);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Включаем табы и таб «Главное»",
                of(
                    PRIORITY_TAB, TRUE,
                    FOLDER_TABS, TRUE
                )
            );
        stepsProd.user().apiMessagesSteps().sendThread(lock.firstAcc(), sbj, 5);
        stepsProd.user().apiLabelsSteps().unPriorityLetters();
    }

    @Test
    @Title("Верстка таба «Главное»")
    @TestCaseId("5974")
    public void shouldSeePriorityTab() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().displayedMessages().list().get(0).priorityMark())
                .shouldSee(st.pages().mail().home().priorityTab());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отображение таба «Главное» в темной теме")
    @TestCaseId("5974")
    public void shouldSeePriorityTabInDarkTheme() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().displayedMessages().list().get(0).priorityMark())
                .shouldSee(st.pages().mail().home().priorityTab());

        parallelRun.withActions(actions).withUrlPath(DARK_THEME_QUERY_PARAM).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Помечаем тред приоритетной меткой")
    @TestCaseId("5976")
    public void shouldMarkAllThreadPriority() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().displayedMessages().list().get(0).priorityMark())
                .clicksOn(st.pages().mail().home().priorityTab());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

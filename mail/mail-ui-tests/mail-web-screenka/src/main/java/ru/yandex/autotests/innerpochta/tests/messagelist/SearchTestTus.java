package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Поиск")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.SEARCH)
@RunWith(DataProviderRunner.class)
public class SearchTestTus {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORED_ELEMENTS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @DataProvider
    public static Object[][] urls() {
        return new Object[][]{
            {QuickFragments.INBOX, QuickFragments.SENT},
            {QuickFragments.SETTINGS, QuickFragments.INBOX}
        };
    }

    @Test
    @Title("Открываем поиск")
    @TestCaseId("2851")
    public void shouldSeeOpenedSearch() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .shouldSee(st.pages().mail().search().mail360HeaderBlock().closeSearch());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Закрываем поиск")
    @TestCaseId("2852")
    public void shouldSeeClosedSearch() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .inputsTextInElement(st.pages().mail().search().mail360HeaderBlock().searchInput(), getRandomString())
                .clicksOn(st.pages().mail().search().mail360HeaderBlock().searchInput())
                .clicksOn(st.pages().mail().search().mail360HeaderBlock().closeSearch())
                .clicksOn(st.pages().mail().search().mail360HeaderBlock().closeSearch())
                .shouldNotSee(st.pages().mail().search().mail360HeaderBlock().closeSearch());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем расширенный поиск")
    @TestCaseId("2853")
    public void shouldSeeAdvancedSearch() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchOptionsBtn())
                .shouldSee(st.pages().mail().search().advancedSearchBlock());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем дату в расширенном поиске")
    @TestCaseId("2854")
    public void shouldSeeMiniDateInSearch() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchOptionsBtn())
                .shouldSee(st.pages().mail().search().advancedSearchBlock())
                .clicksOn(st.pages().mail().search().advancedSearchBlock().advancedSearchRows().get(1))
                .clicksOn(st.pages().mail().search().dataRangeInputs().get(0))
                .shouldSee(st.pages().mail().search().calendar());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }


    @Test
    @Title("Переходим из разных папок с развернутым поиском")
    @TestCaseId("4276")
    @UseDataProvider("urls")
    public void shouldSeeFoldedSearchAfterTransition(QuickFragments from, QuickFragments to) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().opensFragment(from)
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .opensFragment(to)
                .shouldSee(st.pages().mail().home().mail360HeaderBlock().foldedSearch());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Переходим из «Контактов» во «Входящие» с развернутым поиском")
    @TestCaseId("4276")
    public void shouldSeeFoldedSearchAfterTransitionFromContacts() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().opensFragment(QuickFragments.CONTACTS)
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchContactInput())
                .opensFragment(QuickFragments.INBOX)
                .shouldSee(st.pages().mail().home().mail360HeaderBlock().foldedSearch());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

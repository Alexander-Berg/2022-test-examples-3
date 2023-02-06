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
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_RQST;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.SENT_RU;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на расширенный поиск")
@Features(FeaturesConst.SEARCH)
@Stories(FeaturesConst.FILTERS)
@RunWith(DataProviderRunner.class)
public class AdvancedSearchScreenTest {

    private static final String SEARCH_INPUT = "test";

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
        stepsProd.user().apiMessagesSteps().sendMail(accLock.firstAcc(), SEARCH_INPUT, "");
    }

    @Test
    @Title("Видим попап с фильтрами на планшетах")
    @TestCaseId("1479")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeAllFiltersTablet() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFiltersBtn())
                .shouldSee(st.pages().touch().search().advancedSearchFiltersPopup());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим выбранный фильтр выделенным")
    @TestCaseId("1480")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeActiveFilter() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(0))
                .shouldSee(st.pages().touch().search().advancedSearchActiveFilter());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим выбранный фильтр выделенным")
    @TestCaseId("1480")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeActiveFilterTablet() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFiltersBtn())
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(0))
                .shouldSee(st.pages().touch().search().advancedSearchActiveFilter());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с выбором даты")
    @TestCaseId("1481")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeDatesPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(1))
                .shouldSee(st.pages().touch().search().advancedSearchDatesPopup());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с выбором даты")
    @TestCaseId("1481")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeDatesPopupTablet() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFiltersBtn())
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(1))
                .shouldSee(st.pages().touch().search().advancedSearchDatesPopup());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с выбором папки")
    @TestCaseId("1482")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeFoldersPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
                .shouldSee(st.pages().touch().search().advancedSearchFolderPopup());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с выбором папки")
    @TestCaseId("1482")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeFoldersPopupTablet() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFiltersBtn())
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
                .shouldSee(st.pages().touch().search().advancedSearchFolderPopup());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с выбором контактов")
    @TestCaseId("1483")
    @DataProvider({"3", "4"})
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeContactsPopup(int num) {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(num))
                .shouldSee(st.pages().touch().search().advancedSearchContactsPopup());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с выбором контактов")
    @TestCaseId("1483")
    @DataProvider({"3", "4"})
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeContactsPopupTablet(int num) {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFiltersBtn())
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(num))
                .shouldSee(st.pages().touch().search().advancedSearchContactsPopup());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с остальными фильтрами")
    @TestCaseId("1487")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeMorePopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(7))
                .shouldSee(st.pages().touch().search().advancedSearchMorePopup());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с остальными фильтрами")
    @TestCaseId("1487")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeMorePopupTablet() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFiltersBtn())
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(5))
                .shouldSee(st.pages().touch().search().advancedSearchMorePopup());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с деревом папок")
    @TestCaseId("1491")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeFolderTreeInFolderPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
                .shouldSee(st.pages().touch().search().advancedSearchFolderPopup());

        stepsProd.user().apiFoldersSteps().createNewSubFolder(
            Utils.getRandomName(),
            stepsProd.user().apiFoldersSteps().createNewSubFolder(
                Utils.getRandomName(),
                stepsProd.user().apiFoldersSteps().createNewSubFolder(
                    Utils.getRandomName(),
                    stepsProd.user().apiFoldersSteps().createNewFolder(Utils.getRandomName())
                )
            )
        );
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Видим попап с деревом папок")
    @TestCaseId("1491")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeFolderTreeInFolderPopupTablet() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFiltersBtn())
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
                .shouldSee(st.pages().touch().search().advancedSearchFolderPopup());

        stepsProd.user().apiFoldersSteps().createNewSubFolder(
            Utils.getRandomName(),
            stepsProd.user().apiFoldersSteps().createNewSubFolder(
                Utils.getRandomName(),
                stepsProd.user().apiFoldersSteps().createNewSubFolder(
                    Utils.getRandomName(),
                    stepsProd.user().apiFoldersSteps().createNewFolder(Utils.getRandomName())
                )
            )
        );
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Должны видеть отфильтрованный список папок в фильтре по папкам")
    @TestCaseId("1493")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldFindFolderInFolderPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
                .clicksOn(st.pages().touch().search().headerInFolderPopup())
                .inputsTextInElement(st.pages().touch().search().inputInFolderPopup(), SENT_RU)
                .shouldSeeElementsCount(st.pages().touch().search().foldersInPopupFolder(), 1);

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }

    @Test
    @Title("Должны видеть отфильтрованный список папок в фильтре по папкам")
    @TestCaseId("1493")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldFindFolderInFolderPopupTablet() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().advancedSearchFiltersBtn())
                .clicksOn(st.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
                .clicksOn(st.pages().touch().search().headerInFolderPopup())
                .inputsTextInElement(st.pages().touch().search().inputInFolderPopup(), SENT_RU)
                .shouldSeeElementsCount(st.pages().touch().search().foldersInPopupFolder(), 1);

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))).run();
    }
}

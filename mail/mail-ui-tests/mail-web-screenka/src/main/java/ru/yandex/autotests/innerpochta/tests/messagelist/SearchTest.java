package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
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
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.REACT_COMPOSE_DISABLE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Поиск")
@Features({FeaturesConst.MESSAGE_LIST, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.SEARCH)
@RunWith(DataProviderRunner.class)
@Description("Юзер поиска подписан на рассылки и есть прошлые запросы")
public class SearchTest {

    private static final String CREDS_FOR_PREVIOUS_SEARCH = "TopResultsTest";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
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
            {QuickFragments.SETTINGS, QuickFragments.INBOX},
            {QuickFragments.COMPOSE, QuickFragments.INBOX}
        };
    }

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ 5 писем на странице и отключаем компактный вид",
            of(
                SETTINGS_PARAM_MESSAGES_PER_PAGE, 5,
                LIZA_MINIFIED_HEADER, EMPTY_STR,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                REACT_COMPOSE_DISABLE, TRUE
            )
        );
    }

    @Test
    @UseCreds(CREDS_FOR_PREVIOUS_SEARCH)
    @Title("Переходим в прошлый запрос")
    @TestCaseId("2855")
    public void shouldSeeLastQueries() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .shouldSee(st.pages().mail().search().searchSuggest())
                .clicksOn(st.pages().mail().search().lastQueriesList().get(0))
                .shouldSee(st.pages().mail().home().displayedMessages());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Тулбар в поисковой выдаче без выбора вариантов поиска")
    @TestCaseId("4759")
    public void shouldSeeQueryStringWithNoSelectedMsg() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем компактный режим и вертикальный 3-пейн",
            of(
                LIZA_MINIFIED_HEADER, STATUS_ON,
                SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL
            )
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().searchBtnCompactMode())
                .clicksOn(st.pages().mail().search().lastQueriesList().get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Тулбар в поисковой выдаче при выборе варианта поиска")
    @TestCaseId("4759")
    public void shouldSeeActiveToolbarWithNoSelectedMsg() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем компактный режим и вертикальный 3-пейн",
            of(
                LIZA_MINIFIED_HEADER, STATUS_ON,
                SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL
            )
        );
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().searchBtnCompactMode())
                .clicksOn(st.pages().mail().search().lastQueriesList().get(0));
            st.user().messagesSteps().clicksOnMessageByNumber(0);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

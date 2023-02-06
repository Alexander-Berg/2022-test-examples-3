package ru.yandex.autotests.innerpochta.tests.setting;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
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

import static com.google.common.collect.ImmutableBiMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Вёрстка управления рассылками")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SUBSCRIPTIONS)
@RunWith(DataProviderRunner.class)
public class SubscriptionsScreenTest {

    private static final String LAMODA_MAILLIST = "[{\"displayName\": \"Lamoda\", \"messageType\": 13, \"email\": " +
        "\"newsletter@info.lamoda.ru\", \"folderId\": \"3\"}]";
    private static final String BERU_MAILLIST = "[{\"displayName\": \"Беру\", \"messageType\": 13, \"email\": " +
        "\"news@beru.ru\", \"folderId\": \"3\"}]";
    private static final String USER_WITHOUT_SUBSCRIPTIONS = "NoSubscriptionsTest";
    private static final String COLORFUL_THEME = "colorful";
    private static final String NIGHT_THEME = "lamp";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private AccLockRule lockWithoutSubscriptions = AccLockRule.use().names(USER_WITHOUT_SUBSCRIPTIONS);
    private RestAssuredAuthRule authWithoutSubscriptions = RestAssuredAuthRule.auth(lockWithoutSubscriptions);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(lockWithoutSubscriptions)
        .around(authWithoutSubscriptions);

    @DataProvider
    public static Object[][] data() {
        return new Object[][]{
            {"Активные", COLORFUL_THEME},
            {"Скрытые", NIGHT_THEME}
        };
    }

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем светлую тему",
            of(COLOR_SCHEME, COLORFUL_THEME)
        );
        stepsProd.user().apiFiltersSteps().deleteAllUnsubscribeFilters();
        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(TRASH, INBOX);
    }

    @Test
    @Title("Открываем попап рассылок из выпадушки настроек в светлых и темных темах")
    @TestCaseId("5001")
    @UseDataProvider("data")
    public void shouldSeeSubscriptionSettingsPopUpFromHeader(String tabName, String theme) {
        Consumer<InitStepsRule> actions = st -> st.user().settingsSteps().openSubscriptionsSettings();

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем нужную тему",
            of(COLOR_SCHEME, theme)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Скролл не пропадает при переходе в список рассылок")
    @TestCaseId("5016")
    public void shouldSeeScrollWhenReturnedToMLList() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps()
                .onMouseHoverAndClick(st.pages().mail().settingsSubscriptions().subscriptions().get(1))
                .clicksOn(st.pages().mail().settingsSubscriptions().backFromSubsView());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка длинного имени рассылки в попапе «Вы уверены?»")
    @TestCaseId("5015")
    public void shouldSeeCorrectViewOfLongNameInUnsubscribe() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps()
                .onMouseHoverAndClick(st.pages().mail().settingsSubscriptions().subscriptions().get(0))
                .clicksOn(st.pages().mail().settingsSubscriptions().subsViewBtn())
                .shouldSee(st.pages().mail().settingsSubscriptions().confirmPopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка попапа без рассылок в светлых и темных темах")
    @TestCaseId("5022")
    @UseDataProvider("data")
    public void shouldSeeEmptyUnsubscribeList(String tabName, String theme) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps().clicksOnElementWithText(st.pages().mail().settingsSubscriptions().tabs(), tabName);
        };
        stepsProd.user().apiSettingsSteps().withAuth(authWithoutSubscriptions).callWithListAndParams(
            "Включаем нужную тему",
            of(COLOR_SCHEME, theme)
        );
        parallelRun.withActions(actions).withAcc(lockWithoutSubscriptions.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка попапа об успешной отписке")
    @TestCaseId("5260")
    public void shouldSeeSuccessUnsubscribePopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps()
                .onMouseHoverAndClick(st.pages().mail().settingsSubscriptions().subscriptions().get(0))
                .clicksOn(st.pages().mail().settingsSubscriptions().subsViewBtn())
                .clicksOn(st.pages().mail().settingsSubscriptions().confirmPopupBtn());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка попапа об успешной подписке")
    @TestCaseId("5259")
    public void shouldSeeSuccessSubscribePopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().settingsSubscriptions().tabHidden())
                .onMouseHoverAndClick(st.pages().mail().settingsSubscriptions().subscriptions().get(0))
                .clicksOn(st.pages().mail().settingsSubscriptions().subsViewBtn())
                .clicksOn(st.pages().mail().settingsSubscriptions().confirmSubscribePopupBtn());
        };
        stepsProd.user().apiFiltersSteps().createUnsubscribeFilters(LAMODA_MAILLIST, BERU_MAILLIST);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны выделить все рассылки в табе")
    @TestCaseId("6311")
    public void shouldCheckAllSubs() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps().clicksOn(st.pages().mail().settingsSubscriptions().selectAll())
                .shouldSeeElementsCount(st.pages().mail().settingsSubscriptions().subsCheckedCheckboxes(), 2);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Чекбокс «Выделить все» исчезает, если поиск по рассылкам ничего не нашёл")
    @TestCaseId("6312")
    public void shouldNotSeeSelectAllIfEmptySearch() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().settingsSteps().openSubscriptionsSettings();
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().settingsSubscriptions().search(), getRandomString())
                .shouldSeeElementsCount(st.pages().mail().settingsSubscriptions().subscriptions(), 0);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

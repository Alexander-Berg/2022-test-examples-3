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
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Шапка почты")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class HeaderTest {

    private static final String COLORFUL_THEME = "colorful";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(3);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @DataProvider
    public static Object[][] userInterfaces() {
        return new Object[][]{
            {"Включаем 3pane/vertical", SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL},
            {"Включаем 3pane/horizontal", SETTINGS_PARAM_LAYOUT, SETTINGS_LAYOUT_3PANE_HORIZONTAL},
            {"Включаем компактную шапку", LIZA_MINIFIED_HEADER, STATUS_ON}
        };
    }

    @Test
    @Title("Нажимаем на «Все сервисы» в шапке")
    @TestCaseId("2740")
    public void shouldOpenMoreServicesDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().moreServices())
                .shouldSee(st.pages().mail().home().allServices360Popup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку залогина")
    @TestCaseId("2741")
    public void shouldOpenUserMenuDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .shouldSee(st.pages().mail().home().userMenuDropdown());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Аватарки пользователей в выпадушке залогина отображаются друг под другом")
    @TestCaseId("4171")
    public void shouldSeeCorrectUserAvatarsInUserMenuDropdown() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().loginSteps().multiLoginWith(lock.accNum(0), lock.accNum(2));
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .shouldSee(st.pages().mail().home().userMenuDropdown());
        };
        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }

    @Test
    @Title("Нажимаем на шестеренку в шапке")
    @TestCaseId("2850")
    public void shouldSeeSettingsPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().settingsMenu())
                .shouldSee(st.pages().mail().home().mainSettingsPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку тем")
    @TestCaseId("2810")
    public void shouldSeeThemeDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().changeThemeBtn())
                .shouldSee(st.pages().mail().home().changeThemeBlock());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должна быть кнопка «Все сервисы» в бургере")
    @TestCaseId("3938")
    public void shouldOpenAllServicesBtn() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().moreServices())
                .shouldSee(st.pages().mail().home().allServices360Popup())
                .waitInSeconds(1)
                .shouldSee(st.pages().mail().home().allServices360Popup().allServices360());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем названия активной темы и темы по ховеру")
    @TestCaseId("2678")
    public void shouldSeeNameOnActiveThemeAndThemeOnHover() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiSettingsSteps()
                .callWithListAndParams(
                    "Включаем цветную тему с дефолтным цветом",
                    of(COLOR_SCHEME, COLORFUL_THEME)
                );
            st.user().defaultSteps().refreshPage()
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().changeThemeBtn())
                .shouldSee(st.pages().mail().home().changeThemeBlock())
                .onMouseHover(st.pages().mail().home().changeThemeBlock().lampTheme());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отображение аватарки в разных режимах почты")
    @TestCaseId("3567")
    @UseDataProvider("userInterfaces")
    public void shouldSeeAvatar3paneVertical(String description, String type, String version) {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().refreshPage()
                .shouldSee(st.pages().mail().home().mail360HeaderBlock().userAvatar());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(description, of(type, version));
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Аккаунты подсвечиваются по ховеру")
    @TestCaseId("4282")
    public void shouldSeeHoverOnAccs() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().loginSteps().multiLoginWith(lock.accNum(0), lock.accNum(2));
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .onMouseHover(st.pages().mail().home().userMenuDropdown().userList().get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }

    @Test
    @Title("Аккаунты, в которые переходили, подсвечиваются по ховеру")
    @TestCaseId("4282")
    public void shouldSeeHoverOnUsedAccs() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().loginSteps().multiLoginWith(lock.accNum(1));
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .onMouseHoverAndClick(st.pages().mail().home().userMenuDropdown().userList().get(0))
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .onMouseHoverAndClick(st.pages().mail().home().userMenuDropdown().userList().get(0))
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .onMouseHover(st.pages().mail().home().userMenuDropdown().userList().get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }
}

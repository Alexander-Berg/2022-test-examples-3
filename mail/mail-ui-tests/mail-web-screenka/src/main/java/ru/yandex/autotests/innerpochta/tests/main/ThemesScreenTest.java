package ru.yandex.autotests.innerpochta.tests.main;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.ThemeIndexes.WEATHER_THEME;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_GEO_ID;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

@Aqua.Test
@Title("Вёрстка тем")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.GENERAL)
public class ThemesScreenTest {

    private static final String NEW_CITY_NAME = "Барселона";
    private static final String INITIAL_CITY_ID = "2";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsTest.user().apiSettingsSteps().callWithListAndParams(
            "Устанавливаем изначальный город, включаем 2pane",
            of(
                SETTINGS_GEO_ID, INITIAL_CITY_ID,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE
            )
        );
    }

    @Test
    @Title("Проверяем корректность смены фона и температуры при смене города в погодной теме")
    @TestCaseId("2227")
    public void shouldChangeBackgroundAndTemperatureWeatherTheme() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().changeThemeBtn())
                .shouldSee(st.pages().mail().home().changeThemeBlock())
                .clicksOn(st.pages().mail().home().changeThemeBlock().allThemesList().get(WEATHER_THEME.index()));
            st.user().hotkeySteps()
                .pressHotKeys(
                    st.pages().mail().home().changeThemeBlock().cityInput(),
                    Keys.chord(Keys.CONTROL, "a")
                )
                .pressHotKeys(st.pages().mail().home().changeThemeBlock().cityInput(), Keys.BACK_SPACE);
            st.user().defaultSteps()
                .waitInSeconds(2)
                .inputsTextInElement(st.pages().mail().home().changeThemeBlock().cityInput(), NEW_CITY_NAME)
                .waitInSeconds(2)
                .clicksOn(st.pages().mail().home().suggestCitiesList().waitUntil(not(empty())).get(0))
                .waitInSeconds(2);
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

package ru.yandex.autotests.innerpochta.tests.main;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.data.ThemeIndexes.SEASON_THEME;
import static ru.yandex.autotests.innerpochta.data.ThemeIndexes.SIMPLE_THEME;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SEASONS_MODIFIER;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Выпадушка тем")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.GENERAL)
public class ThemeSettingsTest extends BaseTest {

    private static final String COLORFUL = "colorful";
    private static final String COLORFUL_THEME_SKIN = "colorful-theme-skin";
    private static final String BLUE = "blue";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем цветную тему с дефолтным цветом",
            of(COLORFUL_THEME_SKIN, BLUE, COLOR_SCHEME, COLORFUL)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .shouldSee(onMessagePage().mainSettingsPopupNew());
    }

    @Test
    @Title("Настройка цветной темы")
    @TestCaseId("2225")
    public void shouldChangeColorInTheme() {
        user.defaultSteps().clicksOn(onMessagePage().mainSettingsPopupNew().themesList().get(0))
            .clicksOn(onMessagePage().mainSettingsPopupNew().coloursList().get(3));
        shouldSaveSettingsOfTheme(
            "grass_green",
            COLORFUL_THEME_SKIN
        );
        user.defaultSteps().clicksOn(onMessagePage().mainSettingsPopupNew().themesListButtons().get(0),
                onMessagePage().mainSettingsPopupNew().themesList().get(SIMPLE_THEME.index()))
            .shouldNotSee(onMessagePage().mainSettingsPopupNew().coloursList());
    }

    @Test
    @Title("Настройка сезонной темы")
    @TestCaseId("2226")
    public void shouldChangeSeasonInTheme() {
        user.defaultSteps().clicksOn(onMessagePage().mainSettingsPopupNew().themesList().get(SEASON_THEME.index()))
            .shouldSee(onMessagePage().mainSettingsPopupNew().seasonsSettings())
            .clicksOn(onMessagePage().mainSettingsPopupNew().seasonWinter());
        shouldSaveSettingsOfTheme(
            "winter",
            SETTINGS_SEASONS_MODIFIER
        );
        user.defaultSteps().clicksOn(onMessagePage().mainSettingsPopupNew().themesListButtons().get(0),
                onMessagePage().mainSettingsPopupNew().themesList().get(SIMPLE_THEME.index()))
            .shouldNotSee(onMessagePage().mainSettingsPopupNew().seasonsSettings());
    }

    @Test
    @Title("Настройка автосмены сезонов")
    @TestCaseId("2305")
    public void shouldAutoRotationInSeasonTheme() {
        user.defaultSteps().clicksOn(onMessagePage().mainSettingsPopupNew().themesList().get(SEASON_THEME.index()))
            .shouldSee(onMessagePage().mainSettingsPopupNew().seasonsSettings())
            .clicksOn(onMessagePage().mainSettingsPopupNew().seasonWinter())
            .clicksOn(onMessagePage().mainSettingsPopupNew().rotationSeason().get(4));
        ;
        shouldSaveSettingsOfTheme(EMPTY_STR, SETTINGS_SEASONS_MODIFIER);
    }

    @Step
    @Title("Проверяем, что настройка сохранилась правильно")
    private void shouldSaveSettingsOfTheme(String name, String themeSetting) {
        assertThat(
            "Настройка темы не сохранилась!",
            user.apiSettingsSteps().getUserSettings(themeSetting),
            equalTo(name)
        );
    }
}

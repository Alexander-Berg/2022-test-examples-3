package ru.yandex.autotests.innerpochta.tests.differentModes;


import com.google.common.collect.Sets;
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
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ThemeSetupRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.CONTACTS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ThemeSetupRule.themeSetupRule;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Новые темы")
@Features(FeaturesConst.GENERAL)
@Tag(FeaturesConst.GENERAL)
@Stories(FeaturesConst.GENERAL)
@Description("У юзера настроен сборщик")
@RunWith(DataProviderRunner.class)
public class NewThemesTest {

    private List<String> themesWithChangableBackgrounds = Arrays.asList(
        "cosmos",
        "weather",
        "owls",
        "foxes",
        "tanks",
        "region_bashkir",
        "region_chelly",
        "region_ekb",
        "region_krasnoyarsk",
        "region_nn",
        "region_novosibirsk",
        "region_perm",
        "region_primorie",
        "region_samara",
        "region_tatarstan",
        "region_tomsk",
        "region_volgograd",
        "belarus",
        "plasticine"
    );

    private static final Set<By> IGNORE = Sets.newHashSet(
        cssSelector(".ns-view-themes-footer-wrapper"),
        cssSelector(".mail-App-Footer-Group_journal")
    );

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private ThemeSetupRule themeSetup = themeSetupRule(stepsProd);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORE);

    @DataProvider
    public static Object[][] testData() {
        return new Object[][]{
            {INBOX},
            {COMPOSE},
            {SETTINGS},
            {CONTACTS}
        };
    }

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(themeSetup);

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Отключаем промки",
            of(
                DISABLE_PROMO, STATUS_TRUE
            )
        );
    }

    @Test
    @Title("Тема {1}: базовые урлы.")
    @TestCaseId("4386")
    @UseDataProvider("testData")
    public void shouldSeeCorrectTheme(QuickFragments urlPath) {
        Consumer<InitStepsRule> actions = st -> moveToUrlAndSetThemeSkin(st, urlPath);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Фиксируем скин, проверяем, что находимся на нужном урле")
    private void moveToUrlAndSetThemeSkin(InitStepsRule st, QuickFragments urlPath) {
        String JAVASCRIPT_TO_SET_1ST_SKIN =
            "var currentSkinClass = document.documentElement.className.match(/m-skin-[^ ]*/);" +
                "document.documentElement.classList.remove(currentSkinClass);" +
                "document.documentElement.classList.add('m-skin-spring-clouds-day');" +
                "document.documentElement.classList.add('m-skin-spring');" +
                "document.documentElement.classList.add('m-skin-spring-clouds');" +
                "document.documentElement.classList.add('m-skin-1');";
        st.user().defaultSteps().opensFragment(urlPath);
        if (themesWithChangableBackgrounds.contains(UrlProps.urlProps().getTheme()))
            st.user().defaultSteps().executesJavaScript(JAVASCRIPT_TO_SET_1ST_SKIN);
        st.user().defaultSteps().waitInSeconds(5);
    }
}

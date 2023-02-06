package ru.yandex.autotests.innerpochta.tests.setting;

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
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL360_PAID;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME_COLORFUL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAMP_THEME;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Промо опт-ина")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.PROMO)
@RunWith(DataProviderRunner.class)
public class OptInPromoScreenTest {

    private static final String PROMO_QUERY_PARAM = "promo";
    private static final String PROMO_OPTIN = "promo-opt-in-modal";
    private static final String PROMO_OPTIN_TOOLTIP = "promo-opt-in-subs";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private AccLockRule lock2 = rules.getLock().useTusAccount(MAIL360_PAID);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void SetUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ промо",
            of(DISABLE_PROMO, FALSE)
        );
    }

    @Test
    @Title("Вёрстка промо опт-ина")
    @TestCaseId("6220")
    public void shouldSeeOptInPromo() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensUrl(
                    fromUri(st.getDriver().getCurrentUrl()).queryParam(PROMO_QUERY_PARAM, PROMO_OPTIN)
                        .build().toString()
                )
                .shouldSee(st.pages().mail().home().optInPromo());
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка промо-тултипа опт-ина")
    @TestCaseId("6219")
    @DataProvider({COLOR_SCHEME_COLORFUL, LAMP_THEME})
    public void shouldSeeOptInPromoTooltip(String theme) {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensUrl(
                    fromUri(st.getDriver().getCurrentUrl()).queryParam(PROMO_QUERY_PARAM, PROMO_OPTIN_TOOLTIP)
                        .build().toString()
                )
                .shouldSee(st.pages().mail().home().promoTooltip());
        switchTheme(theme);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка промо опт-ина у платного юзера")
    @TestCaseId("6220")
    public void shouldSeeOptInPromoFor360() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensUrl(
                    fromUri(st.getDriver().getCurrentUrl()).queryParam(PROMO_QUERY_PARAM, PROMO_OPTIN)
                        .build().toString()
                )
                .shouldSee(st.pages().mail().home().optInPromo());
        parallelRun.withActions(actions).withAcc(lock2.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка промо-тултипа опт-ина у платного юзера")
    @TestCaseId("6219")
    public void shouldSeeOptInPromoTooltipFor360() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensUrl(
                    fromUri(st.getDriver().getCurrentUrl()).queryParam(PROMO_QUERY_PARAM, PROMO_OPTIN_TOOLTIP)
                        .build().toString()
                )
                .shouldSee(st.pages().mail().home().promoTooltip());
        parallelRun.withActions(actions).withAcc(lock2.firstAcc()).run();
    }

    @Step("Включаем светлую/тёмную тему")
    private void switchTheme(String theme) {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Меняем тему",
            of(COLOR_SCHEME, theme)
        );
    }
}

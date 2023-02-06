package ru.yandex.autotests.innerpochta.tests.screentests;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Cookie;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.SHOW_SUBS_PROMO;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_I;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на верстку промок")
@Features(FeaturesConst.PROMO)
@Stories(FeaturesConst.GENERAL)
public class PromoScreenTest {

    private static final String SUBS_PROMO_SHOW_URL_PART = "?skip-newsletters-count-check";
    private static final String SHOW_WELCOME = "?promo=welcome-screen";
    private static final String SHOW_SMART = "?promo=smart-banner";
    private static final String COOKIE_NAME = "debug-settings-delete";
    private static final String COOKIE_VALUE = "qu_last-time-promo";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private static final Set<Coords> IGNORED_AREA = Sets.newHashSet(
        new Coords(500, 1950, 460, 60)
    );

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(acc.firstAcc(), Utils.getRandomName(), "");
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Верстка велкам скрина в списке писем")
    @TestCaseId("555")
    public void shouldSeeWelcomeScreen() {
        Consumer<InitStepsRule> act = st -> {
            String url = st.user().defaultSteps().getsCurrentUrl();
            st.user().defaultSteps()
                .opensUrl(url + SHOW_WELCOME)
                .shouldSee(st.pages().touch().messageList().welcomeScreen());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Верстка промо подписок")
    @TestCaseId("898")
    public void shouldSeeUnsubscribePromo() {
        Consumer<InitStepsRule> act = st -> {
            Cookie c = new Cookie(COOKIE_NAME, COOKIE_VALUE, ".yandex.ru", "/", null);
            st.getDriver().manage().addCookie(c);
            doPromoSetting(SHOW_SUBS_PROMO);
            st.user().defaultSteps().opensDefaultUrlWithPostFix(SUBS_PROMO_SHOW_URL_PART)
                .shouldSee(st.pages().touch().messageList().unsubscribePromo());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Верстка промо в сайдбаре")
    @TestCaseId("997")
    public void shouldSeeSidebarPromo() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
                .scrollTo(st.pages().touch().sidebar().sidebarPromo())
                .shouldSee(st.pages().touch().sidebar().fullVersion());

        parallelRun.withActions(act).withAcc(acc.firstAcc()).withIgnoredAreas(IGNORED_AREA).run();
    }

    @Test
    @Title("Верстка смарт баннера в списке писем")
    @TestCaseId("556")
    public void shouldSeeSmartBannerInMsgList() {
        Consumer<InitStepsRule> act = st -> {
            String url = st.user().defaultSteps().getsCurrentUrl();
            st.user().defaultSteps()
                .opensUrl(url + SHOW_SMART)
                .shouldSee(st.pages().touch().messageList().smartBanner());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Step("Проставляем нужную настройку промок приложения")
    private void doPromoSetting(String settings) {
        stepsTest.user().apiSettingsSteps().callWithListAndParams(
            "Включаем настройку для телефонов и планшетов",
            of(
                QUINN_PROMO_APP_P_A, settings,
                QUINN_PROMO_APP_T_A, settings,
                QUINN_PROMO_APP_P_I, settings,
                QUINN_PROMO_APP_T_I, settings
            )
        );
    }
}
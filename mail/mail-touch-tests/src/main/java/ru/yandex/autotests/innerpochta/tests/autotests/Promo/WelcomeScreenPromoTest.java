package ru.yandex.autotests.innerpochta.tests.autotests.Promo;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Cookie;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.NOT_SHOW_WELCOME_BEFORE_14_DAYS;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.SHOW_WELCOME;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.SHOW_WELCOME_AFTER_14_DAYS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_I;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на велкам скрин")
@Features(FeaturesConst.PROMO)
@Stories(FeaturesConst.GENERAL)
public class WelcomeScreenPromoTest {

    private static final String SKIP_PROMO_URL = "/touch/folder/1?skip-app-promo=1";
    private static final String COOKIE_NAME = "debug-settings-delete";
    private static final String COOKIE_VALUE = "qu_last-time-promo";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        Cookie c = new Cookie(COOKIE_NAME, COOKIE_VALUE, ".yandex.ru", "/", null);
        steps.getDriver().manage().addCookie(c);
    }

    @Test
    @Title("Велкам скрин закрывается по тапу в фон")
    @TestCaseId("459")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCloseWelcome() {
        doPromoSetting(SHOW_WELCOME);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().welcomeScreen())
            .offsetClick(20, 20)
            .shouldNotSee(steps.pages().touch().messageList().welcomeScreen());
    }

    @Test
    @Title("Не показываем если есть параметр ?skip-app-promo")
    @TestCaseId("661")
    public void shouldNotSeePromoWithParameter() {
        doPromoSetting(SHOW_WELCOME);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SKIP_PROMO_URL)
            .shouldNotSee(steps.pages().touch().messageList().welcomeScreen());
    }

    @Test
    @Title("Велкам скрин пропадает после рефреша страницы")
    @TestCaseId("663")
    public void shouldNotSeeWelcomeAfterRefresh() {
        doPromoSetting(SHOW_WELCOME);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().welcomeScreen())
            .refreshPage()
            .shouldNotSee(steps.pages().touch().messageList().welcomeScreen());
    }

    @Test
    @Title("Велкам скрин показываем, если с момента последнего показа прошло полгода")
    @TestCaseId("815")
    public void shouldSeeWelcomeAfterFortnight() {
        doPromoSetting(SHOW_WELCOME_AFTER_14_DAYS);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().welcomeScreen());
    }

    @Test
    @Title("Велкам скрин не показываем раньше чем через полгода")
    @TestCaseId("815")
    public void shouldNotSeeWelcomeBeforeFortnight() {
        doPromoSetting(NOT_SHOW_WELCOME_BEFORE_14_DAYS);
        steps.user().defaultSteps()
            .shouldNotSee(steps.pages().touch().messageList().welcomeScreen());
    }

    @Step("Проставляем нужную настройку промок и обновляемся")
    private void doPromoSetting(String setting) {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем настройку для телефонов и планшетов",
            of(
                QUINN_PROMO_APP_P_A, setting,
                QUINN_PROMO_APP_T_A, setting,
                QUINN_PROMO_APP_P_I, setting,
                QUINN_PROMO_APP_T_I, setting
            )
        );
        steps.user().defaultSteps().refreshPage();
    }
}

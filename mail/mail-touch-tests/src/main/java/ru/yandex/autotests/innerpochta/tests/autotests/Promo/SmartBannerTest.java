package ru.yandex.autotests.innerpochta.tests.autotests.Promo;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Cookie;
import ru.yandex.aqua.annotations.project.Aqua;
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
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.NOT_SHOW_SMART_DURING_3_WEEKS;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.SHOW_SMART;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.SHOW_SMART_AFTER_3_WEEKS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_I;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на смартбанер")
@Features(FeaturesConst.PROMO)
@Stories(FeaturesConst.GENERAL)
public class SmartBannerTest {

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
    @Title("Смартбаннер пропадает после рефреша страницы")
    @TestCaseId("439")
    public void shouldNotSeeSmartbannerAfterRefresh() {
        doPromoSetting(SHOW_SMART);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().smartBanner())
            .refreshPage()
            .shouldNotSee(steps.pages().touch().messageList().smartBanner());
    }

    @Test
    @Title("Смартбаннер закрывается по крестику")
    @TestCaseId("668")
    public void shouldCloseSmartbanner() {
        doPromoSetting(SHOW_SMART);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().smartBanner())
            .clicksOn(steps.pages().touch().messageList().closeBanner())
            .shouldNotSee(steps.pages().touch().messageList().smartBanner());
    }

    @Test
    @Title("Смарт баннер не показываем в течение 3 недель")
    @TestCaseId("669")
    public void shouldNotSeeSmartDuring3weeks() {
        doPromoSetting(NOT_SHOW_SMART_DURING_3_WEEKS);
        steps.user().defaultSteps()
            .shouldNotSee(
                steps.pages().touch().messageList().smartBanner(),
                steps.pages().touch().messageList().welcomeScreen()
            );
    }

    @Test
    @Title("Смарт баннер показываем через 3 недели")
    @TestCaseId("670")
    public void shouldSeeSmartAfter3weeks() {
        doPromoSetting(SHOW_SMART_AFTER_3_WEEKS);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().smartBanner());
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

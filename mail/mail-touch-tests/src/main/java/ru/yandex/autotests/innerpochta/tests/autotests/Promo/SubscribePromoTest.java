package ru.yandex.autotests.innerpochta.tests.autotests.Promo;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Cookie;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.NOT_SHOW_SUBS_PROMO_AFTER_CLOSE;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.NOT_SHOW_SUBS_PROMO_AFTER_OPEN;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.NOT_SHOW_SUBS_PROMO_DURING_MONTH;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.NOW_SHOW_SUBS_PROMO_MORE_10_TIMES;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.SHOW_SUBS_PROMO;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.SHOW_SUBS_PROMO_AFTER_MONTH;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT_TOUCH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на промки рассылок")
@Features(FeaturesConst.PROMO)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SubscribePromoTest {

    private static final String UNSUBSCRIBE_URL = "touch/settings/general/subscriptions";
    private static final String SUBS_PROMO_SHOW_URL_PART = "?skip-newsletters-count-check";
    private static final String COOKIE_NAME = "debug-settings-delete";
    private static final String COOKIE_VALUE = "qu_last-time-promo";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    private String subject;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @DataProvider
    public static Object[][] diffSettings() {
        return new Object[][]{
            {NOW_SHOW_SUBS_PROMO_MORE_10_TIMES},
            {NOT_SHOW_SUBS_PROMO_AFTER_CLOSE},
            {NOT_SHOW_SUBS_PROMO_AFTER_OPEN},
            {NOT_SHOW_SUBS_PROMO_DURING_MONTH},
        };
    }

    @Before
    public void prep() {
        subject = Utils.getRandomString();
        steps.user().apiMessagesSteps().sendMail(
            accLock.firstAcc(),
            subject,
            ""
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        Cookie c = new Cookie(COOKIE_NAME, COOKIE_VALUE, ".yandex.ru", "/", null);
        steps.getDriver().manage().addCookie(c);
    }

    @Test
    @Title("Промо рассылок закрывается по крестику и не показывается вместе с рекламой")
    @TestCaseId("898")
    public void shouldCloseSubsPromoByCross() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ рекламы",
            of(SHOW_ADVERTISEMENT_TOUCH, TRUE)
        );
        doSubsPromoSetting(SHOW_SUBS_PROMO);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SUBS_PROMO_SHOW_URL_PART)
            .shouldSee(steps.pages().touch().messageList().unsubscribePromo())
            .shouldNotSee(steps.pages().touch().messageList().advertisement())
            .clicksOn(steps.pages().touch().messageList().unsubscribePromoCloseBtn());
    }

    @Test
    @Title("Промо рассылок скрывается после рефреша")
    @TestCaseId("904")
    public void shouldCloseSubsPromoAfterRefresh() {
        doSubsPromoSetting(SHOW_SUBS_PROMO);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SUBS_PROMO_SHOW_URL_PART)
            .shouldSee(steps.pages().touch().messageList().unsubscribePromo())
            .refreshPage()
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .shouldNotSee(steps.pages().touch().messageList().unsubscribePromo());
    }

    @Test
    @Title("По тапу в промо рассылок переходим на страницу рассылок")
    @TestCaseId("899")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldOpenSubsPage() {
        doSubsPromoSetting(SHOW_SUBS_PROMO);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SUBS_PROMO_SHOW_URL_PART)
            .clicksOn(steps.pages().touch().messageList().unsubscribePromo())
            .shouldBeOnUrl(containsString(UNSUBSCRIBE_URL))
            .switchTo(IFRAME_SUBS)
            .clicksOn(steps.pages().touch().unsubscribe().closeSubs())
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .shouldNotSee(steps.pages().touch().messageList().unsubscribePromo());
    }

    @Test
    @Title("По тапу в промо рассылок открывается попап рассылок")
    @TestCaseId("899")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldOpenSubsPopup() {
        doSubsPromoSetting(SHOW_SUBS_PROMO);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SUBS_PROMO_SHOW_URL_PART)
            .clicksOn(steps.pages().touch().messageList().unsubscribePromo())
            .shouldSee(steps.pages().touch().messageList().unsubscribePopup())
            .switchTo(IFRAME_SUBS)
            .clicksOn(steps.pages().touch().unsubscribe().closeSubs())
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .shouldNotSee(steps.pages().touch().messageList().unsubscribePromo());
    }

    @Test
    @Title("Не должны видеть промо рассылок больше 10 раз, после открытия, после закрытия, чаще раза в месяц")
    @TestCaseId("900")
    @UseDataProvider("diffSettings")
    public void shouldNotSeeSubsPromoMore10Times(String setting) {
        doSubsPromoSetting(setting);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SUBS_PROMO_SHOW_URL_PART)
            .shouldNotSee(steps.pages().touch().messageList().unsubscribePromo());
    }

    @Test
    @Title("Не должны видеть промо рассылок, если нет рассылок")
    @TestCaseId("903")
    public void shouldNotSeeSubsPromoIfHaveNoSubscriptions() {
        doSubsPromoSetting(SHOW_SUBS_PROMO);
        steps.user().defaultSteps().refreshPage()
            .shouldNotSee(steps.pages().touch().messageList().unsubscribePromo());
    }

    @Test
    @Title("Должны увидеть промо рассылок через месяц")
    @TestCaseId("905")
    public void shouldSeeSubsPromoAfterMonth() {
        doSubsPromoSetting(SHOW_SUBS_PROMO_AFTER_MONTH);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SUBS_PROMO_SHOW_URL_PART)
            .shouldSee(steps.pages().touch().messageList().unsubscribePromo());
    }

    @Test
    @Title("Промо рассылок не показывается в поиске")
    @TestCaseId("941")
    public void shouldNotSeeSubsPromoInSearch() {
        doSubsPromoSetting(SHOW_SUBS_PROMO);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SUBS_PROMO_SHOW_URL_PART)
            .shouldSee(steps.pages().touch().messageList().unsubscribePromo())
            .clicksOn(steps.pages().touch().messageList().headerBlock().search())
            .inputsTextInElement(steps.pages().touch().search().header().input(), subject)
            .clicksOn(steps.pages().touch().search().header().find())
            .shouldSee(steps.pages().touch().search().messageBlock())
            .shouldNotSee(steps.pages().touch().messageList().unsubscribePromo());
    }

    @Step("Проставляем нужную настройку промо")
    private void doSubsPromoSetting(String setting) {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем настройку для телефонов и планшетов",
            of(
                QUINN_PROMO_APP_P_A, setting,
                QUINN_PROMO_APP_T_A, setting,
                QUINN_PROMO_APP_P_I, setting,
                QUINN_PROMO_APP_T_I, setting
            )
        );
    }
}

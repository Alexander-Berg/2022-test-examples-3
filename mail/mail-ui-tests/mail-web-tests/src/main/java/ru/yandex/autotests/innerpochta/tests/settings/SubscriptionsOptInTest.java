package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableBiMap.of;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS_LIZA;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL360_PAID;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.OPT_IN;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на опт-ин в попапе Управление рассылками")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SUBSCRIPTIONS)
@RunWith(DataProviderRunner.class)
public class SubscriptionsOptInTest extends BaseTest {

    private static String HELP_URL = "support/mail/web/preferences/manage-subscriptions.html#opt-in";

    private AccLockRule lock = AccLockRule.use().useTusAccount(MAIL360_PAID);
    private AccLockRule lock_free = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private RestAssuredAuthRule auth_free = RestAssuredAuthRule.auth(lock_free);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private AllureStepStorage user_free = new AllureStepStorage(webDriverRule, auth_free);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(auth_free);

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Должны открыть страницу тюнинга у юзера без подписки при клике во Включить на табе Новые")
    @TestCaseId("6309")
    public void shouldOpenTuningPageFromOptin() {
        user_free.loginSteps().forAcc(lock_free.firstAcc()).logins();
        user_free.settingsSteps().openSubscriptionsSettings();
        user_free.defaultSteps()
            .clicksOn(onUnsubscribePopupPage().tabNew())
            .clicksOn(onUnsubscribePopupPage().subscribeAndEnableBtn())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString("mail360.yandex.ru"));
    }

    @Test
    @Title("Должны включить опт-ин у юзера с подпиской")
    @TestCaseId("6308")
    public void shouldTurnOnOptin() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps()
            .clicksOn(onUnsubscribePopupPage().tabNew())
            .clicksOn(onUnsubscribePopupPage().enableBtn())
            .shouldSee(onUnsubscribePopupPage().emptySubsList());
    }

    @Test
    @Title("Должны Отключить опт-ин")
    @TestCaseId("6306")
    public void shouldTurnOffOptin() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем опт-ин юзеру",
            of(OPT_IN, STATUS_ON)
        );
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps()
            .clicksOn(onUnsubscribePopupPage().tabNew())
            .clicksOn(onUnsubscribePopupPage().optinDisableBtn())
            .clicksOn(onUnsubscribePopupPage().optinDisableConfirm())
            .shouldSee(onUnsubscribePopupPage().search())
            .clicksOn(onUnsubscribePopupPage().tabNew())
            .shouldSee(onUnsubscribePopupPage().enableBtn());
    }

    @Test
    @Title("Должны отменить отключение опт-ина")
    @TestCaseId("6307")
    public void shouldCancelTurningOptinOff() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем опт-ин юзеру",
            of(OPT_IN, STATUS_ON)
        );
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps()
            .clicksOn(onUnsubscribePopupPage().tabNew())
            .clicksOn(onUnsubscribePopupPage().optinDisableBtn())
            .shouldSee(onUnsubscribePopupPage().optinDisableConfirm())
            .clicksOn(onUnsubscribePopupPage().closeSubs())
            .shouldSee(
                onUnsubscribePopupPage().emptySubsList(),
                onUnsubscribePopupPage().optinDisableBtn()
            );
    }

    @Test
    @Title("Должны перейти в хэлп в табе опт-ина")
    @TestCaseId("6338")
    public void shouldOpenHelpPage() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps()
            .clicksOn(onUnsubscribePopupPage().tabNew())
            .clicksOn(onUnsubscribePopupPage().helpLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(HELP_URL));
    }

    @Test
    @Title("Открываем попап УР по урлу с параметром")
    @TestCaseId("6343")
    public void shouldOpenTabNewByUrl() {
        user.defaultSteps().opensDefaultUrlWithPostFix("?unsubscribe-popup=1")
            .switchTo(IFRAME_SUBS_LIZA)
            .shouldSee(onUnsubscribePopupPage().enableBtn());
    }
}

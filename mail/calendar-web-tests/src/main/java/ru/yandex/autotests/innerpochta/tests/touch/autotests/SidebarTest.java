package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Боковое меню Календаря")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.GENERAL)
public class SidebarTest {

    private static final String HELP_URL = "https://yandex.ru/support/calendar-mobile/common/troubleshooting.html";
    private static final String ACCOUNT_SETTINGS_URL = "https://passport.yandex.ru/profile";
    private static final String YANDEX_MAIN_PAGE = "https://yandex.ru/";
    private static final String HELP_MENU_ITEM = "Справка и поддержка";
    private static final String ACCOUNT_SETTINGS_MENU_ITEM = "Управление аккаунтом";
    private static final String CHANGE_USER_MENU_ITEM = "Сменить аккаунт";

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount(2);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем переход в помощь из боковой панели")
    @TestCaseId("1075")
    public void shouldOpenCorrectHelpUrl() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().burger())
            .clicksOnElementWithText(steps.pages().cal().touchHome().sidebar().menuItems(), HELP_MENU_ITEM)
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(HELP_URL);
    }

    @Test
    @Title("Закрываем боковую панель")
    @TestCaseId("1071")
    public void shouldCloseSidebar() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().burger())
            .shouldSee(steps.pages().cal().touchHome().sidebar().menuItems())
            .clicksOn(steps.pages().cal().touchHome().sidebar().close())
            .shouldNotSee(steps.pages().cal().touchHome().sidebar().menuItems());
    }

    @Test
    @Title("Открываем полную версию календаря. Тачевая версия должна вернуться по рефрешу")
    @TestCaseId("1077")
    public void shouldOpenDesktopVersion() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().burger())
            .shouldSee(steps.pages().cal().touchHome().sidebar().menuItems())
            .clicksOn(steps.pages().cal().touchHome().sidebar().fullView())
            .shouldSee(steps.pages().cal().home().leftPanel().miniCalendar())
            .refreshPage()
            .shouldSee(steps.pages().cal().touchHome().burger());
    }

    @Test
    @Title("Выходим из аккаунта")
    @TestCaseId("1076")
    public void shouldBecomeLoggedOut() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().burger())
            .shouldSee(steps.pages().cal().touchHome().sidebar().close())
            .clicksOn(steps.pages().cal().touchHome().sidebar().logOut())
            .shouldBeOnUrl(containsString(YANDEX_MAIN_PAGE));
    }

    @Test
    @Title("Переходим в паспорт на страницу управления аккаунтом")
    @TestCaseId("1074")
    public void shouldOpenPassportAccountSettings() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().burger())
            .clicksOnElementWithText(steps.pages().cal().touchHome().sidebar().menuItems(), ACCOUNT_SETTINGS_MENU_ITEM)
            .shouldBeOnUrl(ACCOUNT_SETTINGS_URL);
    }

    @Test
    @Title("Смена пользователя")
    @TestCaseId("1073")
    public void shouldChangeUser() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().burger())
            .clicksOnElementWithText(steps.pages().cal().touchHome().sidebar().menuItems(), CHANGE_USER_MENU_ITEM)
            .clicksOn(steps.pages().passport().backToPrevStep())
            .clicksOn(steps.pages().cal().touchHome().burger())
            .clicksOnElementWithText(steps.pages().cal().touchHome().sidebar().menuItems(), CHANGE_USER_MENU_ITEM)
            .clicksOn(steps.pages().passport().enterAnotherAcc())
            .inputsTextInElement(steps.pages().passport().loginField(), lock.accNum(1).getLogin())
            .clicksOn(steps.pages().passport().submit())
            .inputsTextInElement(steps.pages().passport().pwdField(), lock.accNum(1).getPassword())
            .clicksOn(steps.pages().passport().submit())
            .clicksIfCanOn(steps.pages().passport().touchNotNowBtn())
            .clicksIfCanOn(steps.pages().passport().notNowEmailBtn())
            .clicksIfCanOn(steps.pages().passport().skipAvatarBtn())
            .shouldBeOnUrl(containsString(UrlProps.urlProps().getBaseUri()));
    }
}

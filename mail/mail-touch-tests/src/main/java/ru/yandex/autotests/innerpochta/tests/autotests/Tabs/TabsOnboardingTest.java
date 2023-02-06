package ru.yandex.autotests.innerpochta.tests.autotests.Tabs;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Cookie;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TOUCH_ONBOARDING;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на онбординг табов")
@Features(FeaturesConst.TABS)
@Stories(FeaturesConst.PROMO)
@RunWith(DataProviderRunner.class)
public class TabsOnboardingTest {

    private static final String LAST_SLIDE_TITLE = "Только о важном",
        FIRST_SLIDE_TITLE = "Все письма по полочкам";
    private static final String COOKIE_NAME = "debug-settings-delete";
    private static final String COOKIE_VALUE = "show_folders_tabs,touch_onboarding_timestamp,qu_last-time-promo";

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
    public void prepare() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Сбрасываем показ онбординга табов",
            of(TOUCH_ONBOARDING, FALSE)
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        Cookie c = new Cookie(COOKIE_NAME, COOKIE_VALUE, ".yandex.ru", "/", null);
        steps.getDriver().manage().addCookie(c);
        steps.user().defaultSteps().refreshPage();
    }

    @Test
    @Title("Должны закрыть онбодинг по крестику")
    @TestCaseId("1131")
    public void shouldCloseOnboarding() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().tabsOnboarding().cross())
            .shouldNotSee(steps.pages().touch().messageList().tabsOnboarding());
    }

    @Test
    @Title("Должны закрыть онбординг кнопкой «Не сейчас»")
    @TestCaseId("1132")
    public void shouldCloseOnboardingByNotNow() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().touch().messageList().tabsOnboarding().yesBtn(),
            steps.pages().touch().messageList().tabsOnboarding().yesBtn()
        )
            .clicksOn(steps.pages().touch().messageList().tabsOnboarding().notNowBtn())
            .shouldNotSee(steps.pages().touch().messageList().tabsOnboarding());
        assertEquals(
            "Настройка табов должна быть выключена",
            EMPTY_STR,
            steps.user().apiSettingsSteps().getUserSettings(FOLDER_TABS)
        );
    }

    @Test
    @Title("Должны включить табы через онбординг")
    @TestCaseId("1133")
    public void shouldTurnOnTabsByOnboarding() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().touch().messageList().tabsOnboarding().yesBtn(),
            steps.pages().touch().messageList().tabsOnboarding().yesBtn(),
            steps.pages().touch().messageList().tabsOnboarding().yesBtn()
        )
            .shouldNotSee(steps.pages().touch().messageList().tabsOnboarding());
        assertEquals(
            "Настройка табов должна быть включена",
            STATUS_ON,
            steps.user().apiSettingsSteps().getUserSettings(FOLDER_TABS)
        );
    }

    @Test
    @Title("Должны переключать слайды онбординга с помощью свайпа")
    @TestCaseId("1135")
    public void shouldSlideBySwipe() {
        steps.user().touchSteps().rightSwipe(steps.pages().touch().messageList().tabsOnboarding());
        steps.user().defaultSteps().shouldHasText(
            steps.pages().touch().messageList().tabsOnboarding().title(),
            LAST_SLIDE_TITLE
        );
        steps.user().touchSteps().leftSwipe(steps.pages().touch().messageList().tabsOnboarding());
        steps.user().defaultSteps().shouldHasText(
            steps.pages().touch().messageList().tabsOnboarding().title(),
            FIRST_SLIDE_TITLE
        );
    }
}
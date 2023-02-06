package ru.yandex.autotests.innerpochta.tests.autotests.messageslist;

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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS;
import static ru.yandex.autotests.innerpochta.util.MailConst.NEWS_TAB_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QU_LAST_TIME_PROMO;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на полоску опт-ина в списке писем")
@Features({FeaturesConst.OPTIN})
@Stories(FeaturesConst.MESSAGE_LIST)
@Description("У юзера есть 1 новая рассылка")
public class OptInLineTest {

    private static final String TAB_NEW_TEXT = "1";
    private static final String ATTACH_TAB_TEXT = "С вложениями";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().className());
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
        Map<String, Serializable> settings = new HashMap<>();
        settings.put(FOLDER_TABS, FALSE);
        settings.put(QUINN_PROMO_APP_P_A, EMPTY_STR);
        settings.put(QUINN_PROMO_APP_T_A, EMPTY_STR);
        settings.put(QUINN_PROMO_APP_P_I, EMPTY_STR);
        settings.put(QUINN_PROMO_APP_T_I, EMPTY_STR);
        settings.put(QU_LAST_TIME_PROMO, EMPTY_STR);
        steps.user().apiSettingsSteps().callWithListAndParams(settings);
        steps.user().defaultSteps().refreshPage();
    }

    @Test
    @Title("Должны кликнуть в полоску опт-ина")
    @TestCaseId("1549")
    public void shouldClickOnLine() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().optInLine())
            .switchTo(IFRAME_SUBS)
            .shouldSeeThatElementHasText(steps.pages().touch().unsubscribe().tabNew(), TAB_NEW_TEXT);
    }

    @Test
    @Title("Должны кликнуть в крестик полоски опт-ина")
    @TestCaseId("1550")
    public void shouldClickOnCross() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().optInLineClose())
            .shouldNotSee(steps.pages().touch().messageList().optInLine());
    }

    @Test
    @Title("Полоска опт-ин показывается только во Входящих и табах")
    @TestCaseId("1551")
    public void shouldSeeInInboxOnly() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем табы и сбрасываем время последнего показа промо",
            of(
                FOLDER_TABS, TRUE,
                QU_LAST_TIME_PROMO, EMPTY_STR
            )
        );
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().touch().messageList().optInLine())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), NEWS_TAB_RU)
            .shouldSee(steps.pages().touch().messageList().optInLine())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), ATTACH_TAB_TEXT)
            .shouldNotSee(steps.pages().touch().messageList().optInLine());
    }
}

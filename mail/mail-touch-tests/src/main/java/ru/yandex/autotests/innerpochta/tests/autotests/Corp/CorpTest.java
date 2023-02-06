package ru.yandex.autotests.innerpochta.tests.autotests.Corp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
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
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.PromoConstants.SHOW_SUBS_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_I;

@Aqua.Test
@Title("Общие тесты на корп")
@Features({FeaturesConst.CORP_PACK})
@Stories(FeaturesConst.CORP)
public class CorpTest {

    private TouchRulesManager rules = touchRulesManager();
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).loginsToCorp();
    }

    @Test
    @Title("Должны отправить письмо")
    @TestCaseId("1018")
    public void shouldSendMessageOnCorp() {
        String subject = Utils.getRandomString();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().compose())
            .shouldSee(steps.pages().touch().composeIframe().inputTo())
            .clicksOn(steps.pages().touch().composeIframe().inputTo())
            .inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), accLock.firstAcc().getSelfEmail())
            .inputsTextInElement(steps.pages().touch().composeIframe().inputSubject(), subject)
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldNotSee(steps.pages().touch().composeIframe().header().sendBtn())
            .refreshPage()
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().messages().get(0).subject(), subject);
    }

    @Test
    @Title("Не должны видеть промо рассылок на корпе")
    @TestCaseId("921")
    public void shouldNotSeeSubsPromo() {
        doSubsPromoSetting(SHOW_SUBS_PROMO);
        steps.user().defaultSteps().refreshPage()
            .shouldNotSee(steps.pages().touch().messageList().unsubscribePromo());
    }

    @Test
    @Title("Не должно быть табов на корпе")
    @TestCaseId("955")
    public void shouldNotSeeTabs() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем табы",
            of(FOLDER_TABS, TRUE)
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldNotSee(steps.pages().touch().sidebar().tabsBlock());
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
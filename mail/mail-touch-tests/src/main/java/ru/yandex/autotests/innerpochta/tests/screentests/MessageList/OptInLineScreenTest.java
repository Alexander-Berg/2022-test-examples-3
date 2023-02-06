package ru.yandex.autotests.innerpochta.tests.screentests.MessageList;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_P_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_A;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QUINN_PROMO_APP_T_I;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.QU_LAST_TIME_PROMO;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Скриночные тесты на полоску опт-ина в списке писем")
@Features({FeaturesConst.OPTIN})
@Stories(FeaturesConst.MESSAGE_LIST)
@Description("У юзера есть новые рассылки")
public class OptInLineScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule accLock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Верстка полоски опт-ина")
    @TestCaseId("1554")
    public void shouldSeeOptInLine() {
        Consumer<InitStepsRule> act = st -> {
            st.user().apiSettingsSteps().callWithListAndParams(
                "Сбрасываем время последнего показа промо",
                of(QU_LAST_TIME_PROMO, EMPTY_STR)
            );
            st.user().defaultSteps().refreshPage()
                .shouldSee(st.pages().touch().messageList().optInLine());
        };
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Переключаем настройку показа промо",
            of(
                QUINN_PROMO_APP_P_A, EMPTY_STR,
                QUINN_PROMO_APP_T_A, EMPTY_STR,
                QUINN_PROMO_APP_P_I, EMPTY_STR,
                QUINN_PROMO_APP_T_I, EMPTY_STR
            )
        );
        parallelRun.withActions(act).withAcc(accLock.firstAcc()).runSequentially();
    }
}

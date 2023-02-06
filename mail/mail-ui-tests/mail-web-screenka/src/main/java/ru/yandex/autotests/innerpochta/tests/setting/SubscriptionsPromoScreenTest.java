package ru.yandex.autotests.innerpochta.tests.setting;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Промо управления рассылками")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.PROMO)
@RunWith(DataProviderRunner.class)
public class SubscriptionsPromoScreenTest {

    private static final String PROMO_QUERY_PARAM = "promo";
    private static final String PROMO_UNSUBSCRIBE = "promo-unsubscribe-popup";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void SetUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ промо",
            of(DISABLE_PROMO, FALSE)
        );
    }

    @Test
    @Title("Вёрстка промо отписок")
    @TestCaseId("5032")
    public void shouldSeeUnsubscribePromo() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensUrl(
                    fromUri(st.getDriver().getCurrentUrl()).queryParam(PROMO_QUERY_PARAM, PROMO_UNSUBSCRIBE)
                        .build().toString()
                )
                .shouldSee(st.pages().mail().settingsSubscriptions().unsubscribePromo());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

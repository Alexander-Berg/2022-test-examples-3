package ru.yandex.autotests.innerpochta.tests.screentests.Settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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

import static com.jayway.jsonassert.impl.matcher.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на настройку подписей")
@Description("У юзера заготовлены подписи с картинкой, форматированием, длинная")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SIGNATURES_SETTINGS)
@RunWith(DataProviderRunner.class)
public class SignaturesSettingsScreenTest {

    private static final int PREPARED_SIGNS_NUM = 3;
    private static final String SIGNATURES_URL_PART = "general/signatures";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().className());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();

    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), INBOX, TRASH));

    @Test
    @Title("Вёрстка страницы cо списком подписей")
    @TestCaseId("1349")
    public void shouldSeeSignsList() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSeeElementsCount(
                st.pages().touch().settings().signatures().waitUntil(not(empty())),
                PREPARED_SIGNS_NUM
            );

        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }

    @Test
    @Title("Должны открыть подпись для редактирования")
    @TestCaseId("1347")
    @DataProvider({"0", "1", "2"})
    public void shouldOpenSign(int num) {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().settings().signatures().waitUntil(not(Matchers.empty())).get(num))
                .shouldSee(st.pages().touch().settings().signatureInput());

        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }
}

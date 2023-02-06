package ru.yandex.autotests.innerpochta.tests.compose;

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

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Подписи")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeSignatureTest {

    private String msg_body = getRandomString();

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
    public void logIn() {
        stepsProd.user().apiSettingsSteps().changeSignsWithTextAndAmount(
            sign(getRandomString()),
            sign(getRandomString()),
            sign(getRandomString()),
            sign(getRandomString()),
            sign(getRandomString()),
            sign(getRandomString()),
            sign(getRandomString()),
            sign(getRandomString()),
            sign(getRandomString()),
            sign(getRandomString())
        );
    }

    @Test
    @Title("Наводим курсор на подпись")
    @TestCaseId("5893")
    public void shouldSeeHighlightingSignatureAndControl() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .onMouseHover(st.pages().mail().composePopup().signatureBlock())
                .shouldSee(st.pages().mail().composePopup().signatureChooser());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап выбора подписи")
    @TestCaseId("5893")
    public void shouldSeeSignaturePopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .onMouseHover(st.pages().mail().composePopup().signatureBlock())
                .clicksOn(st.pages().mail().composePopup().signatureChooser())
                .shouldSee(st.pages().mail().composePopup().signaturesPopup());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Скроллим попап выбора подписи")
    @TestCaseId("5893")
    public void shouldScrollDownSignaturePopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .onMouseHover(st.pages().mail().composePopup().signatureBlock())
                .clicksOn(st.pages().mail().composePopup().signatureChooser())
                .scrollDown(st.pages().mail().composePopup().signaturesPopup().signaturesBlock());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

}

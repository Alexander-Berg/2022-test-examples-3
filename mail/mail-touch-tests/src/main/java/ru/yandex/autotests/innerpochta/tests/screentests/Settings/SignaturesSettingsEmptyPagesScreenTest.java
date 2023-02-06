package ru.yandex.autotests.innerpochta.tests.screentests.Settings;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.jayway.jsonassert.impl.matcher.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на настройку подписей")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SIGNATURES_SETTINGS)
@RunWith(DataProviderRunner.class)
public class SignaturesSettingsEmptyPagesScreenTest {

    private static final String SIGNATURES_URL_PART = "general/signatures";
    private static final String TEXT_WITH_INDENTS = "∧＿∧ \n" +
                                                   "( ･ω･｡)つ━・*。\n" +
                                                   "⊂  ノ      ・゜+.\n" +
                                                    "しーＪ     °。+ *´¨)\n" +
                                                              ".· ´¸.·*´¨) ¸.·*¨)\n" +
                                                             "(¸.·´ (¸.·'* вжух \n";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Вёрстка нулевой страницы подписей")
    @TestCaseId("1361")
    public void shouldSeeEmptySignaturesList() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().settings().emptyList());

        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }

    @Test
    @Title("Вёрстка попапа удаления подписи")
    @TestCaseId("1352")
    public void shouldSeeDeletingPopup() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().settings().signatures().waitUntil(not(Matchers.empty())).get(0))
                .clicksOn(st.pages().touch().settings().removeSign())
                .shouldSee(st.pages().touch().settings().popup());

        stepsProd.user().apiSettingsSteps().changeSignsAmountTo(1);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }

    @Test
    @Title("Вёрстка пустой страницы создания подписи")
    @TestCaseId("1362")
    public void shouldSeeEmptySignCreatePage() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .shouldSee(st.pages().touch().settings().signatureInput());

        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }

    @Test
    @Title("Вёрстка страницы создания подписи")
    @TestCaseId("1362")
    public void shouldSeeSignCreatePage() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .clicksAndInputsText(st.pages().touch().settings().signatureInput(), TEXT_WITH_INDENTS);

        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }

    @Test
    @Title("Должны создать подпись")
    @TestCaseId("1362")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCreateFirstSignature() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .clicksAndInputsText(st.pages().touch().settings().signatureInput(), TEXT_WITH_INDENTS)
                .clicksOn(st.pages().touch().settings().editOrSave())
                .shouldSee(st.pages().touch().settings().create());

        stepsProd.user().apiSettingsSteps().changeSignsAmountTo(1);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }

    @Test
    @Title("Должны создать подпись")
    @TestCaseId("1362")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldCreateFirstSignatureTablet() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().settings().create())
                .clicksAndInputsText(st.pages().touch().settings().signatureInput(), TEXT_WITH_INDENTS)
                .clicksOn(st.pages().touch().settings().saveTablet())
                .shouldSee(st.pages().touch().settings().create());

        stepsProd.user().apiSettingsSteps().changeSignsAmountTo(1);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }

    @Test
    @Title("Должны отредактировать подпись")
    @TestCaseId("1354")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldEditSignature() {
        String text = getRandomString();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART))
                .clicksOn(st.pages().touch().settings().signatures().waitUntil(not(empty())).get(0))
                .clicksAndInputsText(st.pages().touch().settings().signatureInput(), "\n" + text + "\n")
                .clicksOn(st.pages().touch().settings().editOrSave())
                .shouldSeeThatElementHasText(
                    st.pages().touch().settings().signatures().waitUntil(not(empty())).get(0),
                    text
                );

        stepsProd.user().apiSettingsSteps().changeSignsAmountTo(1);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }

    @Test
    @Title("Должны отредактировать подпись")
    @TestCaseId("1354")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldEditSignatureTablet() {
        String text = getRandomString();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART))
                .clicksOn(st.pages().touch().settings().signatures().waitUntil(not(empty())).get(0))
                .clicksAndInputsText(st.pages().touch().settings().signatureInput(), "\n" + text + "\n")
                .clicksOn(st.pages().touch().settings().saveTablet())
                .shouldSeeThatElementHasText(
                    st.pages().touch().settings().signatures().waitUntil(not(empty())).get(0),
                    text
                );

        stepsProd.user().apiSettingsSteps().changeSignsAmountTo(1);
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(SETTINGS_TOUCH_PART.makeTouchUrlPart(SIGNATURES_URL_PART)).run();
    }
}

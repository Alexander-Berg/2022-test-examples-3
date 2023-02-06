package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
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
import static ru.yandex.autotests.innerpochta.util.MailConst.YA_DISK_URL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тулбар в визивиге")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.WYSIWYG)
public class WYSIWYGTest {

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

    @Test
    @Title("Должны видеть попап ссылок")
    @TestCaseId("3234")
    public void shouldSeeAddLinksPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .clicksOn(st.pages().mail().compose().composeToolbarBlock().addLinkBtn())
                .shouldSee(st.pages().mail().compose().addLinkPopup().hrefInput())
                .waitInSeconds(5); //попап дергается и скрины съезжают

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть выпадушку смайликов")
    @TestCaseId("3235")
    public void shouldSeeAddSmileDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .clicksOn(st.pages().mail().compose().composeToolbarBlock().addSmileBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вставляем смайлик")
    @TestCaseId("3236")
    public void shouldSeeSmileInWisiwyg() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .clicksOn(st.pages().mail().compose().composeToolbarBlock().addSmileBtn())
                .clicksOn(st.pages().mail().compose().smilesList().get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вставляем сссылку")
    @TestCaseId("3237")
    public void shouldSeeLinkInWisiwyg() {
        String text = "hello world";
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .clicksOn(st.pages().mail().compose().composeToolbarBlock().addLinkBtn())
                .clicksOn(st.pages().mail().compose().addLinkPopup().hrefInput())
                .inputsTextInElement(st.pages().mail().compose().addLinkPopup().hrefInput(), YA_DISK_URL)
                .clicksOn(st.pages().mail().compose().addLinkPopup().textInput())
                .inputsTextInElement(st.pages().mail().compose().addLinkPopup().textInput(), text)
                .clicksOn(st.pages().mail().compose().addLinkPopup().addLinkBtn())
                .shouldNotSee(st.pages().mail().compose().addLinkPopup().addLinkBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

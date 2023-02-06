package ru.yandex.autotests.innerpochta.tests.screentests.IframeCompose;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на поля в написании письма")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
public class ComposeFieldsScreenTest {

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
    @Title("Должны видеть ошибку в поле «Кому»")
    @TestCaseId("65")
    public void shouldSeeStatuslineError() {
        String contactName = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().inputsTextInElement(st.pages().touch().composeIframe().inputTo(), contactName)
                .clicksOn(st.pages().touch().composeIframe().header().sendBtn())
                .shouldSee(st.pages().touch().composeIframe().cantSendMailPopup());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть статуслайн в композе при некорректном адресе для СС ")
    @TestCaseId("153")
    public void shouldSeeStatuslineErrorforCC() {
        String contactName = Utils.getRandomName();
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().expandComposeFields())
                .inputsTextInElement(st.pages().touch().composeIframe().inputCc(), contactName)
                .clicksOn(st.pages().touch().composeIframe().header().sendBtn())
                .shouldSee(st.pages().touch().composeIframe().cantSendMailPopup());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть статуслайн в композе при некорректном адресе для BСС ")
    @TestCaseId("871")
    public void shouldSeeStatuslineErrorforBCC() {
        String contactName = Utils.getRandomName();
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().expandComposeFields())
                .inputsTextInElement(st.pages().touch().composeIframe().inputBcc(), contactName)
                .clicksOn(st.pages().touch().composeIframe().header().sendBtn())
                .shouldSee(st.pages().touch().composeIframe().cantSendMailPopup());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Разворачиваем алиасы")
    @TestCaseId("416")
    public void shouldSeeAliasesSuggest() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().expandComposeFields())
                .offsetClick(st.pages().touch().composeIframe().fieldFrom(), 200, 10)
                .shouldSee(st.pages().touch().composeIframe().suggestAliases())
                .waitInSeconds(1); //чтобы успел исчезнуть скроллбар
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Заполнить все поля в композе (to, cc, bcc)")
    @TestCaseId("87")
    public void shouldSeeAllFieldsFilled() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().inputsTextInElement(st.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
                .clicksOn(st.pages().touch().composeIframe().expandComposeFields())
                .inputsTextInElement(st.pages().touch().composeIframe().inputCc(), DEV_NULL_EMAIL)
                .inputsTextInElement(st.pages().touch().composeIframe().inputBcc(), DEV_NULL_EMAIL);
            st.user().hotkeySteps().pressHotKeys(st.pages().touch().composeIframe().inputTo(), Keys.ENTER);
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Раскрытые поля композа должны свернуться")
    @TestCaseId("884")
    public void shouldSeeClosedExpandComposeField() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().expandComposeFields())
                .shouldSee(st.pages().touch().composeIframe().inputCc())
                .clicksOn(st.pages().touch().composeIframe().expandComposeFields())
                .shouldNotSee(st.pages().touch().composeIframe().inputCc());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны видеть ошибку для незаполненного поля «Кому»")
    @TestCaseId("1400")
    public void shouldSeeErrorOfEmptyTo() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().sendBtn())
                .shouldSee(st.pages().touch().composeIframe().cantSendMailPopup());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }
}

package ru.yandex.autotests.innerpochta.tests.screentests.IframeCompose;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;


/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на саджест в композе")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.SUGGEST_COMPOSE)
public class ComposeSuggestScreenTest {

    private static final String MYSELF_EMAIL_PART = "yandex-team-";

    private TouchScreenRulesManager rules = touchScreenRulesManager();
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
        .around(removeAllMessages(() -> stepsProd.user(), DRAFT));

    @Test
    @Title("Должны видеть саджест контактов")
    @TestCaseId("66")
    public void shouldSeeContactSuggest() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().inputsTextInElement(
                st.pages().touch().composeIframe().inputTo(),
                st.user().apiAbookSteps().getPersonalContacts().get(0).getName().getFirst()
            )
                .shouldSee(st.pages().touch().composeIframe().composeSuggest());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны видеть добавленные адреса из саджеста популярных контактов")
    @TestCaseId("414")
    public void shouldAddContactsInSuggest() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().inputTo())
                .clicksOn(st.pages().touch().composeIframe().inputTo())
                .clicksOn(st.pages().touch().composeIframe().composeSuggestItems().get(1))
                .shouldNotSee(st.pages().touch().composeIframe().composeSuggest())
                .shouldSee(st.pages().touch().composeIframe().yabble());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Добавляем в получатели группу контактов")
    @TestCaseId("68")
    public void shouldSeeGroupInFieldTo() {
        String groupName = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().inputsTextInElement(st.pages().touch().composeIframe().inputTo(), groupName)
                .shouldSee(st.pages().touch().composeIframe().composeSuggest())
                .clicksOnElementWithText(st.pages().touch().composeIframe().composeSuggestItems(), groupName)
                .shouldNotSee(st.pages().touch().composeIframe().composeSuggest());
        };
        stepsTest.user().apiAbookSteps().removeAllAbookGroups()
            .addNewAbookGroupWithContacts(
                groupName,
                stepsTest.user().apiAbookSteps().getPersonalContacts().get(0),
                stepsTest.user().apiAbookSteps().getPersonalContacts().get(1)
            );
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны видеть контакт «Себе» в саджесте")
    @TestCaseId("1404")
    public void shouldSeeMyselfInSuggest() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().touch().composeIframe().inputTo(), MYSELF_EMAIL_PART)
                .waitInSeconds(2)
                .shouldContainText(st.pages().touch().composeIframe().composeSuggestItems().get(0), "Себе");
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }
}

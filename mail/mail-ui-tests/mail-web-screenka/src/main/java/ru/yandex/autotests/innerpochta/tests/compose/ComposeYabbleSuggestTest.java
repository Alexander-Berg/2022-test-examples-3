package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Саджест ябблов в композе")
@Features({FeaturesConst.COMPOSE})
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.YABBLE)
public class ComposeYabbleSuggestTest {

    private static final String TO_CONTACT = "Первый Тестович";
    private static final String CC_CONTACT = "Второй Тестович";
    private static final String BCC_CONTACT = "Третий Тестович";
    private static final String CONTACT_WITH_TWO_EMAILS = "Два Адреса";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORED_ELEMENTS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), INBOX, TRASH));

    @Test
    @Title("Саджест показывает только варианты, содержащие подстроку в поле «Кому»")
    @TestCaseId("2862")
    public void shouldSeeRelevantSuggestInTo() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton());
            st.user().composeSteps().inputsAddressInFieldTo(CONTACT_WITH_TWO_EMAILS);
            st.user().defaultSteps()
                .shouldSee(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Саджест показывает только варианты, содержащие подстроку в поле «Копия»")
    @TestCaseId("2862")
    public void shouldSeeRelevantSuggestInCc() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn());
            st.user().composeSteps().inputsAddressInFieldCc(CONTACT_WITH_TWO_EMAILS);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().popupCc())
                .shouldSee(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Саджест показывает только варианты, содержащие подстроку в поле «Скрытая копия»")
    @TestCaseId("2862")
    public void shouldSeeRelevantSuggestInBcc() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn());
            st.user().composeSteps().inputsAddressInFieldBcc(CONTACT_WITH_TWO_EMAILS);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().popupBcc())
                .shouldSee(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Добавление яббла из саджеста")
    @TestCaseId("2754")
    public void shouldSeeYabbleFromSuggest() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn());
            takeContactFromSuggest(st, st.pages().mail().composePopup().expandedPopup().popupTo(), TO_CONTACT);
            takeContactFromSuggest(st, st.pages().mail().composePopup().expandedPopup().popupCc(), CC_CONTACT);
            takeContactFromSuggest(st, st.pages().mail().composePopup().expandedPopup().popupBcc(), BCC_CONTACT);
            st.user().defaultSteps().shouldSee(
                st.pages().mail().composePopup().yabbleTo(),
                st.pages().mail().composePopup().yabbleCc(),
                st.pages().mail().composePopup().yabbleBcc()
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Выбираем яббл из саджеста в указанном поле")
    private void takeContactFromSuggest(InitStepsRule st, MailElement field, String name) {
        st.user().defaultSteps().clicksOn(field)
            .clicksOnElementWithText(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())), name);
    }
}
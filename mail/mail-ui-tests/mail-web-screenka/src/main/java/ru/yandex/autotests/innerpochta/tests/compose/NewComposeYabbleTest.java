package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
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
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.COMPOSE_SMALL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAST_USED_COMPOSE_SIZE;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Ябблы")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeYabbleTest {
    private static final String SMALL_CONTACT = "25";
    private static final String MEDIUM_CONTACT = "normal bot";
    private static final String LONG_CONTACT = "extra huge biggest massive bot";
    private static final String WRONG_CONTACT = "wrongwrong";
    private static final String MANY_RECIPIENTS =
        "testbot1@yandex.ru, testbot2@yandex.ru, testbot3@yandex.ru, testbot5@yandex.ru, testbot6@yandex.ru";
    private static final String GROUP = "111";

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
        .around(removeAllMessages(() -> stepsProd.user(), INBOX, DRAFT, TRASH));

    @Test
    @Title("Добавляем ябблы разных размеров + некорректный")
    @TestCaseId("5702")
    public void shouldSeeYabbleStyles() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), SMALL_CONTACT)
                .clicksOn(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0))
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), WRONG_CONTACT)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn())
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().popupCc(), MEDIUM_CONTACT)
                .clicksOn(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0))
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().popupBcc(), LONG_CONTACT)
                .clicksOn(st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Формируем яббл «Ещё»")
    @TestCaseId("5702")
    public void shouldSeeMoreYabble() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .inputsTextInElement(
                    st.pages().mail().composePopup().expandedPopup().popupTo(),
                    MANY_RECIPIENTS + " " + WRONG_CONTACT
                )
                .clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn())
                .shouldSee(st.pages().mail().composePopup().expandedPopup().popupBcc())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().expandCollapseBtn())
                .shouldSee(st.pages().mail().composePopup().yabbleMore());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем маленький композ",
            of(LAST_USED_COMPOSE_SIZE, COMPOSE_SMALL)
        );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Добавляем групповой яббл")
    @TestCaseId("5724")
    public void shouldSeeGroupYabble() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), GROUP)
                .clicksOn(st.pages().mail().composePopup().suggestList().get(0))
                .clicksOn(st.pages().mail().composePopup().yabbleTo());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

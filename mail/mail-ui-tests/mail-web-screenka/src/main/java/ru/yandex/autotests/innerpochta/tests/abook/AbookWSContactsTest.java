package ru.yandex.autotests.innerpochta.tests.abook;

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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.CONTACTS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Контакты WS")
@Features(FeaturesConst.ABOOK)
@Tag(FeaturesConst.ABOOK)
@Stories(FeaturesConst.WORKSPACE)
public class AbookWSContactsTest {

    private static final String CREDS = "AbookWSContactsTest";
    private static final String CREDS_2 = "WSManyContacts";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().names(CREDS, CREDS_2));
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Смотрим, что аватарки юзеров, групп и отделов корректны")
    @TestCaseId("2370")
    public void shouldSeeCorrectAvatars() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().groupsBlock().sharedContacts())
                .shouldSee(st.pages().mail().abook().contacts().waitUntil(not(empty())).get(0));

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем все общие контакты")
    @TestCaseId("2368")
    public void shouldSeeAllSharedContacts() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().groupsBlock().sharedContacts())
                .shouldSee(st.pages().mail().abook().mail360HeaderBlock())
                .shouldSee(st.pages().mail().abook().contacts().get(0))
                .clicksOn(st.pages().mail().abook().showAllContactsBtn());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.acc(CREDS_2)).run();
    }

    @Test
    @Title("Добавить общий контакт")
    @TestCaseId("6210")
    public void shouldSeeSharedContactInTo() {
        Consumer<InitStepsRule> actions = st -> {
            openAbookPopup(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().compose().abookPopup().selectGroupBtn())
                .clicksOnElementWithText(st.pages().mail().compose().selectGroupItem(), "Общие контакты")
                .clicksOn(st.pages().mail().compose().abookPopup().contacts().get(0))
                .clicksOn(st.pages().mail().compose().abookPopup().selectButton())
                .shouldSee(st.pages().mail().composePopup().yabbleTo());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.acc(CREDS_2)).run();
    }

    @Step("Открываем попап абука")
    private void openAbookPopup(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().popupTo())
            .clicksOn(st.pages().mail().composePopup().abookBtn())
            .shouldSee(st.pages().mail().compose().abookPopup());
    }
}

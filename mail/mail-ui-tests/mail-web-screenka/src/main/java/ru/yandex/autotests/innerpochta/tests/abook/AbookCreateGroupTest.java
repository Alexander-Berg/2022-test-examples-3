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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.CONTACTS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * Created by irmashirma
 */
@Aqua.Test
@Title("Контакты - Создание группы")
@Features(FeaturesConst.ABOOK)
@Tag(FeaturesConst.ABOOK)
@Stories(FeaturesConst.GENERAL)
public class AbookCreateGroupTest {

    private ScreenRulesManager rules = screenRulesManager();
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
    @Title("Выбираем группу в фильтре")
    @TestCaseId("3016")
    public void shouldSeeGroupDropdown() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .onMouseHover(st.pages().mail().abook().groupsBlock())
            .clicksOn(st.pages().mail().abook().groupsBlock().createGroupButton())
            .shouldSee(st.pages().mail().abook().createNewGroupPopup())
            .clicksOn(st.pages().mail().abook().createNewGroupPopup().changeGroupBtn())
            .shouldSee(st.pages().mail().abook().selectGroupsDropdownInPopup());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбираем контакт кликом")
    @TestCaseId("3017")
    public void shouldSeeSelectedContact() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().abook().groupsBlock())
                .clicksOn(st.pages().mail().abook().groupsBlock().createGroupButton())
                .clicksOn(st.pages().mail().abook().createNewGroupPopup().contacts().get(0));

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбираем все контакты")
    @TestCaseId("3023")
    public void shouldSeeAllSelectedContact() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().abook().groupsBlock())
                .clicksOn(st.pages().mail().abook().groupsBlock().createGroupButton())
                .clicksOn(st.pages().mail().abook().createNewGroupPopup().selectAllContacts());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Поиск по контактам")
    @TestCaseId("3019")
    public void shouldSeeCorrectContacts() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().abook().groupsBlock())
                .clicksOn(st.pages().mail().abook().groupsBlock().createGroupButton())
                .inputsTextInElement(st.pages().mail().abook().createNewGroupPopup().searchContactInput(), "a");

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть еще контакты")
    @TestCaseId("3020")
    public void shouldSeeMoreContacts() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().abook().groupsBlock())
                .clicksOn(st.pages().mail().abook().groupsBlock().createGroupButton())
                .clicksOn(st.pages().mail().abook().createNewGroupPopup().moreContactsBtn());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбираем пустую группу")
    @TestCaseId("3022")
    public void shouldNotSeeContacts() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().abook().groupsBlock())
                .clicksOn(st.pages().mail().abook().groupsBlock().createGroupButton())
                .shouldSee(st.pages().mail().abook().createNewGroupPopup())
                .clicksOn(st.pages().mail().abook().createNewGroupPopup().changeGroupBtn())
                .shouldSee(st.pages().mail().abook().selectGroupsDropdownInPopup())
                .clicksOn(st.pages().mail().abook().selectGroupsDropdownInPopup().groupsNames().get(1));

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем адреса контакта")
    @TestCaseId("3024")
    public void shouldSeeEmailsInContact() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().abook().groupsBlock())
                .clicksOn(st.pages().mail().abook().groupsBlock().createGroupButton())
                .shouldSee(st.pages().mail().abook().createNewGroupPopup())
                .clicksOn(st.pages().mail().abook().createNewGroupPopup().moreEmailsInContactBtn());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }
}

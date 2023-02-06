package ru.yandex.autotests.innerpochta.tests.abook;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
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
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * Created by irmashirma
 */
@Aqua.Test
@Title("Контакты - Карточка контакта")
@Features({FeaturesConst.ABOOK, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.ABOOK)
@Stories(FeaturesConst.CONTACT_CARD)
public class AbookContactCardTest {

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
    public RuleChain chain = rules.createRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), DRAFT, TRASH));

    @Before
    public void setUp() {
        stepsProd.user().apiAbookSteps().removeAllAbookContacts().addNewContacts(
            stepsProd.user().abookSteps().createContactWithParametrs(getRandomName(), lock.firstAcc().getSelfEmail())
        );
    }

    @Test
    @Title("Открываем карточку контакта")
    @TestCaseId("3009")
    public void shouldSeeContactCard() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().abook().toolbarBlock())
                .clicksOn(st.pages().mail().abook().contacts().get(0))
                .shouldSee(st.pages().mail().abook().contactPopup());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем переписку контакта")
    @TestCaseId("3011")
    public void shouldSeeMessagesFromContact() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().abook().toolbarBlock())
                .clicksOn(st.pages().mail().abook().contacts().get(0))
                .clicksOn(st.pages().mail().abook().contactPopup().showAllMsg())
                .shouldSee(st.pages().mail().home().displayedMessages());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем письма контакта с аттачами")
    @TestCaseId("3012")
    public void shouldSeeMessagesFromContactWithAttach() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .shouldSee(st.pages().mail().abook().toolbarBlock())
                .clicksOn(st.pages().mail().abook().contacts().get(0))
                .clicksOn(st.pages().mail().abook().contactPopup().showAllAttach())
                .shouldBeOnUrlWith(SEARCH)
                .shouldSee(st.pages().mail().search().mail360HeaderBlock().closeSearch());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }


    @Test
    @Title("Открываем изменение контакта")
    @TestCaseId("3013")
    public void shouldSeeEditContactPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().abook().toolbarBlock())
                .clicksOn(st.pages().mail().abook().contacts().get(0))
                .clicksOn(st.pages().mail().abook().contactPopup().editContactBtn())
                .shouldSee(st.pages().mail().abook().addContactPopup().submitContactButton());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем написание письма контакту")
    @TestCaseId("3014")
    public void shouldSeeComposeWithCorrectFieldTo() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().abook().toolbarBlock())
                .clicksOn(st.pages().mail().abook().contacts().get(0))
                .clicksOn(st.pages().mail().abook().contactPopup().composeBtn())
                .shouldSee(st.pages().mail().composePopup().expandedPopup());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Видим обновленные данные и аватарку контакта")
    @TestCaseId("3939")
    public void shouldSeeUpdatedContact() {
        String name = getRandomString();
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().abook().toolbarBlock())
                .clicksOn(
                    st.pages().mail().abook().contacts().get(0),
                    st.pages().mail().abook().contactPopup().editContactBtn()
                )
                .inputsTextInElement(st.pages().mail().abook().addContactPopup().name(), name)
                .inputsTextInElement(st.pages().mail().abook().addContactPopup().lastName(), name)
                .clicksOn(st.pages().mail().abook().addContactPopup().submitContactButton());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }
}




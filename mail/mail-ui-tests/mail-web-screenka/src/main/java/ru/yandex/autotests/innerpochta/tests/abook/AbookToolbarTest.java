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
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.CONTACTS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author irmashirma
 */
@Aqua.Test
@Title("Контакты - Добавить контакт")
@Features(FeaturesConst.ABOOK)
@Tag(FeaturesConst.ABOOK)
@Stories(FeaturesConst.TOOLBAR)
public class AbookToolbarTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private String groupName;
    private Contact contact;

    List<Email> emails = Arrays.asList(
        new Email().withValue(DEV_NULL_EMAIL),
        new Email().withValue(lock.firstAcc().getSelfEmail())
    );

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        contact = stepsProd.user().abookSteps().createContactWithParametrs(
            getRandomString(),
            lock.firstAcc().getSelfEmail()
        ).withEmail(emails);
        stepsProd.user().apiAbookSteps().addContactWithTwoEmails(contact.getName().getFirst(), contact)
            .addNewContacts(contact);
        groupName = stepsProd.user().apiAbookSteps().addNewAbookGroup(Utils.getRandomName()).getTitle();
    }

    @Test
    @Title("Открываем попап добавления контактов")
    @TestCaseId("2681")
    public void shouldSeeAddContactPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().toolbarBlock().addContactButton())
                .shouldSee(st.pages().mail().abook().addContactPopup());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }


    @Test
    @Title("Открываем композ из контактов")
    @TestCaseId("2686")
    public void shouldSeeOpenedCompose() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().composeButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем дропдаун по кнопке «Еще»")
    @TestCaseId("2688")
    public void shouldSeeMoreSelect() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().toolbarBlock().moreButton())
                .shouldSee(st.pages().mail().abook().popupMenu());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап импорта контактов")
    @TestCaseId("2689")
    public void shouldSeeAddContactFromFilePopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().toolbarBlock().moreButton())
                .clicksOn(st.pages().mail().abook().moreDropdown().get(0))
                .shouldSee(st.pages().mail().abook().popup());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап экспорта контактов")
    @TestCaseId("2866")
    public void shouldSeeSaveContactToFilePopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().toolbarBlock().moreButton())
                .clicksOn(st.pages().mail().abook().moreDropdown().get(1))
                .shouldSee(st.pages().mail().abook().popupSubmit());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем дропдаун добавления в группу")
    @TestCaseId("2690")
    public void shouldSeeAddContactToGroupDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().toolbarBlock().selectAllContacts())
                .clicksOn(st.pages().mail().abook().toolbarBlock().addContactToGroupButton())
                .shouldSee(st.pages().mail().abook().groupSelectDropdown());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап адресов")
    @TestCaseId("2932")
    public void shouldSeeAddContactToGroupPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().toolbarBlock().selectAllContacts());
            st.user().abookSteps().addsContactToGroup(groupName);
            st.user().defaultSteps().shouldSee(st.pages().mail().abook().addContactsToGroupPopup());
        };
        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должен быть статуслайн при добавлении контактов")
    @TestCaseId("3028")
    public void shouldSeeStatusLineAddContactToGroup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().toolbarBlock().selectAllContacts());
            st.user().abookSteps().addsContactToGroup(groupName);
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().addContactsToGroup())
                .shouldSee(st.pages().mail().home().statusLineBlock());
        };
        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть тулбар контактов")
    @TestCaseId("4080")
    public void shouldSeeAbookToolbar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().toolbarBlock().selectAllContacts())
                .shouldSee(st.pages().mail().abook().toolbarBlock())
                .shouldNotSee(st.pages().mail().home().selectAllMessagesPopup());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }
}

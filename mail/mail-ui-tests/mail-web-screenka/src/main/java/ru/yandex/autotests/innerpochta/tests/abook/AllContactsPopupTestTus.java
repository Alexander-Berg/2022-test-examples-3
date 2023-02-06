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
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Попап контактов в композе")
@Features(FeaturesConst.ABOOK)
@Tag(FeaturesConst.ABOOK)
@Stories(FeaturesConst.GENERAL)
public class AllContactsPopupTestTus {

    private String groupName;

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

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

    @Test
    @Title("Открываем группу без контактов")
    @TestCaseId("2834")
    public void shouldSeeEmptyGroup() {
        Consumer<InitStepsRule> actions = st -> {
            openAbookPopup(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().compose().abookPopup().selectGroupBtn())
                .clicksOnElementWithText(st.pages().mail().compose().selectGroupItem(), groupName)
                .shouldHasText(st.pages().mail().compose().abookPopup().selectGroupBtn(), groupName);
        };
        groupName = Utils.getRandomName();
        Contact contact = stepsProd.user().abookSteps().createDefaultContact();
        stepsProd.user().apiAbookSteps().addNewContacts(contact).addNewAbookGroup(groupName);

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Должны видеть все адреса контакта")
    @TestCaseId("2829")
    public void shouldSeeAllEmailsInContact() {
        Consumer<InitStepsRule> actions = st -> {
            openAbookPopup(st);
            st.user().defaultSteps().clicksOn(
                st.pages().mail().compose().abookPopup().contacts().get(0).remainingEmailsInPopupBtn()
            );
        };

        Contact contact = stepsProd.user().abookSteps().createContactWithParametrs(
            getRandomString(),
            lock.firstAcc().getSelfEmail()
        ).withEmail(emails);
        stepsProd.user().apiAbookSteps().addContactWithTwoEmails(contact.getName().getFirst(), contact);

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).runSequentially();
    }

    @Step("Открываем попап абука")
    private void openAbookPopup(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().popupTo())
            .clicksOn(st.pages().mail().composePopup().abookBtn())
            .shouldSee(st.pages().mail().compose().abookPopup());
    }
}

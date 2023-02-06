package ru.yandex.autotests.innerpochta.tests.messageView;

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
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author a-zoshchuk
 */

@Aqua.Test
@Title("Просмотр карточки контакта")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.CONTACT_CARD)
public class ContactsCardTest {

    private static final String PHONE_NUMBER = "+79999999999";

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

    @Before
    public void setUp() {
        Contact contact = stepsProd.user().abookSteps().createContactWithParametrs(
            getRandomString(),
            lock.firstAcc().getSelfEmail()
        );
        stepsProd.user().apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), getRandomString(), getRandomString());
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
        stepsProd.user().apiAbookSteps().removeAllAbookContacts().removeAllAbookGroups()
            .addContact(Utils.getRandomName(), contact)
            .addNewAbookGroupWithContacts(
                Utils.getRandomString(),
                stepsProd.user().apiAbookSteps().getPersonalContacts().get(0)
            );
    }

    @Test
    @Title("Проверяем попап контакта")
    @TestCaseId("2682")
    public void shouldSeeContactCard() {
        Consumer<InitStepsRule> actions = this::openContactCard;

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем сохранение изменений в контакте")
    @TestCaseId("2683")
    public void shouldSeeSavedChanges() {
        Consumer<InitStepsRule> actions = st -> {
            openContactCard(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().mailCard().editContactBtn())
                .inputsTextInElement(st.pages().mail().msgView().mailCard().addNumberField(), PHONE_NUMBER)
                .clicksOn(st.pages().mail().msgView().mailCard().saveChangesBtn())
                .shouldSee(st.pages().mail().msgView().mailCard().editContactBtn());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть все письма от контакта")
    @TestCaseId("2036")
    public void shouldShowAllMsgFromContact() {
        Consumer<InitStepsRule> actions = st -> {
            openContactCard(st);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().msgView().mailCard().showAllMsg())
                .shouldSee(st.pages().mail().home().mail360HeaderBlock().closeSearch());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем карточку контакта через просмотр письма")
    private void openContactCard(InitStepsRule st) {
        st.user().messagesSteps().clicksOnMessageByNumber(0);
        st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageTextBlock().text())
            .clicksOn(st.pages().mail().msgView().messageHead().fromName())
            .shouldSee(st.pages().mail().msgView().mailCard().contactName());
    }
}

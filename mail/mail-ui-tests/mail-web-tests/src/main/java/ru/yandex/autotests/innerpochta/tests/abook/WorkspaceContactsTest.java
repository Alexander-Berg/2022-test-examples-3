package ru.yandex.autotests.innerpochta.tests.abook;

import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Name;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.CONTACTS;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllContactsFromAbookRule.removeAllContactsFromAbookRule;

/**
 * @author a-zoshchuk
 */

@Aqua.Test
@Title("Общие и личные контакты в WS")
@Features({FeaturesConst.ABOOK, FeaturesConst.NOT_TUS})
@Stories(FeaturesConst.WORKSPACE)
@Tag(FeaturesConst.ABOOK)
@UseCreds(WorkspaceContactsTest.CREDS)
public class WorkspaceContactsTest extends BaseTest {

    public static final String CREDS = "WorkspaceContactsTest";

    private static final String FROM_EMAIL = "robbitter-2@robbiter.havroshik.ru";
    private static final String CONTACT_ROLE = "потакатель прихоти автотестов";
    private static final String COMMON_CONTACT = "Botsman";

    private AccLockRule lock = AccLockRule.use().annotation();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(removeAllContactsFromAbookRule(user));

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Добавляем комментарий в общий контакт")
    @TestCaseId("2351")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66332")
    public void shouldSeeSharedContactInPersonalAbook() {
        String comment = Utils.getRandomString();
        user.defaultSteps().opensFragment(CONTACTS)
            .clicksOn(onAbookPage().groupsBlock().sharedContacts())
            .clicksOnElementWithText(onAbookPage().contacts().waitUntil(not(empty())), FROM_EMAIL)
            .clicksOn(onAbookPage().contactPopup().editContactBtn())
            .inputsTextInElement(onAbookPage().contactPopup().descriptionInput(), comment)
            .clicksOn(onAbookPage().contactPopup().saveChangesBtn());
        shouldSeeContactWithCommentInPersonalAbook(comment);
    }

    @Test
    @Title("Добавляем общий контакт в личные при просмотре письма")
    @TestCaseId("2352")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66332")
    public void shouldAddWSContactFromLetter() {
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().clicksOn(onMessageView().messageHead().fromName())
            .clicksOn(onAbookPage().contactPopup().saveToAbookBtn())
            .shouldSee(onAbookPage().contactPopup().editContactBtn())
            .opensFragment(CONTACTS)
            .clicksOn(onAbookPage().groupsBlock().personalContacts());
        user.abookSteps().shouldSeeNumberOfContacts(1);
        user.defaultSteps().clicksOnElementWithText(onAbookPage().contacts(), FROM_EMAIL);
        user.abookSteps().shouldSeeEmail(FROM_EMAIL);
    }

    @Test
    @Title("Изменяем комментарий у шаренного контакта в личных контактах")
    @TestCaseId("2353")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66332")
    public void shouldSeeCommentInSharedAbookChanged() {
        String comment = Utils.getRandomString();
        String newComment = Utils.getRandomString();
        Contact contact = user.apiAbookSteps().getSharedContacts().get(0);
        contact.setDescription(comment);
        user.apiAbookSteps().editSharedContact(contact);
        user.defaultSteps().opensFragment(CONTACTS);
        shouldSeeContactWithCommentInPersonalAbook(comment);
        user.defaultSteps().clicksOn(onAbookPage().contactPopup().editContactBtn())
            .inputsTextInElement(onAbookPage().contactPopup().descriptionInput(), newComment)
            .clicksOn(onAbookPage().contactPopup().saveChangesBtn())
            .clicksOn(onAbookPage().groupsBlock().sharedContacts())
            .shouldSee(onAbookPage().toolbarBlock())
            .clicksOn(onAbookPage().contacts().get(0));
        user.defaultSteps().shouldHasText(onAbookPage().contactPopup().description(), newComment);
    }

    @Test
    @Title("Должны видеть должность общего контакта в карточке контакта")
    @TestCaseId("2379")
    public void shouldSeePostInContactsCard() {
        user.defaultSteps().opensFragment(CONTACTS)
            .clicksOn(onAbookPage().groupsBlock().sharedContacts())
            .shouldSee(onAbookPage().contacts().waitUntil(not(empty())).get(0))
            .clicksOnElementWithText(onAbookPage().contacts(), FROM_EMAIL)
            .shouldHasText(onAbookPage().contactPopup().contactPost().get(1), CONTACT_ROLE);

    }

    @Test
    @Title("Должны найти личный контакт со страницы общих контактов")
    @TestCaseId("2372")
    public void shouldFindPersonalContactFromSharedContacts() {
        Contact contact = user.abookSteps().createDefaultContact();
        user.apiAbookSteps().addNewContacts(contact);
        String contactName = contact.getName().getFirst();
        searchContact(onAbookPage().groupsBlock().sharedContacts(), contactName);
    }

    @Test
    @Title("Должны найти общий контакт со страницы личных контактов")
    @TestCaseId("2373")
    public void shouldFindSharedContactFromPersonalContacts() {
        searchContact(onAbookPage().groupsBlock().personalContacts(), FROM_EMAIL);
    }

    @Test
    @Title("Должны найти общий контакт со страницы общих контактов")
    @TestCaseId("2371")
    @UseDataProvider("contactsGroups")
    public void shouldFindSharedContactFromSharedContacts() {
        searchContact(onAbookPage().groupsBlock().sharedContacts(), FROM_EMAIL);
    }

    @Step("Проверяем что в личных контактах есть всего один контакт с нужным комментарием")
    //TODO: убрать рефреш если кто-нибудь когда-нибудь починит DARIA-57180
    private void shouldSeeContactWithCommentInPersonalAbook(String comment) {
        user.defaultSteps().refreshPage();
        user.abookSteps().shouldSeePersonalContactsCounter(1);
        user.defaultSteps().clicksOn(onAbookPage().groupsBlock().personalContacts())
            .shouldSee(onAbookPage().groupsBlock().activePersonalContacts());
        user.abookSteps().shouldSeeNumberOfContacts(1);
        user.defaultSteps().clicksOn(onAbookPage().contacts().get(0))
            .shouldHasText(onAbookPage().contactPopup().description(), comment);
    }

    @Step("Ищем контакт {1} находясь в {0}")
    private void searchContact(MailElement contactsGroup, String contactName) {
        user.defaultSteps().opensFragment(CONTACTS)
            .clicksOn(contactsGroup)
            .clicksOn(onMessagePage().mail360HeaderBlock().searchContactInput())
            .inputsTextInElement(onMessagePage().mail360HeaderBlock().searchContactInput(), contactName)
            .shouldSee(onMessagePage().mail360HeaderBlock().clearContactInput())
            .shouldSee(onAbookPage().searchResultsHeader())
            .shouldSeeThatElementHasText(onAbookPage().contacts().get(0), contactName);
    }

    @Test
    @Title("Должны видеть тулбар действий только для личных контактов")
    @TestCaseId("2375")
    public void shouldSeeToolbarForPersonalContactsOnly() {
        Contact contact = user.abookSteps().createDefaultContact().withName(new Name().withFirst(COMMON_CONTACT));
        user.apiAbookSteps().addNewContacts(contact);
        searchContact(onAbookPage().groupsBlock().personalContacts(), COMMON_CONTACT);
        user.defaultSteps().clicksOn(onAbookPage().contacts().get(1).contactCheckBox())
            .shouldNotSee(onAbookPage().toolbarBlock().deleteContactButton())
            .clicksOn(onAbookPage().contacts().get(0).contactCheckBox())
            .shouldNotSee(onAbookPage().toolbarBlock().deleteContactButton())
            .clicksOn(onAbookPage().contacts().get(1).contactCheckBox())
            .shouldSee(onAbookPage().toolbarBlock().deleteContactButton());
    }

    @Test
    @Title("Должны открыть композ с несколькими адресатами из общих контактов")
    @TestCaseId("2377")
    public void shouldOpenComposeWithTwoSharedContacts() {
        user.defaultSteps().opensFragment(CONTACTS)
            .clicksOn(onAbookPage().groupsBlock().sharedContacts());
        String contactName1 = onAbookPage().contacts().get(5).name().getText();
        String contactName2 = onAbookPage().contacts().get(6).name().getText();
        user.defaultSteps().clicksOn(
            onAbookPage().contacts().get(5).contactCheckBox(),
            onAbookPage().contacts().get(6).contactCheckBox(),
            onAbookPage().composeButton()
        )
            .shouldSee(onComposePopup().expandedPopup());
        user.composeSteps().shouldSeeSendToAreaHas(contactName1, contactName2);
    }
}

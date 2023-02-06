package ru.yandex.autotests.innerpochta.tests.abook;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author arttimofeev
 */
@Aqua.Test
@Title("Создание групп контактов")
@Description("Тесты на abook")
@Features(FeaturesConst.ABOOK)
@Stories(FeaturesConst.GENERAL)
@Tag(FeaturesConst.ABOOK)
public class ContactsGroupTest extends BaseTest {

    private String group = Utils.getRandomString();
    private Contact contact1, contact2, contact3;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        contact1 = user.abookSteps().createDefaultContact();
        contact2 = user.abookSteps().createDefaultContact();
        contact3 = user.abookSteps().createDefaultContact();
        user.apiAbookSteps().addNewContacts(contact1, contact2, contact3);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.CONTACTS);
        user.defaultSteps().clicksOn(onAbookPage().groupsBlock().showAllContactsLink());
    }

    @Test
    @Title("Создаём новую группу с существующим контактом")
    @TestCaseId("1181")
    public void shouldCreateNewGroupWithContacts() {
        user.defaultSteps().clicksOn(onAbookPage().groupsBlock().createGroupButton())
            .inputsTextInElement(onAbookPage().createNewGroupPopup().groupNameInput(), group);
        user.abookSteps().addsContactsToGroup(0, 1, 2);
        user.defaultSteps()
            .clicksOn(onAbookPage().createNewGroupPopup().createGroupButton())
            .shouldNotSee(onAbookPage().createNewGroupPopup())
            .shouldSee(onAbookPage().groupsBlock())
            .clicksOnElementWithText(onAbookPage().groupsBlock().groups(), group);
        checksContactsGroup();
    }

    @Test
    @Title("Создаём группу и после этого добавляем туда контакты")
    @TestCaseId("1182")
    public void shouldCreateNewGroupAndAddContacts() {
        user.defaultSteps().clicksOn(onAbookPage().groupsBlock().createGroupButton())
            .inputsTextInElement(onAbookPage().createNewGroupPopup().groupNameInput(), group)
            .clicksOn(onAbookPage().createNewGroupPopup().createGroupButton())
            .shouldSee(onAbookPage().groupsBlock())
            .shouldSee(onAbookPage().groupsBlock().groups().get(0))
            .clicksOn(onAbookPage().groupsBlock().showAllContactsLink());
        user.abookSteps().checksAllContactsCheckBoxes()
            .addsContactToGroup(group);
        user.defaultSteps().refreshPage()
            .clicksOnElementWithText(onAbookPage().groupsBlock().groups(), group)
            .waitInSeconds(1);
        checksContactsGroup();
    }

    @Test
    @Title("Добавляем контакт в группу драг-н-дропом")
    @TestCaseId("1086")
    public void shouldDragAndDropContactIntoGroup() {
        user.apiAbookSteps().addNewAbookGroup(group);
        user.defaultSteps().refreshPage()
            .shouldSee(onAbookPage().groupsBlock())
            .dragAndDrop(
                onAbookPage().contacts().get(0).name(),
                onAbookPage().groupsBlock().groups().get(0)
            )
            .shouldSee(onAbookPage().groupsBlock());
        user.abookSteps().shouldSeeGroupCounter(group, 1);
    }

    @Test
    @Title("Проверяем кнопку “Написать письмо“ из карточки контактов в абуке")
    @TestCaseId("1084")
    public void shouldSendMsgToContactFromContactCard() {
        user.defaultSteps()
            .clicksOnElementWithText(onAbookPage().contacts(), user.abookSteps().getFullName(contact1))
            .shouldSee(onAbookPage().contactPopup())
            .clicksOn(onAbookPage().contactPopup().composeBtn())
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        user.composeSteps().shouldSeeSendToAreaHas(user.abookSteps().getFullName(contact1));
    }

    private void checksContactsGroup() {
        user.defaultSteps().clicksOn(onAbookPage().contacts().waitUntil(not(empty())).get(0).contactCheckBox());
        user.abookSteps().shouldSeeNumberOfContacts(3)
            .shouldSeeGroupCounter(group, 3)
            .shouldSeeContact(contact1)
            .shouldSeeContact(contact2)
            .shouldSeeContact(contact3);
    }
}

package ru.yandex.autotests.innerpochta.tests.abook;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomAddress;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Попап «Все контакты» в композе")
@Features(FeaturesConst.ABOOK)
@Tag(FeaturesConst.ABOOK)
@Stories(FeaturesConst.GENERAL)
public class AllContactsPopupInComposeTest extends BaseTest {

    private Contact contact;
    private Contact contact2;
    private String groupName;

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
    public void prepare() {
        contact = user.abookSteps().createContactWithParametrs("0" + getRandomString(), getRandomAddress());
        contact2 = user.abookSteps().createContactWithParametrs("1" + getRandomString(), getRandomAddress());
        groupName = getRandomName();
        user.apiAbookSteps().addNewContacts(
            contact,
            contact2,
            user.abookSteps().createDefaultContact(),
            user.abookSteps().createDefaultContact()
        );
        List<Contact> contacts = user.apiAbookSteps().getPersonalContacts();
        user.apiAbookSteps().removeAllAbookGroups()
            .addNewAbookGroupWithContacts(groupName, contacts.get(0), contacts.get(1));
        user.loginSteps().forAcc(lock.firstAcc()).logins(COMPOSE);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().popupTo())
            .clicksOn(onComposePopup().abookBtn())
            .shouldSee(onComposePopup().abookPopup());
    }

    @Test
    @Title("Выбираем группу по клику на группу контакта")
    @TestCaseId("2825")
    public void shouldSeeGroup() {
        user.defaultSteps().clicksOn(onComposePopup().abookPopup().contacts().get(0).groupLabel().get(0))
            .shouldHasText(onComposePopup().abookPopup().selectGroupBtn(), groupName)
            .shouldSeeElementsCount(onComposePopup().abookPopup().abookAdressesCheckboxList(), 2)
            .shouldHasText(onComposePopup().abookPopup().contacts().get(0).name(), contact.getName().getFirst())
            .shouldHasText(onComposePopup().abookPopup().contacts().get(1).name(), contact2.getName().getFirst());
    }

    @Test
    @Title("Должны находить контакты по поиску")
    @TestCaseId("3931")
    public void shouldFoundContact() {
        user.defaultSteps()
            .inputsTextInElement(onComposePopup().abookPopup().searchInput(), contact.getName().getFirst())
            .shouldSeeElementsCount(onComposePopup().abookPopup().abookAdressesCheckboxList(), 1)
            .shouldHasText(onComposePopup().abookPopup().contacts().get(0).name(), contact.getName().getFirst());
    }
}

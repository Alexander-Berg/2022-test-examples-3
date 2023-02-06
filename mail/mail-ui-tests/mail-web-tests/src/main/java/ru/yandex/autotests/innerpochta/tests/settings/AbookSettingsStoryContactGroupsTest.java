package ru.yandex.autotests.innerpochta.tests.settings;

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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

@Aqua.Test
@Title("Создание/удаление/изменение групп контактов")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SETTINGS_ABOOK)
public class AbookSettingsStoryContactGroupsTest extends BaseTest {

    private static final String GROUP_URL = "#contacts/group";
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
    public void logIn() throws IOException {
        groupName = getRandomName();
        user.apiAbookSteps().addNewContacts(
            user.abookSteps().createDefaultContact(),
            user.abookSteps().createDefaultContact(),
            user.abookSteps().createDefaultContact()
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_ABOOK);
    }

    @Test
    @Title("Создание группы")
    @TestCaseId("1712")
    public void testCreateContactGroupFromAbookSettingsPage() {
        user.settingsSteps().deletesContactsGroup();
        user.defaultSteps().clicksOn(onAbookSettingsPage().blockSetupAbook().groupsManage().createGroupButton())
            .inputsTextInElement(onAbookSettingsPage().newGroupPopup().groupName(), groupName)
            .clicksOn(onAbookSettingsPage().newGroupPopup().сreateBtn());
        user.settingsSteps().shouldSeeGroupWithName(groupName);
        user.defaultSteps().shouldSee(onAbookSettingsPage().blockSetupAbook().groupsManage().createdGroups().get(0))
            .shouldSeeThatElementTextEquals(onAbookSettingsPage().blockSetupAbook().groupsManage()
                .createdGroups().get(0).groupName(), groupName);
        user.defaultSteps().opensFragment(QuickFragments.CONTACTS);
        user.abookSteps().clicksOnGroup(groupName);
        user.defaultSteps().shouldBeOnUrl(containsString(GROUP_URL));
    }

    @Test
    @Title("Переименование группы")
    @TestCaseId("1713")
    public void shouldRenameContactGroupFromAbookSettingsPage() {
        clearContactGroupsAndAddNewOne();
        user.defaultSteps().clicksOnElementWithText(
            onAbookSettingsPage().blockSetupAbook().groupsManage().createdGroups(), groupName);
        user.defaultSteps().clicksOn(onAbookSettingsPage().blockSetupAbook().groupsManage().editGroupButton());
        groupName = getRandomString();
        user.defaultSteps().inputsTextInElement(onAbookSettingsPage().newGroupPopup().groupName(), groupName)
            .clicksOn(onAbookSettingsPage().newGroupPopup().сreateBtn());
        user.settingsSteps().shouldSeeGroupWithName(groupName);
        user.defaultSteps().opensFragment(QuickFragments.CONTACTS);
        user.abookSteps().clicksOnGroup(groupName);
        user.defaultSteps().shouldBeOnUrl(containsString(GROUP_URL));
    }

    @Test
    @Title("Удаление группы")
    @TestCaseId("1714")
    public void shouldDeleteContactGroupFromAbookSettingsPage() {
        clearContactGroupsAndAddNewOne();
        user.defaultSteps().clicksOn(onAbookSettingsPage().blockSetupAbook().groupsManage().createdGroups().get(0))
            .clicksOn(onAbookSettingsPage().blockSetupAbook().groupsManage().deleteGroupButton())
            .opensFragment(QuickFragments.CONTACTS)
            .shouldNotSeeElementInList(onAbookPage().groupsBlock().groups(), groupName);
    }

    @Test
    @Title("Счётчик группы")
    @TestCaseId("1715")
    public void shouldUpdateCounterFromAbookSettingsPage() {
        clearContactGroupsAndAddNewOne();
        user.defaultSteps().shouldSee(
            onAbookSettingsPage().blockSetupAbook().groupsManage().createdGroups().get(0).groupCounter())
            .shouldSeeThatElementTextEquals(
                onAbookSettingsPage().blockSetupAbook().groupsManage().createdGroups().get(0).groupCounter(), "0")
            .opensFragment(QuickFragments.CONTACTS);
        user.abookSteps().checksAllContactsCheckBoxes();
        user.defaultSteps().shouldSee(onAbookPage().toolbarBlock().addContactToGroupButton());
        user.abookSteps().addsContactToGroup(groupName);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_ABOOK)
            .shouldSee(
                onAbookSettingsPage()
                    .blockSetupAbook().groupsManage().createdGroups().waitUntil(not(empty())).get(0).groupCounter()
            )
            .shouldSeeThatElementTextEquals(
                onAbookSettingsPage().blockSetupAbook().groupsManage().createdGroups().get(0).groupCounter(), "3");
    }

    private void clearContactGroupsAndAddNewOne() {
        user.apiAbookSteps().removeAllAbookGroups()
            .addNewAbookGroup(groupName);
        user.defaultSteps().refreshPage();
        user.settingsSteps().shouldSeeGroupWithName(groupName);
    }
}

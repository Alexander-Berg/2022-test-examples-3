package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
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
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Name;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllCustomFoldersRule.removeAllCustomFolders;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllFiltersRule.removeAllFiltersRule;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllLabelsRule.removeAllLabelsRule;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVEL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_LABEL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;


/**
 * @author sbdsh
 */
@Aqua.Test
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FILTERS_SETTINGS)
@Title("Тесты на настройки правил")
public class FiltersSettingsTests extends BaseTest {

    private static final String EMAIL = "testbot2@yandex.ru";
    private static String[] FILTER_CONDITIONS = {"Кому", "Копия", "Кому или копия"};
    private Contact contactWithEmail;
    private String name;

    public AccLockRule lock = AccLockRule.use().useTusAccount();
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
        name = getRandomString();
        contactWithEmail = user.abookSteps().createDefaultContact()
            .withName(new Name().withFirst(name)
                .withMiddle(Utils.getRandomString())
                .withLast(Utils.getRandomString())
            )
            .withEmail(singletonList(new Email().withValue(EMAIL)));
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.CONTACTS);
        user.abookSteps().addsContact(contactWithEmail);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FILTERS);
    }

    @Test
    @Title("Драг-н-дроп правил")
    @TestCaseId("2501")
    public void shouldChangeFiltersOrder() {
        Folder folder = user.apiFoldersSteps().createNewFolder(getRandomString());
        createFilterToFolder(getRandomString(), folder);
        createFilterToFolder(getRandomString(), folder);
        createFilterToFolder(getRandomString(), folder);
        user.defaultSteps().refreshPage();
        String condition = onFiltersOverview().createdFilterBlocks().get(0).сonditionContent().getText();
        user.defaultSteps().dragAndDrop(
            onFiltersOverview().createdFilterBlocks().get(0),
            onFiltersOverview().createdFilterBlocks().get(1)
        )
            .shouldHasText(onFiltersOverview().createdFilterBlocks().get(1).сonditionContent(), condition);
    }

    @Test
    @Title("Удалить фильтр из списка")
    @TestCaseId("2521")
    public void shouldNotMoveMessageAfterDeletingFilter() {
        Folder folder = user.apiFoldersSteps().createNewFolder(getRandomString());
        String subject = createFilterToFolder(lock.firstAcc().getLogin(), folder);
        user.defaultSteps().refreshPage();
        user.filtersSteps().clicksOnDeleteFilter();
        user.defaultSteps().clicksOn(onFiltersOverview().deleteFilterPopUp().deleteFilterButton());
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc().getSelfEmail(), subject, "");
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Работа настройки «Не применять остальные правила»")
    @TestCaseId("3375")
    public void shouldWorkDontApplyOtherFiltersSetting() {
        Label lastLabel = user.apiLabelsSteps().addNewLabel(getRandomString(), LABELS_PARAM_GREEN_COLOR);
        createLabelAndFilter();
        String subject = createLabelAndFilter();
        user.apiFiltersSteps().createFilterForFolderOrLabel(
            lock.firstAcc().getLogin(),
            subject,
            FILTERS_ADD_PARAM_MOVE_LABEL,
            lastLabel.getLid(),
            FILTERS_ADD_PARAM_CLICKER_MOVEL,
            false
        );
        user.defaultSteps().refreshPage();
        user.filtersSteps().clicksOnFilter(1);
        user.defaultSteps().turnTrue(onFiltersCreationPage().setupFiltersCreate().dontApplyAnyOtherFilter());
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(subject);
        user.defaultSteps().shouldNotSeeElementInList(
            onMessagePage().displayedMessages().list().get(0).labels(), lastLabel.getName()
        );
    }

    @Test
    @Title("Проверить работу фильтра, перемещающего письмо в папку")
    @TestCaseId("3380")
    public void shouldMoveMessageToFolderByFilter() {
        String subject = getRandomString();
        user.defaultSteps().clicksOn(onFiltersOverview().createNewFilterButton()).inputsTextInElement(
            onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(0).inputCondition(),
            lock.firstAcc().getLogin()
        );
        String folder = user.filtersSteps().chooseToMoveInRandomlyCreatedFolder();
        user.defaultSteps().shouldSee(onFiltersCreationPage().setupFiltersCreate().submitFilterButton());
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.apiMessagesSteps().sendMailWithNoSaveWithoutCheck(lock.firstAcc().getSelfEmail(), subject, "");
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().openFolders().opensCustomFolder(folder);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Проверить работу фильтра, вешающего метку на письмо")
    @TestCaseId("3381")
    public void shouldPutLabelOnMessageByFilter() {
        user.defaultSteps().clicksOn(onFiltersOverview().createNewFilterButton())
            .inputsTextInElement(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                    .conditionsList().get(0).inputCondition(),
                lock.firstAcc().getLogin()
            );
        user.filtersSteps().chooseToPutRandomlyCreatedMark();
        user.defaultSteps().shouldSee(onFiltersCreationPage().setupFiltersCreate().submitFilterButton());
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.apiMessagesSteps().sendMailWithNoSaveWithoutCheck(
            lock.firstAcc().getSelfEmail(),
            getRandomString(),
            ""
        );
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .refreshPage()
            .shouldSee(onMessagePage().displayedMessages().list().get(0).labels().get(0));
    }

    @Test
    @Title("Проверить работу фильтра, помечающего прочитанным")
    @TestCaseId("3388")
    public void shouldMarkMessageAsRead() {
        user.defaultSteps().clicksOn(onFiltersOverview().createNewFilterButton())
            .inputsTextInElement(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                    .conditionsList().get(0).inputCondition(),
                lock.firstAcc().getLogin()
            )
            .deselects(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().moveToFolderCheckBox())
            .turnTrue(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().markAsReadCheckBox());
        user.defaultSteps().shouldSee(onFiltersCreationPage().setupFiltersCreate().submitFilterButton());
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.apiMessagesSteps().sendMailWithNoSaveWithoutCheck(
            lock.firstAcc().getSelfEmail(),
            getRandomString(),
            ""
        );
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .refreshPage()
            .shouldSee(onMessagePage().displayedMessages().list().get(0).messageRead());
    }

    @Test
    @Title("Проверить работу фильтра, удаляющего письмо")
    @TestCaseId("3387")
    public void shouldDeleteMessageByFilter() {
        String subject = getRandomString();
        user.defaultSteps().clicksOn(onFiltersOverview().createNewFilterButton()).inputsTextInElement(
            onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(0).inputCondition(),
            lock.firstAcc().getLogin()
        )
            .deselects(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().moveToFolderCheckBox())
            .turnTrue(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().deleteCheckBox());
        user.defaultSteps().shouldSee(onFiltersCreationPage().setupFiltersCreate().submitFilterButton());
        user.filtersSteps().submitsFilter(lock.firstAcc());
        user.apiMessagesSteps().sendMailWithNoSaveWithoutCheck(lock.firstAcc().getSelfEmail(), subject, "");
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Удалить пустую метку с правилом")
    @TestCaseId("2492")
    public void shouldDeleteLabelWithFilter() {
        user.defaultSteps().clicksOn(onFiltersOverview().createNewFilterButton()).inputsTextInElement(
            onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(0).inputCondition(),
            lock.firstAcc().getLogin()
        );
        user.filtersSteps().chooseToPutRandomlyCreatedMark();
        user.defaultSteps().shouldSee(onFiltersCreationPage().setupFiltersCreate().submitFilterButton());
        user.filtersSteps().submitsFilter(lock.firstAcc());
        openLabelSettingsAndDelete();
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().deleteLabelPopUpOld().myFilter())
            .shouldSee(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions());
        openLabelSettingsAndDelete();
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().deleteLabelPopUpOld().toSettings())
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.SETTINGS_FILTERS);
        openLabelSettingsAndDelete();
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().deleteLabelPopUpOld().cancelBtnOld())
            .shouldSee(onFoldersAndLabelsSetup().setupBlock().labels().userLabelsList().get(0))
            .clicksOn(
                onFoldersAndLabelsSetup().setupBlock().labels().deleteLabel(),
                onFoldersAndLabelsSetup().deleteLabelPopUpOld().deleteBtnOld()
            )
            .shouldSeeElementsCount(onFoldersAndLabelsSetup().setupBlock().labels().userLabelsList(), 0);

    }

    @Test
    @Title("Показываем саджест при создании правила")
    @TestCaseId("2181")
    public void shouldSeeSuggestOnCreateRulePage() {
        user.filtersSteps().clicksOnCreateNewFilter();
        user.defaultSteps().clicksOn(
            onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList().get(0)
                .deleteConditionButton()
        );
        for (String condition : FILTER_CONDITIONS) {
            checkSuggest(condition);
        }
    }

    @Step("Создаем метку и фильтр для нее")
    private String createLabelAndFilter() {
        String subject;
        user.apiFiltersSteps().createFilterForFolderOrLabel(
            lock.firstAcc().getLogin(),
            subject = getRandomString(),
            FILTERS_ADD_PARAM_MOVE_LABEL,
            user.apiLabelsSteps().addNewLabel(getRandomString(), LABELS_PARAM_GREEN_COLOR).getLid(),
            FILTERS_ADD_PARAM_CLICKER_MOVEL,
            false
        );
        return subject;
    }

    @Step("Создаем фильтр для переноса в папку")
    private String createFilterToFolder(String address, Folder folder) {
        String subject;
        user.apiFiltersSteps().createFilterForFolderOrLabel(
            address,
            subject = getRandomString(),
            FILTERS_ADD_PARAM_MOVE_FOLDER,
            folder.getFid(),
            FILTERS_ADD_PARAM_CLICKER_MOVE,
            false
        );
        return subject;
    }

    @Step("Открываем страницу настроек меток и пробуем удалить метку с фильтром")
    private void openLabelSettingsAndDelete() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS)
            .clicksOn(
                onFoldersAndLabelsSetup().setupBlock().labels().userLabelsList().get(0),
                onFoldersAndLabelsSetup().setupBlock().labels().deleteLabel()
            )
            .shouldSee(onFoldersAndLabelsSetup().deleteLabelPopUpOld());
    }

    @Step("Должны видеть саджест при указании условия «{0}»")
    private void checkSuggest(String condition) {
        user.defaultSteps()
            .clicksOn(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().addConditionButton())
            .clicksOn(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList().get(0)
                    .firstConditionDropDown()
            )
            .shouldSee(onMessagePage().selectItem())
            .clicksOnElementWithText(onMessagePage().selectItem(), condition);
        MailElement input = onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList()
            .get(0).inputCondition();
        user.defaultSteps().clicksOn(input)
            .inputsTextInElement(input, name)
            .shouldSeeElementsCount(onMessagePage().suggestList().waitUntil(not(empty())), 1)
            .clicksOn(onMessagePage().suggestList().get(0))
            .shouldNotSee(onMessagePage().suggestList())
            .clicksOn(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList().get(0)
                    .deleteConditionButton()
            );
    }

    @Test
    @Title("Переход на страницу создания простого правила на перемещение в папку")
    @TestCaseId("2512")
    public void shouldSeeSimpleRuleForMoving() {
        user.defaultSteps().clicksOn(onFiltersOverview().createSimpleFilterForMoving())
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_FILTERS_CREATE_SIMPLE)
            .shouldSee(
                user.pages().FiltersOverviewSettingsPage().createSimpleFilter().cancelButton(),
                user.pages().FiltersOverviewSettingsPage().filtersBuilder()
            );
    }

    @Test
    @Title("Переход на страницу создания простого правила на пометку меткой")
    @TestCaseId("2512")
    public void shouldSeeSimpleRuleForLabeling() {
        user.defaultSteps().clicksOn(onFiltersOverview().createSimpleFilterForLabeling())
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_FILTERS_CREATE_SIMPLE_LABEL)
            .shouldSee(
                user.pages().FiltersOverviewSettingsPage().createSimpleFilter().cancelButton(),
                user.pages().FiltersOverviewSettingsPage().filtersBuilder()
            );
    }

    @Test
    @Title("Переход на страницу создания простого правила на удаление")
    @TestCaseId("2512")
    public void shouldSeeSimpleRuleForDeleting() {
        user.defaultSteps().clicksOn(onFiltersOverview().createSimpleFilterForDeleting())
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_FILTERS_CREATE_SIMPLE_DELETE)
            .shouldSee(
                user.pages().FiltersOverviewSettingsPage().createSimpleFilter().cancelButton(),
                user.pages().FiltersOverviewSettingsPage().filtersBuilder()
            );
    }
}

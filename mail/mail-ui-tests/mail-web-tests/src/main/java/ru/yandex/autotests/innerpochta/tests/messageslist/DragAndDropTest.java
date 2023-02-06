package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.KeysOwn;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на драг-н-дроп")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class DragAndDropTest extends BaseTest {

    private static final String FULL_SIZE = "300";
    private static final String COMPACT_SIZE = "60";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private AddFolderIfNeedRule addFolder = AddFolderIfNeedRule.addFolderIfNeed(() -> user);

    private String subject;
    private String labelName;
    private String body;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user))
        .around(addFolder);

    @Before
    public void setUp() {
        subject = getRandomString();
        labelName = getRandomString();
        body = getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, body);
        user.apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_GREEN_COLOR);
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем компактную левую колонку и включаем 2 pane",
            of(
                SIZE_LAYOUT_LEFT, FULL_SIZE,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubjectWithWaiting(subject);
    }

    @Test
    @Title("Драг-н-дроп письма на метку")
    @TestCaseId("3302")
    public void shouldSeeLabelInMessage() {
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().waitUntil(not(empty())).get(0).subject(),
            onMessagePage().labelsNavigation().userLabels().get(0).labelName()
        );
        user.messagesSteps().shouldSeeLabelsOnMessage(labelName, subject);
    }

    @Test
    @Title("Драг-н-дроп письма в папку")
    @TestCaseId("3303")
    public void shouldDragMessageInFolder() {
        user.leftColumnSteps().openFolders();
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().waitUntil(not(empty())).get(0).subject(),
            onMessagePage().foldersNavigation().customFolders().get(2).customFolderName()
        );
        user.leftColumnSteps().opensCustomFolder(addFolder.getFirstFolder().getName());
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Драг-н-дроп письма на кнопку «Прочитано»")
    @TestCaseId("3304")
    public void shouldSeeReadMessage() {
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().waitUntil(not(empty())).get(0).subject(),
            onMessagePage().toolbar().markAsReadButton()
        );
        user.defaultSteps().shouldNotSee(onMessagePage().displayedMessages().list().get(0).messageUnread());
    }

    @Test
    @Title("Драг-н-дроп со скроллом письма на папку")
    @TestCaseId("4481")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-63172")
    public void shouldScrollAndMoveToFolder() {
        dragNDropWithScrollToFolderAndCheckMessageInFolder(false);
    }

    @Test
    @Title("Драг-н-дроп со скроллом письма на метку")
    @TestCaseId("4481")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-63172")
    public void shouldScrollAndMoveToLabel() {
        dragNDropWithScrollToLabelAndCheckLabel();
    }

    @Test
    @Title("Драг-н-дроп со скроллом письма на папку c компактной левой колонкой")
    @TestCaseId("4481")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-63172")
    public void shouldScrollAndMoveToFolderCompactLeftPanel() {
        enableCompactLeftPanel();
        dragNDropWithScrollToFolderAndCheckMessageInFolder(true);
    }

    @Test
    @Title("Драг-н-дроп со скроллом письма на метку c компактной левой колонкой")
    @TestCaseId("4481")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-63172")
    public void shouldScrollAndMoveToLabelCompactLeftPanel() {
        enableCompactLeftPanel();
        dragNDropWithScrollToLabelAndCheckLabel();
    }

    @Test
    @Title("Драг-н-дроп со скроллом письма на папку в 3pane")
    @TestCaseId("4481")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-63172")
    public void shouldScrollAndMoveToFolder3Pane() {
        enable3Pane();
        dragNDropWithScrollToFolderAndCheckMessageInFolder(false);
    }

    @Test
    @Title("Драг-н-дроп со скроллом письма на метку в 3pane")
    @TestCaseId("4481")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-63172")
    public void shouldScrollAndMoveToLabel3Pane() {
        enable3Pane();
        dragNDropWithScrollToLabelAndCheckLabel();
    }

    @Test
    @Title("Драг-н-дроп письма на кнопку «Переслать»")
    @TestCaseId("2010")
    public void shouldSeeForwardMessageAfterDnD() {
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().waitUntil(not(empty())).get(0).subject(),
            onMessagePage().toolbar().forwardButton()
        )
            .shouldHasText(onComposePopup().expandedPopup().popupTo(), "")
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), body);
        user.composeSteps().clicksOnAddEmlBtn()
            .shouldSeeSubject("Fwd: " + subject)
            .shouldSeeMessageAsAttachment(0, subject);
    }

    @Test
    @Title("Драг-н-дроп письма на кнопку «Удалить»")
    @TestCaseId("2010")
    public void shouldNotSeeMessageAfterDnDOnDeleteButton() {
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().waitUntil(not(empty())).get(0).subject(),
            onMessagePage().toolbar().deleteButton()
        );
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(TRASH);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Драг-н-дроп письма на кнопку «Это спам»")
    @TestCaseId("2010")
    public void shouldSeeMessageAsSpamAfterDnD() {
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().waitUntil(not(empty())).get(0).subject(),
            onMessagePage().toolbar().spamButton()
        );
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.defaultSteps().opensFragment(SPAM);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Драг-н-дроп письма на кнопку «Переслать» в 3pane")
    @TestCaseId("1051")
    public void shouldSeeForwardMessageAfterDnD3Pane() {
        enable3Pane();
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().get(0).subject(),
            onMessagePage().toolbar().forwardButton()
        )
            .shouldHasText(onComposePopup().expandedPopup().popupTo(), "")
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), body);
        user.composeSteps().clicksOnAddEmlBtn()
            .shouldSeeSubject("Fwd: " + subject)
            .shouldSeeMessageAsAttachment(0, subject);
    }

    @Step("Перетаскиваем со скролом в папку и проверяем, что письмо переместилось")
    private void dragNDropWithScrollToFolderAndCheckMessageInFolder(boolean isCompactLeftPanel) {
        user.defaultSteps().setsWindowSize(1800, 300); // Это какой-то костыль
        user.hotkeySteps().pressSimpleHotKey(KeysOwn.key(Keys.END));
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().get(0).subject(),
            isCompactLeftPanel
                ? onMessagePage().foldersNavigation().customFolders().get(2).hover().waitUntil(isPresent())
                : onMessagePage().foldersNavigation().customFolders().get(2).customFolderName().waitUntil(isPresent())
        );
        user.leftColumnSteps().opensCustomFolder(0);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Step("Перетаскиваем со скролом на метку и проверяем, что метка проставилась")
    private void dragNDropWithScrollToLabelAndCheckLabel() {
        user.defaultSteps().setsWindowSize(1800, 300); // Это какой-то костыль
        user.hotkeySteps().pressSimpleHotKey(KeysOwn.key(Keys.END));
        user.defaultSteps().dragAndDrop(
            onMessagePage().displayedMessages().list().get(0).subject(),
            onMessagePage().labelsNavigation().userLabels().get(0).labelName()
        );
        user.messagesSteps().shouldSeeLabelsOnMessage(labelName, subject);
    }

    @Step("Включаем компактную левую колонку")
    private void enableCompactLeftPanel() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную левую колонку",
            of(SIZE_LAYOUT_LEFT, COMPACT_SIZE)
        );
        user.defaultSteps().refreshPage();
    }

    @Step("Включаем 3 pane")
    private void enable3Pane() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3 pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        user.defaultSteps().refreshPage();
    }
}

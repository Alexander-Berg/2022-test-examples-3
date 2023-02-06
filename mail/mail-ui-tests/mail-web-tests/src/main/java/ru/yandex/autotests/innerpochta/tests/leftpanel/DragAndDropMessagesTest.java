package ru.yandex.autotests.innerpochta.tests.leftpanel;

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
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.LEFT_PANEL_FULL_SIZE;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на драг-н-дроп папок")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.LEFT_PANEL)
public class DragAndDropMessagesTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private AddFolderIfNeedRule addFolder = AddFolderIfNeedRule.addFolderIfNeed(() -> user);

    private String subject;
    private String labelName;

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
        String body = getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, body);
        user.apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_GREEN_COLOR);
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем компактную левую колонку, включаем 2 pane, раскрываем все папки",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                FOLDERS_OPEN, user.apiFoldersSteps().getAllFids(),
                SIZE_LAYOUT_LEFT, LEFT_PANEL_FULL_SIZE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Драг-н-дроп письма на метку в узкой ЛК")
    @TestCaseId("2318")
    public void shouldDragMsgToLabel() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .turnTrue(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(1))
            .dragAndDrop(
                onMessagePage().displayedMessages().list().get(0).subject(),
                onMessagePage().labelsNavigation().userLabels().get(0)
            );
        user.messagesSteps().shouldSeeLabelsOnMessage(labelName, subject);
    }

    @Test
    @Title("Драг-н-дроп письма в папку в узкой ЛК")
    @TestCaseId("5245")
    public void shouldDragMessageToFolder() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .turnTrue(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(1))
            .dragAndDrop(
                onMessagePage().displayedMessages().list().get(0).subject(),
                onMessagePage().foldersNavigation().customFolders().get(2)
            );
        user.defaultSteps().onMouseHoverAndClick(
            user.pages().MessagePage().foldersNavigation().customFolders().get(2)
        );
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }
}

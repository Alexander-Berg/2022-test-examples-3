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
import ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule.addFolderIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule.addMessageIfNeed;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASS_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.SERVER_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.HIDE_EMPTY_FOLDERS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_ASIDE_EXPANDED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тест на узкую левую колонку")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.COMPACT_LEFT_PANEL)
public class CompactLeftPanelTest extends BaseTest {

    private Folder folder;
    private Label label;
    private Message msg;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private AddFolderIfNeedRule addFolderIfNeed = addFolderIfNeed(() -> user);
    private AddLabelIfNeedRule addLabelIfNeed = addLabelIfNeed(() -> user);
    private AddMessageIfNeedRule addMsgIfNeed = addMessageIfNeed(() -> user, () -> lock.firstAcc());

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user))
        .around(addFolderIfNeed)
        .around(addLabelIfNeed)
        .around(addMsgIfNeed);

    @Before
    public void setUp() {
        folder = addFolderIfNeed.getFirstFolder();
        label = addLabelIfNeed.getFirstLabel();
        msg = addMsgIfNeed.getFirstMessage();
        user.apiCollectorSteps().createNewCollector(MAIL_COLLECTOR, PASS_COLLECTOR, SERVER_COLLECTOR);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную ЛК и настройку показа папок только с непрочитанными, раскрываем все папки",
            of(
                HIDE_EMPTY_FOLDERS, false,
                FOLDERS_OPEN, user.apiFoldersSteps().getAllFids(),
                SIZE_LAYOUT_LEFT, 60,
                SIZE_ASIDE_EXPANDED, 350
            )
        );
        user.apiMessagesSteps().deleteAllMessagesInFolder(folder);
        user.apiLabelsSteps().markWithLabel(msg, label);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().shouldSee(onMessagePage().compactLeftPanel());
    }

    @Test
    @Title("Сворачиваем левую колонку")
    @TestCaseId("2293")
    public void shouldSeeCompactLeftPanel() {
        user.defaultSteps().shouldSee(
            onMessagePage().foldersNavigation(),
            onMessagePage().labelsNavigation(),
            onMessagePage().collectorsNavigation()
        );
        user.leftColumnSteps().shouldSeeFoldersCountOnHomePage(7)
            .shouldSeeLabelOnHomePage(label.getName().substring(0, 1).toUpperCase());
    }

    @Test
    @Title("Разворачиваем левую колонку")
    @TestCaseId("2295")
    public void shouldSeeFullLeftPanel() {
        user.defaultSteps().clicksOn(onMessagePage().toolbar().layoutSwitchBtn())
            .deselects(onMessagePage().layoutSwitchDropdown().compactLeftColumnSwitch())
            .shouldNotSee(onMessagePage().compactLeftPanel())
            .shouldSee(
                onMessagePage().foldersNavigation(),
                onMessagePage().labelsNavigation(),
                onMessagePage().collectorsNavigation()
            );
        user.leftColumnSteps().shouldSeeFoldersWithName(folder.getName());
    }

    @Test
    @Title("Должны видеть только папки с непрочитанными")
    @TestCaseId("2300")
    public void shouldSeeFolderWithUnreadMsg() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем настройку показа папок только с непрочитанными,",
            of(HIDE_EMPTY_FOLDERS, true)
        );
        user.defaultSteps().refreshPage();
        user.leftColumnSteps().shouldSeeFoldersCountOnHomePage(6);
        user.messagesSteps().movesMessageToFolder(msg.getSubject(), folder.getName());
        user.defaultSteps().clicksIfCanOn(onMessagePage().foldersNavigation().expandInboxFolders());
        user.leftColumnSteps().shouldSeeFoldersCountOnHomePage(7);
        user.defaultSteps().clicksOn(onMessagePage().foldersNavigation().hideInboxThread());
        user.leftColumnSteps().shouldSeeFoldersCountOnHomePage(6);
    }

    @Test
    @Title("Открываем пользовательскую папку")
    @TestCaseId("2304")
    public void shouldOpenCustomFolder() {
        user.messagesSteps().movesMessageToFolder(msg.getSubject(), folder.getName());
        user.defaultSteps().clicksOn(onMessagePage().foldersNavigation().customFolders().get(2));
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Проверяем счетчик непрочитанных")
    @TestCaseId("2294")
    public void shouldSeeCorrectInboxCounter() {
        user.leftColumnSteps().shouldSeeInboxUnreadCounterCompactLC(1);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.leftColumnSteps().shouldSeeInboxUnreadCounterCompactLC(2);
        user.messagesSteps().movesMessageToFolder(msg.getSubject(), folder.getName());
        user.leftColumnSteps().shouldSeeInboxUnreadCounterCompactLC(1);
        user.defaultSteps().clicksIfCanOn(onMessagePage().foldersNavigation().expandInboxFolders())
            .clicksOn(onMessagePage().foldersNavigation().customFolders().get(2))
            .shouldSeeThatElementHasText(onMessagePage().foldersNavigation().customFolders().get(2), "1");
    }

    @Test
    @Title("Проверяем контекстное меню")
    @TestCaseId("3143")
    public void shouldSeeContextMenu() {
        user.defaultSteps().rightClick(onMessagePage().foldersNavigation().inboxFolderLink())
            .shouldSee(onMessagePage().contextMenuFolder());
    }
}

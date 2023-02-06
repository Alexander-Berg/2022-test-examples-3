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
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL360_PAID;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;


@Aqua.Test
@Title("Настройки - резервная копия")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SETTINGS)
public class BackupTest extends BaseTest {

    private static final String BANNER_TEXT_BACKUP_IN_PROGRESS = "Идет резервное копирование почтового ящика. " +
            "Пока доступно только чтение писем.";
    private static final String BANNER_TEXT_BACKUP_RESTORE_TO_SOURCE = "Идет восстановление писем из " +
            "резервной копии. Письма будут постепенно появляться в исходных папках.";
    private static final String BANNER_TEXT_BACKUP_RESTORE_TO_RESTORED = "Идет восстановление писем из " +
            "резервной копии. Письма будут постепенно появляться в папке «Восстановленные».";
    private static final String NO_BACKUP_DATA = "Резервных копий нет.";
    private static final String MSG_SBJ_1 = "subj 1";
    private static final String MSG_SBJ_2 = "subj 2";
    private static final String MSG_SBJ_3 = "subj 3";

    private AccLockRule lock = AccLockRule.use().useTusAccount(MAIL360_PAID);
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
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 3);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_BACKUP);
    }

    @Test
    @Title("Создаем резервную копию")
    @TestCaseId("6066")
    public void shouldCreateBackup() {
        user.defaultSteps().clicksOn(onBackupSettingsPage().foldersToggler())
                .clicksOn(onBackupSettingsPage().createBtn())
                .clicksOn(onBackupSettingsPage().applyActionBtn())
                .shouldSee(onBackupSettingsPage().backupInProgressBanner())
                .shouldContainText(onBackupSettingsPage().backupInProgressBanner(), BANNER_TEXT_BACKUP_IN_PROGRESS)
                .shouldContainCSSAttributeWithValue(
                        onBackupSettingsPage().createBtn(),
                                "aria-disabled",
                                STATUS_TRUE
                )
                .shouldSeeWithWaiting(onMessagePage().statusLineBlock(), 20)
                .shouldNotContainText(onBackupSettingsPage().lastCopyInfo(), NO_BACKUP_DATA);
    }

    @Test
    @Title("Удаляем резервную копию")
    @TestCaseId("6067")
    public void shouldDeleteBackup() {
        createBackup();
        user.defaultSteps().clicksOn(onBackupSettingsPage().deleteBtn())
                .clicksOn(onBackupSettingsPage().applyActionBtn())
                .shouldSeeWithWaiting(onMessagePage().statusLineBlock(), 20)
                .clicksIfCanOn(onBackupSettingsPage().foldersToggler())
                .shouldContainText(onBackupSettingsPage().lastCopyInfo(), NO_BACKUP_DATA);
    }

    @Test
    @Title("Восстанавливаем резервную копию в исходную папку")
    @TestCaseId("6068")
    public void shouldRestoreBackupToSource() {
        createBackup();
        user.apiFoldersSteps().purgeFolder(user.apiFoldersSteps().getFolderBySymbol(INBOX));
        user.defaultSteps().clicksOn(onBackupSettingsPage().restoreBtn())
                .clicksOn(onBackupSettingsPage().restoreOptionsBtn().get(1))
                .clicksOn(onBackupSettingsPage().applyActionBtn());
        checkInterfaceWhileRestoring();
        user.defaultSteps()
                .shouldContainText(onBackupSettingsPage().backupRestoreBanner(), BANNER_TEXT_BACKUP_RESTORE_TO_SOURCE)
                .shouldSeeWithWaiting(onMessagePage().statusLineBlock(), 20);
        checkRestoredLetter(QuickFragments.INBOX_FOLDER);
    }

    @Test
    @Title("Восстанавливаем резервную копию в отдельную папку")
    @TestCaseId("6069")
    public void shouldRestoreBackupToRestored() {
        createBackup();
        user.apiFoldersSteps().purgeFolder(user.apiFoldersSteps().getFolderBySymbol(INBOX));
        user.defaultSteps().clicksOn(onBackupSettingsPage().restoreBtn())
                .clicksOn(onBackupSettingsPage().applyActionBtn());
        checkInterfaceWhileRestoring();
        user.defaultSteps()
                .shouldContainText(onBackupSettingsPage().backupRestoreBanner(), BANNER_TEXT_BACKUP_RESTORE_TO_RESTORED)
                .shouldSeeWithWaiting(onMessagePage().statusLineBlock(), 20);
        checkRestoredLetter(QuickFragments.RESTORED);
    }

    @Test
    @Title("Просматриваем удаленные письма")
    @TestCaseId("6070")
    public void shouldSeeDeletedLetters() {
        user.defaultSteps().clicksOn(onBackupSettingsPage().enotControl());
        user.apiFoldersSteps().purgeFolder(user.apiFoldersSteps().getFolderBySymbol(INBOX));
        user.defaultSteps().clicksOn(onBackupSettingsPage().hiddenLettersView());
        user.messagesSteps().shouldSeeMessageWithSubject(MSG_SBJ_1, MSG_SBJ_2, MSG_SBJ_3);
    }

    @Test
    @Title("Очищаем папку удаленных писем")
    @TestCaseId("6091")
    public void shouldPurgeHiddenTrashFolder() {
        user.defaultSteps().clicksOn(onBackupSettingsPage().enotControl());
        user.apiFoldersSteps().purgeFolder(user.apiFoldersSteps().getFolderBySymbol(INBOX));
        user.defaultSteps().clicksOn(onBackupSettingsPage().enotControl())
                .clicksOn(onBackupSettingsPage().enotDisableApplyBtn())
                .clicksOn(onBackupSettingsPage().enotControl())
                .clicksOn(onBackupSettingsPage().hiddenLettersView());
        user.messagesSteps().shouldNotSeeMessageWithSubject(MSG_SBJ_1, MSG_SBJ_2, MSG_SBJ_3);
    }

    @Step("Создаем резервную копию")
    private void createBackup() {
        user.apiBackupSteps().createBackup();
        user.defaultSteps().refreshPage()
                .shouldNotSee(onBackupSettingsPage().backupInProgressBanner());
    }

    @Step("Проверяем восстановленные письма")
    private void checkRestoredLetter(QuickFragments folder) {
        user.defaultSteps().opensFragment(folder);
        user.messagesSteps().shouldSeeMessageWithSubject(MSG_SBJ_1, MSG_SBJ_2, MSG_SBJ_3);
    }

    @Step("Проверяем интерфейс в процессе восстановления писем")
    private void checkInterfaceWhileRestoring() {
        user.defaultSteps()
                .shouldContainCSSAttributeWithValue(
                        onBackupSettingsPage().createBtn(),
                        "aria-disabled",
                        STATUS_TRUE
                )
                .shouldContainCSSAttributeWithValue(
                        onBackupSettingsPage().restoreBtn(),
                        "aria-busy",
                        STATUS_TRUE
                )
                .shouldContainCSSAttributeWithValue(
                        onBackupSettingsPage().deleteBtn(),
                        "aria-disabled",
                        STATUS_TRUE
                )
                .shouldSee(onBackupSettingsPage().backupRestoreBanner());
    }

}
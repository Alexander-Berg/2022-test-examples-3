package ru.yandex.autotests.innerpochta.barbet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.barbet.Create;
import ru.yandex.autotests.innerpochta.beans.barbet.Settings;
import ru.yandex.autotests.innerpochta.beans.barbet.Status;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.backup.restore.ApiRestore;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.barbet.BackupMatcher.activeBackupWithId;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.barbet.BackupMatcher.completeRestore;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[Barbet] Тесты на восстановление бэкапа")
@Credentials(loginGroup = "BarbetRestoreFromBackup")
@Features(MyFeatures.BARBET)
@Stories(MyStories.BACKUP)
public class RestoreTest extends BackupBaseTest {
    private String hiddenTrashFid;
    private String userFid;
    private String userFolderName;
    private String fixedMid;
    private String mid;
    private Long backupId;

    @Rule
    public DeleteFoldersRule deleteFolders = DeleteFoldersRule.with(authClient).all().before(true);

    private void restoreAndWait(final ApiRestore.MethodParam method) {
        restore().withMethod(method).post(shouldBe(ok200()));
        assertThat(authClient, withWaitFor(completeRestore(backupId)));
    }

    @Before
    public void prepareBackup() throws Exception {
        prepareRestoredFolder();

        userFolderName = Util.getRandomString();
        userFid = Mops.newFolder(authClient, userFolderName);
        hiddenTrashFid = folderList.fidBySymbol(Symbol.HIDDEN_TRASH);

        assertNotNull(hiddenTrashFid);

        List<String> mids = sendWith(authClient).viaProd().count(2).send().waitDeliver().getMids();
        fixedMid = mids.get(0);
        mid = mids.get(1);

        Mops.complexMove(authClient, userFid, new MidsSource(mids)).post(shouldBe(okSync()));
        updateSettings()
                .withFids(userFid)
                .post(shouldBe(ok200()))
                .as(Settings.class);
        backupId = create()
                .post(shouldBe(ok200()))
                .as(Create.class)
                .getBackupId();
        assertThat(authClient, withWaitFor(activeBackupWithId(backupId)));

        long backupMessageCount = status().get(shouldBe(ok200())).as(Status.class).getPrimary().getMessageCount();
        assertThat("Иное число писем в бекапе", backupMessageCount, equalTo((long) mids.size()));
    }

    @Test
    @Title("Письмо из бэкапа не должны восстанавливаться, если они не удалены")
    public void shouldDoNothingWhenMessagesNotDeleted() throws Exception {
        restoreAndWait(ApiRestore.MethodParam.FULL_HIERARCHY);

        FolderList folders = new FolderList(authClient);
        assertThat(folders.count(userFid), equalTo(2));
        assertThat(folders.count(folderList.defaultFID()), equalTo(0));
        assertThat(folders.count(folders.fidBySymbol(Symbol.RESTORED)), equalTo(0));
    }

    @Test
    @Title("Письмо из бэкапа не должны восстанавливаться, если они перемещены")
    public void shouldDoNothingWhenMessagesJustMoved() throws Exception {
        Mops.complexMove(authClient, folderList.defaultFID(), new MidsSource(mid)).post(shouldBe(okSync()));

        restoreAndWait(ApiRestore.MethodParam.FULL_HIERARCHY);

        FolderList folders = new FolderList(authClient);
        assertThat(folders.count(userFid), equalTo(1));
        assertThat(folders.count(folderList.defaultFID()), equalTo(1));
        assertThat(folders.count(folders.fidBySymbol(Symbol.RESTORED)), equalTo(0));
    }

    @Test
    @Title("Письмо из backup_box при восстановлении со стратегией restored_folder должно попадать в restored")
    public void shouldRestoreMessageToRestoreFolder() throws Exception {
        Mops.purge(authClient, new MidsSource(mid)).post(shouldBe(okSync()));

        restoreAndWait(ApiRestore.MethodParam.RESTORED_FOLDER);

        FolderList folders = new FolderList(authClient);
        assertThat(folders.count(userFid), equalTo(1));
        assertThat(folders.count(folderList.defaultFID()), equalTo(0));
        assertThat(folders.count(folders.fidBySymbol(Symbol.RESTORED)), equalTo(1));
    }

    @Test
    @Title("Письмо из backup_box при восстановлении со стратегией full_hierarchy должно попадать в исходную папку, когда она существует")
    public void shouldRestoreMessageToOriginalFolder() throws Exception {
        Mops.purge(authClient, new MidsSource(mid)).post(shouldBe(okSync()));

        restoreAndWait(ApiRestore.MethodParam.FULL_HIERARCHY);

        FolderList folders = new FolderList(authClient);
        assertThat(folders.count(userFid), equalTo(2));
        assertThat(folders.count(folderList.defaultFID()), equalTo(0));
        assertThat(folders.count(folders.fidBySymbol(Symbol.RESTORED)), equalTo(0));
    }

    @Test
    @Title("Письмо из backup_box при восстановлении со стратегией full_hierarchy должно попадать в папку восстановленную, если исходная удалена")
    public void shouldRestoreMessageToRecreatedFolderWhenOriginalWasDeleted() throws Exception {
        Mops.purge(authClient, new MidsSource(mid, fixedMid)).post(shouldBe(okSync()));
        Mops.deleteFolder(authClient, userFid).post(shouldBe(okSync()));

        restoreAndWait(ApiRestore.MethodParam.FULL_HIERARCHY);

        FolderList folders = new FolderList(authClient);
        assertFalse(folders.nonsystemFids().contains(userFid));

        assertThat(folders.count(folderList.defaultFID()), equalTo(0));
        assertThat(folders.count(folders.fidByName(userFolderName)), equalTo(2));
    }


    @Test
    @Title("Письмо из hidden_box при восстановлении со стратегией restored_folder должно попадать в restored")
    public void shouldRestoreHiddenMessageToRestoreFolder() throws Exception {
        Mops.complexMove(authClient, hiddenTrashFid, new MidsSource(mid)).post(shouldBe(okSync()));

        restoreAndWait(ApiRestore.MethodParam.RESTORED_FOLDER);

        FolderList folders = new FolderList(authClient);
        assertThat(folders.count(userFid), equalTo(1));
        assertThat(folders.count(folderList.defaultFID()), equalTo(0));
        assertThat(folders.count(folders.fidBySymbol(Symbol.RESTORED)), equalTo(1));
    }


    @Test
    @Title("Письмо из hidden_box при восстановлении со стратегией full_hierarchy должно попадать в исходную папку, когда она существует")
    public void shouldRestoreHiddenMessageToOriginalFolder() throws Exception {
        Mops.complexMove(authClient, hiddenTrashFid, new MidsSource(mid)).post(shouldBe(okSync()));

        restoreAndWait(ApiRestore.MethodParam.FULL_HIERARCHY);

        FolderList folders = new FolderList(authClient);
        assertThat(folders.count(userFid), equalTo(2));
        assertThat(folders.count(folderList.defaultFID()), equalTo(0));
        assertThat(folders.count(folders.fidBySymbol(Symbol.RESTORED)), equalTo(0));
    }

    @Test
    @Title("Письмо из hidden_box при восстановлении со стратегией full_hierarchy должно попадать в папку restored, если исходная удалена")
    public void shouldRestoreHiddenMessageToRecreatedFolderWhenOriginalWasDeleted() throws Exception {
        Mops.complexMove(authClient, hiddenTrashFid, new MidsSource(mid, fixedMid)).post(shouldBe(okSync()));
        Mops.deleteFolder(authClient, userFid).post(shouldBe(okSync()));

        restoreAndWait(ApiRestore.MethodParam.FULL_HIERARCHY);

        FolderList folders = new FolderList(authClient);
        assertFalse(folders.nonsystemFids().contains(userFid));

        assertThat(folders.count(folderList.defaultFID()), equalTo(0));
        assertThat(folders.count(folders.fidByName(userFolderName)), equalTo(2));
    }
}

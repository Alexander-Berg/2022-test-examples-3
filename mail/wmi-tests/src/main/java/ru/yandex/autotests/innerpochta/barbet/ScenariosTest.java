package ru.yandex.autotests.innerpochta.barbet;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.barbet.Create;
import ru.yandex.autotests.innerpochta.beans.barbet.Settings;
import ru.yandex.autotests.innerpochta.beans.barbet.Status;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.backup.restore.ApiRestore;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.barbet.BackupMatcher.activeBackupWithId;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.barbet.BackupMatcher.completeRestore;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;


@Aqua.Test
@Title("[Barbet] Сценарии бекапа")
@Credentials(loginGroup = "BarbetScenario1")
@Features(MyFeatures.BARBET)
@Stories(MyStories.BACKUP)
public class ScenariosTest extends BackupBaseTest {
    @Test
    @Title("Пустой статус")
    public void shouldCheckEmptyStatusIfThereIsNoBackupOrRestore() {
        Status st = status()
                .get(shouldBe(ok200()))
                .as(Status.class);

        assertThat(st.getPrimary(), nullValue());
        assertThat(st.getRestore(), nullValue());
    }

    @Test
    @Title("Обновляем настройки")
    public void shouldUpdateFidsInSettings() {
        Settings updated = updateSettings()
                .withFids(folderList.defaultFID())
                .withFids(folderList.sentFID())
                .withFids(folderList.outgoingFID())
                .post(shouldBe(ok200()))
                .as(Settings.class);

        assertThat(updated.getFids().toArray(), hasItemInArray(folderList.defaultFID()));
        assertThat(updated.getFids().toArray(), hasItemInArray(folderList.sentFID()));
        assertThat(updated.getFids().toArray(), hasItemInArray(folderList.outgoingFID()));
        assertThat(updated.getFids().toArray(), arrayWithSize(3));
    }

    @Test
    @Title("Обновляем настройки")
    public void shouldFailOnEmptyFid() {
        updateSettings()
                .withFids("")
                .post(shouldBe(badRequest400()));

    }

    @Test
    @Title("Создание и восстановление бекапа")
    public void shouldCreateAndRestoreBackup() throws Exception {
        final long MESSAGE_COUNT = 5;

        updateSettings()
                .withFids(folderList.defaultFID())
                .post(shouldBe(ok200()))
                .as(Settings.class);


        List<String> mids = sendWith(authClient).count((int)MESSAGE_COUNT).send().waitDeliver().getMids();


        Long id = create()
                .post(shouldBe(ok200()))
                .as(Create.class)
                .getBackupId();
        assertThat(authClient, withWaitFor(activeBackupWithId(id)));


        Long backupMessageCount = status().get(shouldBe(ok200())).as(Status.class).getPrimary().getMessageCount();
        assertThat("Иное число писем в бекапе", backupMessageCount, equalTo(MESSAGE_COUNT));


        Mops.purge(authClient, new MidsSource(mids)).withUid(getUid()).post(shouldBe(ok200()));
        assertThat(new FolderList(authClient).count(folderList.defaultFID()), equalTo(0));


        restore()
                .withMethod(ApiRestore.MethodParam.FULL_HIERARCHY)
                .post(shouldBe(ok200()));
        assertThat(authClient, withWaitFor(completeRestore(id)));
        assertThat(new FolderList(authClient).count(folderList.defaultFID()), equalTo((int)MESSAGE_COUNT));
    }
}

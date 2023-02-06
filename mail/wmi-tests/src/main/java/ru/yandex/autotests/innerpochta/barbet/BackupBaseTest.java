package ru.yandex.autotests.innerpochta.barbet;

import org.junit.Rule;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.backup.create.ApiCreate;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.backup.delete.ApiDelete;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.backup.restore.ApiRestore;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.backup.settings.ApiSettings;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.backup.status.ApiStatus;
import ru.yandex.autotests.innerpochta.wmi.core.barbet.backup.updatesettings.ApiUpdateSettings;
import ru.yandex.autotests.innerpochta.wmi.core.rules.barbet.CleanBackupsRule;

import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiBarbet;

public class BackupBaseTest extends BarbetBaseTest {

    @Rule
    public CleanBackupsRule cleanBackup = new CleanBackupsRule(authClient);

    ApiStatus status() {
        return apiBarbet(getUserTicket()).status()
                .withUid(getUid());
    }

    ApiCreate create() {
        return apiBarbet(getUserTicket()).create()
                .withUid(getUid());
    }

    ApiRestore restore() {
        return apiBarbet(getUserTicket()).restore()
                .withUid(getUid());
    }

    ApiSettings settings() {
        return apiBarbet(getUserTicket()).settings()
                .withUid(getUid());
    }

    ApiUpdateSettings updateSettings() {
        return apiBarbet(getUserTicket()).updateSettings()
                .withUid(getUid());
    }

    ApiDelete delete() {
        return apiBarbet(getUserTicket()).delete()
                .withUid(getUid());
    }
}

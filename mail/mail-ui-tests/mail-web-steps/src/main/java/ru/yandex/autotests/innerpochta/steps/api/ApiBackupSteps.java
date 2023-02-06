package ru.yandex.autotests.innerpochta.steps.api;

import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Objects;

import static ru.yandex.autotests.innerpochta.api.backup.CreateBackupHandler.doCreateBackupHandler;
import static ru.yandex.autotests.innerpochta.api.backup.DeleteBackupHandler.doDeleteBackupHandler;
import static ru.yandex.autotests.innerpochta.api.backup.PurgeHiddenTrashHandler.doPurgeHiddenTrashHandler;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.HIDDEN_TRASH_ENABLED;

/**
 * @author eremin-n-s
 */
public class ApiBackupSteps {

    private AllureStepStorage user;
    private RestAssuredAuthRule auth;

    public ApiBackupSteps(AllureStepStorage user) {
        this.user = user;
    }

    public ApiBackupSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Создаем резервную копию")
    public ApiBackupSteps createBackup() {
        doCreateBackupHandler().withAuth(auth).callCreateBackupHandler();
        return this;
    }

    @Step("Удаляем резервную копию")
    public ApiBackupSteps deleteBackup() {
        doDeleteBackupHandler().withAuth(auth).callDeleteBackupHandler();
        return this;
    }

    @Step("Очищаем hidden trash")
    public ApiBackupSteps purgeHiddenTrash() {
        if (Objects.equals(user.apiSettingsSteps().getUserSettings(HIDDEN_TRASH_ENABLED), "true")) {
            doPurgeHiddenTrashHandler().withAuth(auth).callPurgeHiddenTrashHandler();
        }
        return this;
    }
}

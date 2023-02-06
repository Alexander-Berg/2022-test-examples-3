package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * Created by mabelpines on 13.05.15.
 */
public class RemoveAllCustomFoldersRule extends ExternalResource{
    private AllureStepStorage user;

    private RemoveAllCustomFoldersRule(AllureStepStorage user) {
        this.user = user;
    }

    public static RemoveAllCustomFoldersRule removeAllCustomFolders(AllureStepStorage user) {
        return new RemoveAllCustomFoldersRule(user);
    }

    @Override
    protected void before() throws Throwable {
        deleteFolders();
    }

    public RemoveAllCustomFoldersRule deleteFolders() {
        user.apiFoldersSteps().deleteAllCustomFolders();
        return this;
    }
}

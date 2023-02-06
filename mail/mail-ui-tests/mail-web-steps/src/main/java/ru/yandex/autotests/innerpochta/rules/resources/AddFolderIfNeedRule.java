package ru.yandex.autotests.innerpochta.rules.resources;

import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.util.Utils;

/**
 * @author cosmopanda on 23.07.2016.
 */
public class AddFolderIfNeedRule extends ExternalResource {

    private AllureStepStorage user;
    private Producer<AllureStepStorage> producer;

    private AddFolderIfNeedRule(Producer<AllureStepStorage> producer) {
        this.producer = producer;
    }

    public static AddFolderIfNeedRule addFolderIfNeed(Producer<AllureStepStorage> producer) {
        return new AddFolderIfNeedRule(producer);
    }

    public Folder getFirstFolder() {
        return user.apiFoldersSteps().getAllUserFolders().get(0);
    }

    @Override
    protected void before() throws Throwable {
        user = producer.call();
        if (user.apiFoldersSteps().getAllUserFolders().isEmpty())
            user.apiFoldersSteps().createNewFolder(Utils.getRandomName());
    }
}

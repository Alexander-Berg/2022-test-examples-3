package ru.yandex.autotests.innerpochta.rules.resources;

import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;

public class ClearFoldersRule extends ExternalResource {

    private String[] folderNames;
    private Producer<AllureStepStorage> producer;

    private ClearFoldersRule(Producer<AllureStepStorage> producer, String... folderNames) {
        this.producer = producer;
        this.folderNames = folderNames;
    }

    public static ClearFoldersRule clearFoldersRule(Producer<AllureStepStorage> producer, String... folderNames) {
        return new ClearFoldersRule(producer, folderNames);
    }

    @Override
    protected void before() throws Throwable {
        AllureStepStorage user = producer.call();
        for (String folderName : folderNames) {
            Folder folder  = user.apiFoldersSteps().getFolderBySymbol(folderName);
            user.apiFoldersSteps().purgeFolder(folder);
        }
    }
}

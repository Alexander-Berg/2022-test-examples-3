package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * @author pavponn
 */
public class RemoveOldMessagesRule extends ExternalResource {
    private AllureStepStorage user;
    private String folder;
    private String olderThan;

    private RemoveOldMessagesRule(AllureStepStorage user, String folder, String olderThan) {
        this.user = user;
        this.folder = folder;
        this.olderThan = olderThan;
    }

    public static RemoveOldMessagesRule removeOldMessagesRule(AllureStepStorage user, String folder, String olderThan) {
        return new RemoveOldMessagesRule(user, folder, olderThan);
    }

    @Override
    protected void before() throws Throwable {
        user.apiFoldersSteps()
            .purgeInboxByDateAction(olderThan, olderThan, user.apiFoldersSteps().getFolderBySymbol(folder).getFid());
    }
}

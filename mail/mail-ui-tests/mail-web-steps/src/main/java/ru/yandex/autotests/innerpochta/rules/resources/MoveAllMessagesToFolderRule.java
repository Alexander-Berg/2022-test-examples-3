package ru.yandex.autotests.innerpochta.rules.resources;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * Created by mabelpines on 26.08.15.
 */
public class MoveAllMessagesToFolderRule extends ExternalResource {
    private AllureStepStorage user;
    private String fromFolder;
    private String toFolder;

    private MoveAllMessagesToFolderRule(AllureStepStorage user, String fromFolder, String toFolder) {
        this.user = user;
        this.fromFolder = fromFolder;
        this.toFolder = toFolder;
    }

    public static MoveAllMessagesToFolderRule moveAllMessagesToFolderRule(AllureStepStorage user,
                                                                          String fromFolder,
                                                                          String toFolder) {
        return new MoveAllMessagesToFolderRule(user, fromFolder, toFolder);
    }

    @Override
    protected void before() throws Throwable {
        user.apiMessagesSteps().moveAllMessagesFromFolderToFolder(fromFolder, toFolder);
    }
}

package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.filesystem.exception.NoPermissionException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserTestBase;

/**
 * @author eoshch
 */
public class FilesystemCreateFolderDoubleUserTest extends DjfsDoubleUserTestBase {
    private static DjfsResourcePath PATH_1 = DjfsResourcePath.cons(UID_1, "/disk/folder");

    @Test(expected = NoPermissionException.class)
    public void wrongActorUidThrowsException() {
        filesystem.createFolder(PRINCIPAL_2, PATH_1);
    }
}

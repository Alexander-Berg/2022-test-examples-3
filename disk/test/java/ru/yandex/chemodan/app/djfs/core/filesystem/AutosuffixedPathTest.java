package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.legacy.exception.mkdir.LegacyMkdirResourceExistsException;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.test.Assert;

public class AutosuffixedPathTest extends DjfsSingleUserTestBase {

    @Test
    public void folderAutosuffixedWithoutSpecialExtensionTreatment() throws Exception {
        final DjfsResourcePath pathOriginal     = DjfsResourcePath.cons(UID, "/disk/folder.txt");
        final DjfsResourcePath pathAutosuffixed = DjfsResourcePath.cons(UID, "/disk/folder.txt (1)");
        final Assert.Block mkdir =
            () -> legacyFilesystemActions.mkdir(PRINCIPAL.toString(), pathOriginal.getPath(), Option.empty());

        mkdir.execute();
        try {
            mkdir.execute();
        } catch (LegacyMkdirResourceExistsException e) {
            Assert.equals(Option.of(pathAutosuffixed), e.availablePath);
            return;
        }
        Assert.fail("Expected LegacyMkdirResourceExistsException");
    }

    @Test
    @Ignore
    public void fileAutosuffixedSkippingExtension() {
        // TODO
        // create file "file.tar.gz" -> OK
        // create file "file.tar.gz" again -> error
        // expect suggested "file (1).tar.gz" -- autonumbering only in base name
    }

}

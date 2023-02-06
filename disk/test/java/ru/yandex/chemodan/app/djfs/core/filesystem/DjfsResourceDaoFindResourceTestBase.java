package ru.yandex.chemodan.app.djfs.core.filesystem;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;

/**
 * @author yappo
 */
public abstract class DjfsResourceDaoFindResourceTestBase extends DjfsSingleUserTestBase {
    static final FolderDjfsResource ROOT = FolderDjfsResource.cons(UID, "/disk");
    static FolderDjfsResource D1 = FolderDjfsResource.cons(ROOT, "d1");
    static FolderDjfsResource D1_DUP = FolderDjfsResource.cons(ROOT, "d1_dup", x -> x.fileId(D1.getFileId()));
    static FileDjfsResource F1 = FileDjfsResource.random(ROOT, "f1");
    static FileDjfsResource F1_DUP = FileDjfsResource.random(ROOT, "f1_dup", x -> x.fileId(F1.getFileId()));

    static final FolderDjfsResource TRASH_ROOT = FolderDjfsResource.cons(UID, "/trash");
    static FolderDjfsResource D1_TRASH_DUP = FolderDjfsResource.cons(TRASH_ROOT, "d1_trash_dup",
            x -> x.fileId(D1.getFileId()));
    static FileDjfsResource F1_TRASH_DUP = FileDjfsResource.random(TRASH_ROOT, "f1_trash_dup",
            x -> x.fileId(F1.getFileId()));

    ListF<DjfsResourceArea> defaultAreasToBeSearched = Cf.list(DjfsResourceArea.DISK, DjfsResourceArea.PHOTOUNLIM);
}

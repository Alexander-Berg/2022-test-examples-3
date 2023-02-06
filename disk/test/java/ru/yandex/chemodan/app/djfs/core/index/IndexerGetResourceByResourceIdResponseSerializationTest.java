package ru.yandex.chemodan.app.djfs.core.index;


import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.MediaType;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;

public class IndexerGetResourceByResourceIdResponseSerializationTest {

    @Test
    public void fileResponseSerialization() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons("123", "/disk/folder"),
                x -> x.fileId(DjfsFileId.random())
                        .isVisible(true)
                        .version(Option.of(1234L))
                        .fileStid("STID")
                        .md5("MD5")
                        .size(0)
                        .modificationTime(Instant.now())
                        .creationTime(Instant.now())
                        .mimetype(Option.of("photo"))
                        .mediaType(Option.of(MediaType.IMAGE)));

        new IndexerFilePojo(DjfsUid.cons(1), file, UUID.randomUUID(), Option.empty());
    }

    @Test
    public void folderResponseSerialization() {
        FolderDjfsResource folder = FolderDjfsResource.cons(DjfsResourcePath.cons("123", "/disk/folder"),
                x -> x.fileId(DjfsFileId.random())
                        .isVisible(true)
                        .version(Option.of(1234L))
                        .modificationTime(Option.of(Instant.now()))
                        .creationTime(Option.of(Instant.now())));

        new IndexerFolderPojo(DjfsUid.cons(1), folder, Cf.list(UUID.randomUUID()), Option.empty());
    }
}

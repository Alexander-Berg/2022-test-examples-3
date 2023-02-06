package ru.yandex.chemodan.app.djfs.core.filesystem;

import com.mongodb.ReadPreference;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.image.Dimension;
import ru.yandex.misc.test.Assert;

public class FilesystemSaveExtractedDataTest extends DjfsSingleUserTestBase {
    @Test
    public void testSavingExtractedAesthetics() {
        DjfsFileId fileId = DjfsFileId.random();
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/file.txt");
        filesystem.createFile(PRINCIPAL, path, x -> x.fileId(fileId));

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId);
        double aesthetics = 0.4;

        filesystem.setAesthetics(PRINCIPAL, resourceId, aesthetics, false);
        FileDjfsResource file = (FileDjfsResource)filesystem.find(PRINCIPAL, path, Option.of(ReadPreference.primary())).get();

        Assert.some(file.getAesthetics());
        Assert.isTrue(Math.abs(aesthetics - file.getAesthetics().get()) < 0.01);
    }

    @Test
    public void testAestheticsFieldSaveOnCopy() {
        DjfsFileId fileId = DjfsFileId.random();
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/file.txt");
        filesystem.createFile(PRINCIPAL, path, x -> x.fileId(fileId));

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId);
        double aesthetics = 0.4;

        filesystem.setAesthetics(PRINCIPAL, resourceId, aesthetics, false);

        DjfsResourcePath newPath = DjfsResourcePath.cons(UID, "/disk/file-2.txt");
        filesystem.copySingleResource(PRINCIPAL, path, newPath);

        FileDjfsResource file = (FileDjfsResource)filesystem.find(PRINCIPAL, newPath, Option.of(ReadPreference.primary())).get();

        Assert.some(file.getAesthetics());
        Assert.isTrue(Math.abs(aesthetics - file.getAesthetics().get()) < 0.01);
    }

    @Test
    public void testSavingExtractedDimensions() {
        DjfsFileId fileId = DjfsFileId.random();
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/file.txt");
        filesystem.createFile(PRINCIPAL, path, x -> x.fileId(fileId));

        DjfsResourceId resourceId = DjfsResourceId.cons(UID, fileId);

        Dimension dimensions = new Dimension(800, 600);
        int angle = 180;

        filesystem.setDimensionsWithAngle(PRINCIPAL, resourceId, dimensions, angle, false);
        FileDjfsResource file = (FileDjfsResource)filesystem.find(PRINCIPAL, path, Option.of(ReadPreference.primary())).get();

        Assert.some(file.getWidth());
        Assert.some(file.getHeight());
        Assert.some(file.getAngle());

        Assert.equals(file.getWidth().get(), dimensions.getWidth());
        Assert.equals(file.getHeight().get(), dimensions.getHeight());
        Assert.equals(file.getAngle().get(), angle);
    }
}

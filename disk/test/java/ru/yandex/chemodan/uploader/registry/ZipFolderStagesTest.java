package ru.yandex.chemodan.uploader.registry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.uploader.config.UploaderCoreContextConfigurationForTests;
import ru.yandex.chemodan.uploader.registry.record.status.FileToZipInfo;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.commune.archive.ArchiveEntry;
import ru.yandex.commune.archive.ArchiveListing;
import ru.yandex.commune.archive.ArchiveManager;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.InputStreamSourceUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        ChemodanInitContextConfiguration.class,
        UploaderCoreContextConfigurationForTests.class})
public class ZipFolderStagesTest extends AbstractTest {

    @Autowired
    private ZipFolderStages zipFolderStages;
    @Autowired
    private ArchiveManager archiveManager;

    @Test
    public void streamZeroStidFromMulca() throws Exception {
        FileToZipInfo info = new FileToZipInfo(
                "", MulcaId.fromSerializedString("2739.yadisk:uploader.97099719030826319143612335895"),
                DataSize.fromBytes(0), "document", "text/plain");

        streamFileAndCheckArchive(info);
    }

    @Test
    public void streamZeroSizeFileFromMulca() throws Exception {
        FileToZipInfo info = new FileToZipInfo(
                "", MulcaId.fromSerializedString("2739.yadisk:uploader.123123"),
                DataSize.fromBytes(0), "document", "text/plain");

        streamFileAndCheckArchive(info);
    }

    private void streamFileAndCheckArchive(FileToZipInfo info) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipArchiveOutputStream zipArchiveOutputStream = zipOutputStream(out);
        zipFolderStages.streamFromMulca(zipArchiveOutputStream, "empty.txt", Option.of(info), zipFolderStages::openStream);
        zipArchiveOutputStream.close();

        checkZipFile(out.toByteArray());
    }

    public void checkZipFile(byte[] out) {
        ArchiveListing listing = archiveManager.listArchive(InputStreamSourceUtils.bytes(out));
        Assert.equals(Cf.list("empty.txt"), listing.getEntries().map(ArchiveEntry.getReadablePathF()));
        Assert.equals(0L, listing.getEntries().find(ArchiveEntry.pathEqualsToF("empty.txt")).get().getSize().get());
    }

    public ZipArchiveOutputStream zipOutputStream(OutputStream out) {
        ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(out);
        zipOutputStream.setMethod(ZipArchiveOutputStream.DEFLATED);
        zipOutputStream.setLevel(1);
        return zipOutputStream;
    }
}

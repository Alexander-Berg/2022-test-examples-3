package ru.yandex.chemodan.app.docviewer.utils.cache;

import java.io.File;

import org.joda.time.Duration;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.storages.FileLink;
import ru.yandex.chemodan.app.docviewer.storages.fsstorage.FileSystemFileLink;
import ru.yandex.chemodan.app.docviewer.utils.FileUtils;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.IoFunction;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.misc.time.Stopwatch;

/**
 * @author akirakozov
 * @author vlsergey
 */
public class TemporaryFileCacheTest extends TestBase {

    private static final Logger logger = LoggerFactory.getLogger(TemporaryFileCacheTest.class);

    @Test
    public void test() {
        TemporaryFilesCleaner temporaryFilesCleaner = new TemporaryFilesCleaner(
                Duration.standardSeconds(1), Duration.standardSeconds(1), DataSize.MEGABYTE, DataSize.MEGABYTE, 1, "so");
        TemporaryFileCache cache = new TemporaryFileCache(temporaryFilesCleaner, DataSize.fromBytes(10));
        temporaryFilesCleaner.start();

        File2 tempFolder = FileUtils.createTempDirectory("TemporaryFileCacheTest", ".tmp");
        int numOfFiles = 100;
        fill(cache, tempFolder, numOfFiles);

        ThreadUtils.sleep(Duration.standardSeconds(3));

        int existed = 0;
        for (int i = 0; i < numOfFiles; i++) {
            File tempFile = new File(tempFolder.getAbsolutePath() + "/" + i + ".txt");
            if (tempFile.exists())
                existed++;
        }

        Assert.assertEquals(0, existed);
    }

    private static void fill(TemporaryFileCache temporaryFileCache, File2 tempFolder, int numOfFiles) {
        for (int i = 0; i < numOfFiles; i++) {
            File2 tempFile = temporaryFileCache.getOrCreateTemporaryFile(new FileSystemFileLink(
                    tempFolder.getAbsolutePath() + "/" + i + ".txt"),
                    (IoFunction<FileLink, File2>) a -> {
                        final File2 newFile = new File2(((FileSystemFileLink) a)
                                .getAbsolutePath());
                        newFile.getFile().createNewFile();
                        newFile.write("a");
                        return newFile;
                    });

            Assert.isTrue(tempFile.exists());
        }
    }

    @Ignore
    @Test
    public void testPerformance() {
        TemporaryFilesCleaner temporaryFilesCleaner = new TemporaryFilesCleaner(
                Duration.standardMinutes(1), Duration.standardMinutes(1), DataSize.MEGABYTE, DataSize.MEGABYTE, 1, "so");
        TemporaryFileCache cache = new TemporaryFileCache(temporaryFilesCleaner, DataSize.fromMegaBytes(100));

        Stopwatch watch = Stopwatch.createAndStart();
        File2 tempFolder = FileUtils.createTempDirectory("TemporaryFileCacheTest", ".tmp");
        fill(cache, tempFolder, 20000);
        watch.stopAndLog("Duration: ", logger);
    }
}

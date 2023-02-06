package ru.yandex.market.mbo.cms.tms.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import io.qameta.allure.Issue;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author sergtru
 * @since 11.03.2018
 */
@Issue("MBO-14822")
public class Md5FilesRegistryTest {
    /**
     * Test that we write hashes and valid files.
     */
    @Test
    public void testFiles() throws IOException {
        final List<FileWithHash> files = Arrays.asList(
                new FileWithHash("test file content 1", "f5721993dbbc8c3d9b7152d99d1957df"),
                new FileWithHash("test file content 2", "c3a5cc5f801a51cf46912c925600e0e4")
        );
        File tempDirectory = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();

        Md5FilesRegistry registry = new Md5FilesRegistry(tempDirectory);
        writeThenRead(files, registry);
        registry.complete();

        File[] exports = tempDirectory.listFiles();
        Assert.assertEquals("Should be 2 files + md5sums", files.size() + 1, exports.length);
        checkSumms(files, registry.getSumsFile());

        //remove only on success
        FileUtils.deleteDirectory(tempDirectory);
    }

    private void checkSumms(List<FileWithHash> files, File sumsFile) throws IOException {
        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     new GZIPInputStream(
                                             new FileInputStream(sumsFile)
                                     )
                             )
                     )
        ) {
            Map<String, String> fileToMd5 = new HashMap<>(reader.lines()
                    .map(s -> s.split("  "))
                    .collect(Collectors.toMap(arr -> arr[1], arr -> arr[0])));

            Assert.assertEquals(files.size(), fileToMd5.size());

            for (FileWithHash file : files) {
                String hash = fileToMd5.remove(file.fileName);
                Assert.assertEquals(file.hash, hash);
            }
            Assert.assertTrue(fileToMd5.isEmpty());
        }
    }

    private void writeThenRead(List<FileWithHash> files, Md5FilesRegistry registry) throws IOException {
        for (FileWithHash file : files) {
            try (BufferedWriter writer = registry.createBufferedWriter(file.fileName, this)) {
                writer.write(file.content);
            }
        }
        for (FileWithHash file : files) {
            try (BufferedReader reader = registry.createBufferedReader(file.fileName)) {
                Assert.assertEquals(file.content, reader.readLine());
            }
        }
    }

    private static class FileWithHash {
        private static AtomicInteger nextNum = new AtomicInteger();
        final String fileName;
        final String content;
        final String hash;

        FileWithHash(String content, String hash) {
            this.fileName = "test-" + nextNum.incrementAndGet();
            this.content = content;
            this.hash = hash;
        }
    }
}

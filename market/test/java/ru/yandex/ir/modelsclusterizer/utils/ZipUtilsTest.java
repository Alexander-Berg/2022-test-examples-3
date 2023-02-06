package ru.yandex.ir.modelsclusterizer.utils;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.Assert.*;

public class ZipUtilsTest {
    @Test
    public void testPipeline() throws Exception {
        Path zipTestDir = null;
        try {
            zipTestDir = Files.createTempDirectory("zip-test");
            Path zipFile = zipTestDir.resolve("zipTestFile.zip");
            Path data = zipTestDir.resolve("data");
            Path dataUnzipped = zipTestDir.resolve("data_unzipped");
            createStructure(data);
            ZipUtils.zipData(data, zipFile, false);
            ZipUtils.unzipData(dataUnzipped, zipFile);
            checkContainsAtLeastSource(data, dataUnzipped);
            checkContainsAtLeastSource(dataUnzipped, data);
        } finally {
            try {
                if (zipTestDir != null) {
                    FileUtils.deleteDirectory(zipTestDir.toFile());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void checkContainsAtLeastSource(Path sourcePath, Path targetPath) throws IOException {
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDirPath = targetPath.resolve(sourcePath.relativize(dir).toString());
                assertTrue(Files.exists(targetDirPath));
                assertTrue(Files.isDirectory(targetDirPath));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFilePath = targetPath.resolve(sourcePath.relativize(file).toString());
                assertTrue(Files.exists(targetFilePath));
                assertTrue(!Files.isDirectory(targetFilePath));
                byte[] sourceBytes = Files.readAllBytes(file);
                byte[] targetBytes = Files.readAllBytes(targetFilePath);
                assertArrayEquals(sourceBytes, targetBytes);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void createStructure(Path data) throws IOException {
        Files.createDirectories(data);
        Path subdir = data.resolve("subdir");
        Path subsubdir1 = subdir.resolve("subsubdir1");
        Path subsubdir2 = subdir.resolve("subsubdir2");
        Files.createDirectories(subsubdir1);
        Files.createDirectories(subsubdir2);
        writeToFile(data.resolve("test1.txt"), "one1");
        writeToFile(subdir.resolve("test2.txt"), "two");
        writeToFile(subsubdir2.resolve("test3.txt"), "three333");
        assertTrue(Files.exists(subsubdir2.resolve("test3.txt")));
        long fileSize = Files.size(subsubdir2.resolve("test3.txt"));
        assertEquals(fileSize, 8);
    }

    private void writeToFile(Path file, String text) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            bw.write(text);
        }
    }
}
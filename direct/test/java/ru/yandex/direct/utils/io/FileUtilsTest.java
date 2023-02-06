package ru.yandex.direct.utils.io;

import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class FileUtilsTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void atomicWriteWorks() {
        Path path = folder.getRoot().toPath().resolve("file");

        FileUtils.atomicWrite("data", path);

        assertEquals(FileUtils.slurp(path), "data");
    }
}

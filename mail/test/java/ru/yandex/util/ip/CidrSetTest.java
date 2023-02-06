package ru.yandex.util.ip;

import java.net.InetAddress;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;
import ru.yandex.util.filesystem.CloseableDeleter;

public class CidrSetTest extends TestBase {
    public CidrSetTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        try (CloseableDeleter deleter =
                new CloseableDeleter(Files.createTempFile("temp", ".txt")))
        {
            Files.writeString(deleter.path(), "::1");
            IpChecker checker =
                new IpSetChecker<>(deleter.path().toFile(), CidrSet.INSTANCE);
            Assert.assertTrue(checker.test(InetAddress.getByName("::1")));
        }
    }
}


package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

/**
 * @author aostrikov
 */
@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
@RunWith(MockitoJUnitRunner.class)
public class FileTimestampCheckerTest {

    private FileTimestampChecker fileChecker;

    @Before
    public void setUp() {
        fileChecker = new FileTimestampChecker();
    }

    @Test
    public void shouldRunCallbackOnFileChange() throws Exception {
        Path path = Paths.get(getClass().getClassLoader().getResource("test.txt").toURI());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean updated = new AtomicBoolean(false);

        fileChecker.onFileChange(path, () -> {
            updated.set(true);
            latch.countDown();
        });

        latch.await();

        assertTrue("Flag was not updated", updated.get());
    }
}

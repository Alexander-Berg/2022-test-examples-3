package ru.yandex.market.ir.tms.logs.saasoffers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.misc.thread.ThreadUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:magicnumber")
public class StandaloneIndexerControllerTest {
    private static final int WAIT_STEP_MS = 10;
    private static final String DIR_PREFIX = "standalone-indexer-controller-test";
    private StandaloneIndexerController indexerController;
    private Path indexerScript;
    private Path tempDirectory;

    @Before
    public void unpackScript() throws IOException {
        tempDirectory = Files.createTempDirectory(DIR_PREFIX);
        // Development:
        Files.list(tempDirectory.getParent()).forEach(path -> {
            if (path.toString().contains("standalone-indexer-controller")
                && !Objects.equals(path.toString(), tempDirectory.toString())) {
                FileUtils.deleteQuietly(path.toFile());
            }
        });


        Path controllerScript = Files.createFile(tempDirectory.resolve("test-controller.sh"),
            PosixFilePermissions.asFileAttribute(EnumSet.allOf(PosixFilePermission.class)));

        Files.write(controllerScript, IOUtils.toByteArray(getClass().getResource("/standalone-indexer-controller.sh")));

        indexerScript = Files.createFile(tempDirectory.resolve("test-indexer.sh"),
            PosixFilePermissions.asFileAttribute(EnumSet.allOf(PosixFilePermission.class)));

        indexerController = new StandaloneIndexerController(
            controllerScript.toAbsolutePath().toString(),
            () -> indexerScript.toAbsolutePath().toString(), "arnold", "", "", "", "", "");

        indexerController.setEnvironment("LOCK_NAME", tempDirectory.resolve("lock").toString());
        indexerController.setEnvironment("PID_NAME", tempDirectory.resolve("pid").toString());
        indexerController.setEnvironment("LOG_NAME", tempDirectory.resolve("log").toString());
    }

    @After
    public void cleanupTemp() throws IOException {
        if (Files.exists(tempDirectory)) {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    @Test
    public void testCheckProcessReturnsFalseIfNoFile() {
        assertFalse(indexerController.checkRunning());
    }

    @Test
    public void testCheckProcessReturnsFalseIfNotRunning() {
        indexerController.buildIndex("some", "thing", 123, null);
        assertFalse(indexerController.checkRunning());
    }

    @Test
    public void testCheckProcessReturnsTrueIfRunning() throws IOException {
        Files.write(indexerScript, ("" +
            "echo > " + tempDirectory.resolve("start-marker") + "; " +
            "sleep 3; " +
            "echo > " + tempDirectory.resolve("marker")).getBytes());

        runInTread(() ->
            indexerController.buildIndex("some", "thing", 123, null));

        waitFor(() -> Files.exists(tempDirectory.resolve("start-marker")));

        assertTrue(indexerController.checkRunning());
    }

    @Test
    public void testAwait() throws IOException {
        Files.write(indexerScript, ("" +
            "echo > " + tempDirectory.resolve("start-marker") + "; " +
            "sleep 3; " +
            "echo > " + tempDirectory.resolve("done-marker")).getBytes());

        runInTread(() ->
            indexerController.buildIndex("some", "thing", 123, null));

        waitFor(() -> Files.exists(tempDirectory.resolve("start-marker")));

        assertTrue(indexerController.checkRunning());
        indexerController.await();
        assertTrue(Files.exists(tempDirectory.resolve("done-marker")));
        assertFalse(indexerController.checkRunning());
    }

    @Test
    public void testLock() throws IOException {
        Path marker = tempDirectory.resolve("marker");
        // Creates file by ENV, then removes it - this way we can observe start and end of it's work
        Files.write(indexerScript, ("" +
            "echo ! >> " + marker + "$NUM; " +
            "sleep 3;" +
            "rm " + marker + "$NUM;").getBytes());

        runInTread(() -> {
            indexerController.setEnvironment("NUM", "1");
            indexerController.buildIndex("some", "thing", 123, null);
        });

        // Actual start of first script
        waitFor(() -> Files.exists(tempDirectory.resolve("marker1")));

        runInTread(() -> {
            indexerController.setEnvironment("NUM", "2");
            indexerController.buildIndex("some", "thing", 123, null);
        });

        // Won't start due to previous (timeout = error)
        waitFor("There should be 'another instance' text in log", 2_000, () ->
            Files.readAllLines(tempDirectory.resolve("log")).stream().anyMatch(s -> s.contains("another instance")));

        // But it must appear there.. eventually, will throw Timeout if it doesn't
        waitFor("First process finished", 6_000,
            () -> !Files.exists(tempDirectory.resolve("marker1")));

        // Wait a bit, truncate log & retry
        ThreadUtils.sleep(500);
        Files.write(tempDirectory.resolve("log"), new byte[0]);

        runInTread(() -> {
            indexerController.setEnvironment("NUM", "2");
            indexerController.buildIndex("some", "thing", 123, null);
        });

        waitFor("Second did start", 1_000, () ->
            Files.exists(tempDirectory.resolve("marker2")));
    }

    @Test
    public void testTimeout() throws IOException {
        Path marker = tempDirectory.resolve("marker");
        // Creates file by ENV, then removes it - this way we can observe start and end of it's work
        Files.write(indexerScript, ("" +
            "echo ! >> " + marker + "$NUM; " +
            "sleep 3;" +
            "rm " + marker + "$NUM;").getBytes());

        indexerController.setEnvironment("TIMEOUT", "1");

        runInTread(() -> {
            indexerController.setEnvironment("NUM", "1");
            indexerController.buildIndex("some", "thing", 123, null);
        });

        // Actual start of first script
        waitFor(() -> Files.exists(tempDirectory.resolve("marker1")));
        ThreadUtils.sleep(1_500);

        runInTread(() -> {
            indexerController.setEnvironment("NUM", "2");
            indexerController.buildIndex("some", "thing", 123, null);
        });

        waitFor("Second instance must start", 1_000,
            () -> Files.exists(tempDirectory.resolve("marker2")));

        assertTrue("Must have killed previous instance",
            Files.readAllLines(tempDirectory.resolve("log")).stream().anyMatch(s -> s.contains("killing")));

        ThreadUtils.sleep(2_000); // Give it a bit more time

        assertTrue("marker1 is still there, not cleaned... no one to clean it",
            Files.exists(tempDirectory.resolve("marker1")));
    }

    @Test
    public void testFailingProcess() throws IOException {
        Files.write(indexerScript, "exit 42".getBytes());

        int returnCode = indexerController.buildIndex("some", "thing", 123, null);

        assertEquals(42, returnCode);
    }

    @Test
    public void testOkProcess() throws IOException {
        Files.write(indexerScript, "echo Fine!".getBytes());

        int returnCode = indexerController.buildIndex("some", "thing", 123, null);

        assertEquals(0, returnCode);
    }

    private void runInTread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.start();
    }

    private void waitFor(Callable<Boolean> test) {
        waitFor(null, 1_000, test);
    }

    private void waitFor(String assertion, int millis, Callable<Boolean> test) {
        try {
            while (!test.call()) {
                ThreadUtils.sleep(10);
                millis -= WAIT_STEP_MS;

                if (millis < 0) {
                    throw new AssertionError(assertion == null ? "Timeout" : "Assertion failed: " + assertion);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

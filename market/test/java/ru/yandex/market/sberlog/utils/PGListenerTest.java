package ru.yandex.market.sberlog.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Kirillov <a href="mailto:isonami@yandex-team.ru"></a>
 * @date 21.04.20
 */
@Ignore
public class PGListenerTest {
    private File dataDir;
    private int iterationTime = 3;
    private String notifyChannel = "listenerTest";
    private String notifyTest = "notifyTest";

    private File getTestDataDir() throws IOException {
        if (dataDir == null) {
            dataDir = Files.createTempDirectory("sberlogPGTest").toFile();
        }
        return dataDir;
    }

    @After
    public void clear() throws IOException {
        FileUtils.deleteDirectory(getTestDataDir());
    }

    @Test
    public void testReconnect() throws IOException, InterruptedException {

        EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setCleanDataDirectory(false)
                .setDataDirectory(getTestDataDir())
                .start();

        int port = pg.getPort();

        AtomicBoolean disconnectedCallback = new AtomicBoolean(false);

        PGListener listener = new PGListener(pg.getPostgresDatabase(),
                notifyChannel,
                iterationTime,
                (ignore) -> {},
                () -> disconnectedCallback.set(true));

        listener.start();

        TimeUnit.SECONDS.sleep(iterationTime);

        assertFalse("Listener is connected", listener.isClosed());

        pg.close();

        // Wait for two iterations
        TimeUnit.SECONDS.sleep((iterationTime * 2));

        assertTrue("Listener is disconnected", listener.isClosed());
        assertTrue("Got disconnect callback", disconnectedCallback.get());

        pg = EmbeddedPostgres.builder()
                .setCleanDataDirectory(false)
                .setDataDirectory(getTestDataDir())
                .setPort(port)
                .start();

        TimeUnit.SECONDS.sleep(iterationTime);

        assertFalse("Listener is connected again", listener.isClosed());

        listener.interrupt();
        pg.close();

    }

    @Test
    public void testNotify() throws Exception {
        try (EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setCleanDataDirectory(false)
                .setDataDirectory(getTestDataDir())
                .start()) {

            AtomicReference<String> notifyString = new AtomicReference<>();

            PGListener listener = new PGListener(pg.getPostgresDatabase(),
                    notifyChannel,
                    iterationTime,
                    notifyString::set);

            listener.start();

            TimeUnit.SECONDS.sleep(iterationTime);

            Connection c = pg.getPostgresDatabase().getConnection();
            Statement s = c.createStatement();
            s.execute("NOTIFY \"" + notifyChannel + "\", '" + notifyTest + "'");

            TimeUnit.SECONDS.sleep(iterationTime);

            assertEquals("Notify result expected", notifyTest, notifyString.get());

        }
    }

    @Test
    public void testDbLateStart() throws IOException, InterruptedException {
        EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setCleanDataDirectory(false)
                .setDataDirectory(getTestDataDir())
                .start();

        int port = pg.getPort();

        pg.close();

        PGListener listener = new PGListener(pg.getPostgresDatabase(),
                notifyChannel,
                iterationTime,
                (ignore) -> {});

        listener.start();

        TimeUnit.SECONDS.sleep(iterationTime);

        pg = EmbeddedPostgres.builder()
                .setCleanDataDirectory(false)
                .setDataDirectory(getTestDataDir())
                .setPort(port)
                .start();

        TimeUnit.SECONDS.sleep(iterationTime);

        assertFalse("Listener is connected", listener.isClosed());

        TimeUnit.SECONDS.sleep(iterationTime);

        listener.interrupt();
        pg.close();
    }
}

package ru.yandex.market.vmid.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;


public class PGaaSZonkyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int DEFAULT_STARTUP_WAIT_SECONDS = 30;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        File tempDir = createAndSetTempDir();

        try {
            EmbeddedPostgres epg = EmbeddedPostgres.builder()
                    .setDataDirectory(Files.createTempDirectory(tempDir.toPath(), "epg"))
                    .setCleanDataDirectory(true)
                    .setPGStartupWait(Duration.ofSeconds(DEFAULT_STARTUP_WAIT_SECONDS))
                    .start();
            int port = epg.getPort();
            String url = String.format("jdbc:postgresql://localhost:%d/postgres?prepareThreshold=0", port);
            System.setProperty("db.url", url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File createAndSetTempDir() {
        // https://wiki.yandex-team.ru/yatool/test/#ramdrive
        String ramDisk = System.getenv("YA_TEST_RAM_DRIVE_PATH");
        File workDir = new File(
                ramDisk != null ? ramDisk : System.getProperty("java.io.tmpdir"),
                "embedded-pg-" + System.getProperty("user.name"));
        if (!workDir.exists() && !workDir.mkdir()) {
            throw new RuntimeException("Can't create workDir " + workDir);
        }
        System.setProperty("ot.epg.working-dir", workDir.toPath().toAbsolutePath().toString());
        return workDir;
    }

}

package ru.yandex.direct.test.mysql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.mysql.MySQLDockerContainer;
import ru.yandex.direct.mysql.MySQLServer;
import ru.yandex.direct.process.Docker;
import ru.yandex.direct.process.DockerException;
import ru.yandex.direct.process.DockerRunner;
import ru.yandex.direct.utils.Checked;
import ru.yandex.direct.utils.NamedThreadFactory;

import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

public class DirectMysqlDbDocker {

    private static final Logger logger = LoggerFactory.getLogger(DirectMysqlDbDocker.class);

    // сейчас initialized всегда используется под локом, тем не менее объявлена volatile "на всякий случай"
    private static volatile boolean initialized = false;
    private static volatile RuntimeException initializationException = null;
    private final TestMysqlConfig config;
    private static final String MYSQL_DOCKER_CONTAINER_NAME = "direct_mysql";

    public DirectMysqlDbDocker(TestMysqlConfig config) {
        this.config = config;
    }

    public MySQLDockerContainer start(Docker docker) throws InterruptedException {
        // Контейнер мог остаться от предыдущего запуска
        // Если так случилось, его надо остановить
        docker.stopAndRemoveContainerByName(MYSQL_DOCKER_CONTAINER_NAME);
        assertFirstStart();
        try {
            var dockerContainer = new MySQLDockerContainer(
                    patchRunner(MySQLDockerContainer.createRunner(docker, readImageTag()), MOSCOW_TIMEZONE, MYSQL_DOCKER_CONTAINER_NAME)
            );

            Runtime.getRuntime().addShutdownHook(new NamedThreadFactory("StopMysqlDockerContainerThread").newThread(() -> {
                try {
                    logger.info("Closing Mysql docker container {}", dockerContainer.getContainerId());
                    dockerContainer.close();
                } catch (Exception e) {
                    logger.error("Exception while stopping Mysql docker container {}", dockerContainer.getContainerId());
                }
            }));
            return dockerContainer;
        } catch (DockerException ex) {
            initializationException = ex;
            if (ex.getMessage().toLowerCase().contains("unable to find image")) {
                // Это сообщение об ошибке может оказаться посередине стены текста из стектрейсов suppressed-ошибок.
                // Оно должно бросаться в глаза при беглом осмотре.
                throw new RuntimeException("\n\n"
                        + "**************************************\n"
                        + "* Possibly you're not authorized in registry.yandex.net. Check\n"
                        + "* https://wiki.yandex-team" +
                        ".ru/direct/development/java/setup/#avtorizacijavjandeksovomdocker-registry\n"
                        + "**************************************\n\n"
                        + ex.getMessage(),
                        ex);
            } else {
                throw ex;
            }
        } catch (RuntimeException ex) {
            initializationException = ex;
            throw ex;
        }
    }

    private static synchronized void assertFirstStart() {
        // При работе с MySQL из Sandbox ресурса мы пока не умеем поднимать второй инстанс БД,
        // В докерном варианте повторяем такое же поведение для консистентности
        // Дополнительно - если контекст не собирается, лучше упасть, чем запустить кучу контейнеров
        // а если первый запуск не удался - добавляем его ошибку как suppressed
        if (initialized) {
            IllegalStateException ex = new IllegalStateException("Can't start second MySQL DB from Sandbox resource");
            if (initializationException != null) {
                ex.addSuppressed(initializationException);
            }
            throw ex;
        }
        initialized = true;
    }

    private String readImageTag() {
        if (DirectMysqlDb.class.getResource(config.getDockerImageFilename()) == null) {
            throw new IllegalStateException(config.getDockerImageFilename() + " not found.");
        }

        try (
                InputStream stream = DirectMysqlDb.class.getResourceAsStream(config.getDockerImageFilename());
                InputStreamReader reader = new InputStreamReader(stream);
                BufferedReader bufferedReader = new BufferedReader(reader)
        ) {
            String imageTag = bufferedReader.readLine();
            if (bufferedReader.readLine() != null) {
                throw new IllegalStateException(config.getDockerImageFilename() + " contains more than one line");
            }
            return imageTag;
        } catch (IOException exc) {
            throw new Checked.CheckedException(exc);
        }
    }

    public DockerRunner createRunner(Docker docker) {
        return patchRunner(MySQLDockerContainer.createRunner(docker), null, null);
    }

    public DockerRunner createRunner(Docker docker, String image) {
        return patchRunner(MySQLDockerContainer.createRunner(docker, image), null, null);
    }

    public DockerRunner createRunner(Docker docker, String image, String name) {
        return patchRunner(MySQLDockerContainer.createRunner(docker, image), null, name);
    }

    private DockerRunner patchRunner(DockerRunner runner, @Nullable String timezone) {
        return patchRunner(runner, timezone, null);
    }
    private DockerRunner patchRunner(DockerRunner runner, @Nullable String timezone, @Nullable String name) {
        ImmutableList.Builder<String> argsBuilder = ImmutableList.<String>builder()
                .add("--datadir=" + DirectMysqlDb.MYSQL_DATA_DIR)
                .addAll(MySQLServer.MYSQL_OPTIMIZATION_PARAMS)
                .addAll(MySQLServer.MYSQL_SERVER_PARAMS);
        if (timezone != null) {
            argsBuilder.add("--default-time-zone=" + timezone);
        }
        runner.withCmd(
                argsBuilder.build().toArray(new String[0])
        );
        if(name != null) {
            runner.withName(name);
            runner.withAutoRemove();
        }
        return runner;
    }

    public void createImage(
            Path arcadiaRoot, MySQLDockerContainer mysql, String imageTag
    ) throws InterruptedException {
        Path resourceFile = arcadiaRoot.resolve(config.getDockerImageArcadiaPath());
        mysql.commit(imageTag);
        try {
            Files.createDirectories(resourceFile.getParent());
            Files.write(resourceFile, imageTag.getBytes(StandardCharsets.UTF_8));
        } catch (IOException exc) {
            throw new Checked.CheckedException(exc);
        }
    }
}

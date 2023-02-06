package ru.yandex.direct.test.grut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.process.Docker;
import ru.yandex.direct.process.DockerContainer;
import ru.yandex.direct.process.DockerRunner;
import ru.yandex.direct.utils.Checked;
import ru.yandex.direct.utils.HostPort;
import ru.yandex.direct.utils.InterruptedRuntimeException;
import ru.yandex.direct.utils.NamedThreadFactory;
import ru.yandex.direct.utils.ThreadUtils;
import ru.yandex.grut.client.GrutClient;
import ru.yandex.grut.client.GrutGrpcClient;
import ru.yandex.grut.client.SingleHostServiceHolder;
import ru.yandex.grut.object_api.proto.ObjectApiServiceOuterClass;

public class DockerGrutClientProvider {
    private static final Logger logger = LoggerFactory.getLogger(DockerGrutClientProvider.class);

    private static final String IMAGE_RESOURCE_NAME = "/ru/yandex/direct/test/grut/grut_local_docker_image.txt";

    // Внешний порт отличается от 1310, чтобы не нужно было заказывать дырку до виртуалки
    private static final int OBJECT_API_PORT = 10310;
    private static final int OBJECT_API_PORT_IN_CONTAINER = 1310;
    private static final int YT_PORT = 80;

    // имя контейнера (чтобы не запускать более одного контейнера)
    private static final String GRUT_CONTAINER_NAME = "grut_local";

    public GrutClient getGrutClient() {
        Docker docker = new Docker();

        // Контейнер мог остаться от предыдущего запуска
        // Если так случилось, его надо остановить
        docker.stopAndRemoveContainerByName(GRUT_CONTAINER_NAME);

        DockerRunner dockerRunner = new DockerRunner(docker, readImageTag())
                .withPublishedPort(OBJECT_API_PORT, OBJECT_API_PORT_IN_CONTAINER)
                .withPublishedPort(YT_PORT, YT_PORT)  // чтобы можно было делать yt --proxy localhost:80 select-rows ...
                .withName(GRUT_CONTAINER_NAME)
                .withAutoRemove();

        DockerContainer dockerContainer;
        HostPort hostAndPort;
        try {
            logger.info("Starting GrUT container.. (first run may take a while)");
            dockerContainer = new DockerContainer(dockerRunner);
            hostAndPort = dockerContainer.getPublishedPort(OBJECT_API_PORT_IN_CONTAINER);
            logger.info("GrUT object_api endpoint: {}", hostAndPort);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedRuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new NamedThreadFactory("StopGrutDockerContainerThread").newThread(() -> {
            try {
                logger.info("Closing GrUT docker container {}", dockerContainer.getContainerId());
                dockerContainer.close();
            } catch (Exception e) {
                logger.error("Exception while stopping GrUT docker container {}", dockerContainer.getContainerId());
            }
        }));

        // Подождём, когда контейнер станет healthy
        // Контейнер становится healthy, когда там поднимается и становится доступным yt
        dockerContainer.waitUntilContainerBecomeHealthy();

        var serviceHolder = new SingleHostServiceHolder(
                hostAndPort.getHost(),
                hostAndPort.getPort(),
                null, 2, null, 10L
        );

        GrutClient grutClient = new GrutGrpcClient(serviceHolder, null, null);

        // После того, как контейнер стал healthy, нужно подождать, когда object_api это осознает и подключится к yt
        // Так как изнутри контейнера это сделать проблематично, ждём здесь
        waitForObjectApiToBeReady(grutClient);

        return grutClient;
    }

    private String readImageTag() {
        if (DockerGrutClientProvider.class.getResource(IMAGE_RESOURCE_NAME) == null) {
            throw new IllegalStateException(IMAGE_RESOURCE_NAME + " not found.");
        }
        try (
                InputStream stream = DockerGrutClientProvider.class.getResourceAsStream(IMAGE_RESOURCE_NAME);
                InputStreamReader reader = new InputStreamReader(stream);
                BufferedReader bufferedReader = new BufferedReader(reader)
        ) {
            String imageTag = bufferedReader.readLine();
            if (bufferedReader.readLine() != null) {
                throw new IllegalStateException(IMAGE_RESOURCE_NAME + " contains more than one line");
            }
            return imageTag;
        } catch (IOException exc) {
            throw new Checked.CheckedException(exc);
        }
    }

    private void waitForObjectApiToBeReady(GrutClient grutClient) {
        logger.info("Waiting for ObjectApi to be ready");
        ThreadUtils.execWithRetries(attempt -> {
            logger.info("Probing ObjectApi...");
            grutClient.generateTimestamp(ObjectApiServiceOuterClass.TReqGenerateTimestamp.newBuilder().build());
        }, 50, 500, 1.1, null);
        logger.info("Got successfull ObjectApi response");
    }
}

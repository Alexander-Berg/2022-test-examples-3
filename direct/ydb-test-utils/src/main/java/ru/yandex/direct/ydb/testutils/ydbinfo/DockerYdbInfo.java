package ru.yandex.direct.ydb.testutils.ydbinfo;

import com.yandex.ydb.core.grpc.GrpcTransport;
import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.process.Docker;
import ru.yandex.direct.process.DockerContainer;
import ru.yandex.direct.process.DockerRunner;
import ru.yandex.direct.utils.HostPort;
import ru.yandex.direct.utils.InterruptedRuntimeException;
import ru.yandex.direct.utils.NamedThreadFactory;
import ru.yandex.direct.ydb.YdbPath;

public class DockerYdbInfo implements YdbInfo {
    private static final Logger logger = LoggerFactory.getLogger(DockerYdbInfo.class);

    private TableClient tableClient;

    private static final YdbPath DB = YdbPath.of("local");
    private static final String IMAGE = "registry.yandex.net/yandex-docker-local-ydb:stable";

    // Внешний порт указал побольше, чтобы не нужно было заказывать дырку до виртуалки,
    // если docker настроен на запуск в QYP
    private static final int GRPC_PORT = 12135;
    private static final int GRPC_PORT_IN_CONTAINER = 2135;

    // имя контейнера (чтобы не запускать более одного контейнера)
    private static final String YDB_CONTAINER_NAME = "ydb_local";
    private static final String INMEMORY_ENV_NAME = "YDB_USE_IN_MEMORY_PDISKS";

    @Override
    public void init() {
        Docker docker = new Docker();

        // Контейнер мог остаться от предыдущего запуска
        // Если так случилось, его надо остановить
        docker.stopAndRemoveContainerByName(YDB_CONTAINER_NAME);

        DockerRunner dockerRunner = new DockerRunner(docker, IMAGE);
        String ydbInMemory = System.getProperty(INMEMORY_ENV_NAME);
        if ("aarch64".equals(System.getProperty("os.arch"))) {
            // https://st.yandex-team.ru/KIKIMR-13216#6206394b0dccb251fe4e2d14
            ydbInMemory = "true";
        }
        // если m1, но java x86 — можно вручную добавить переменную в параметры запуска юнит-тестов в IDEA
        if (ydbInMemory != null) {
            dockerRunner.withEnvironment(INMEMORY_ENV_NAME, ydbInMemory);
        }
        dockerRunner.withPublishedPort(GRPC_PORT, GRPC_PORT_IN_CONTAINER);
        // [podman-compatibility] Задаём явно healthcheck-команду, чтобы доопределился дефолтный таймаут
        //   Без этого при запуске через podman контейнер никогда не переходит в healthy
        //   TODO: унести этот костыль под условие if (isPodman()), чтобы оно не затрагивало обычный докер
        // Временно закомментировал, т.к. на старых stable-образах эта команда не работает
        //dockerRunner.withRunArgs("--health-cmd", "sh ./health_check");
        dockerRunner.withName(YDB_CONTAINER_NAME);
        dockerRunner.withAutoRemove();

        HostPort hostPort;
        DockerContainer dockerContainer;
        try {
            dockerContainer = new DockerContainer(dockerRunner);
            hostPort = dockerContainer.getPublishedPort(GRPC_PORT_IN_CONTAINER);
            logger.info("Ydb port {}", hostPort.getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedRuntimeException(e);
        }
        var rpcTransportBuilder = GrpcTransport.forHost(hostPort.getHost(), hostPort.getPort());
        dockerContainer.waitUntilContainerBecomeHealthy();
        tableClient = TableClient.newClient(GrpcTableRpc.useTransport(rpcTransportBuilder.build())).build();
        Runtime.getRuntime().addShutdownHook(new NamedThreadFactory("StopYdbDockerContainer").newThread(() -> {
            try {
                logger.info("Closing ydb docker container {}", dockerContainer.getContainerId());
                dockerContainer.close();
            } catch (Exception e) {
                logger.error("Exception while stopping ydb docker container {}", dockerContainer.getContainerId());
            }
        }));
    }

    @Override
    public TableClient getClient() {
        return tableClient;
    }

    @Override
    public YdbPath getDb() {
        return DB;
    }
}

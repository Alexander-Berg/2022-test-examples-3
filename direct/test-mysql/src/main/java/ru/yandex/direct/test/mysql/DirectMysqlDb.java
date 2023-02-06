package ru.yandex.direct.test.mysql;

import ru.yandex.direct.mysql.MySQLInstance;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.process.Docker;

public class DirectMysqlDb {
    // /var/lib/mysql определён как volume в Dockerfile, поэтому не сохраняет данные и мы вынуждены
    // использовтаь другую директорию
    public static final String MYSQL_DATA_DIR = "/var/lib/mysql_intern";

    private final TestMysqlConfig config;
    private final DirectMysqlDbDocker docker;
    private final DirectMysqlDbSandbox sandbox;

    enum Engine {
        DOCKER,
        SANDBOX
    }

    public DirectMysqlDb(TestMysqlConfig config) {
        this.config = config;
        docker = new DirectMysqlDbDocker(config);
        sandbox = new DirectMysqlDbSandbox(config);
    }

    public MySQLInstance start() throws InterruptedException {
        Engine engine = detectEngine();
        switch (engine) {
            case DOCKER:
                return docker.start(new Docker());
            case SANDBOX:
                return sandbox.start();
            default:
                throw new IllegalStateException("Unexpected engine: " + engine);
        }
    }

    public MySQLServerBuilder useSandboxMysqlServerIfPossible(MySQLServerBuilder builder) {
        Engine engine = detectEngine();
        switch (engine) {
            case DOCKER:
                return builder;
            case SANDBOX:
                return sandbox.useSandboxMysqlServer(builder);
            default:
                throw new IllegalStateException("Unexpected engine: " + engine);
        }
    }

    public Engine detectEngine() {
        String engineProperty = System.getProperty("test_mysql.engine");

        if (engineProperty != null) {
            return Engine.valueOf(engineProperty.toUpperCase());
        } else {
            if (ru.yandex.devtools.test.Paths.getSandboxResourcesRoot() != null
                    && "Linux".equals(System.getProperty("os.name"))
            ) {
                // Запуск из под ya make в linux
                return Engine.SANDBOX;
            } else {
                // Во всех остальных случаях (в т.ч. при запуске в IDEA) используем docker
                return Engine.DOCKER;
            }
        }
    }

    public DirectMysqlDbDocker docker() {
        return docker;
    }

    public DirectMysqlDbSandbox sandbox() {
        return sandbox;
    }
}

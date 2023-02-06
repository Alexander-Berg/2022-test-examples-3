package ru.yandex.direct.test.mysql;

import java.time.Duration;

import ru.yandex.direct.mysql.MySQLDockerContainer;
import ru.yandex.direct.mysql.MySQLInstance;
import ru.yandex.direct.utils.GracefulShutdownHook;

public class DirectMysqlRunner {
    private DirectMysqlRunner() {
    }

    @SuppressWarnings("squid:S106") // Это main консольной утилиты, так что можно System.err.println()
    public static void main(String[] args) throws InterruptedException {
        TestMysqlConfig config = TestMysqlConfig.directConfig();
        runMysqlServer(config);
    }

    public static void runMysqlServer(TestMysqlConfig config) throws InterruptedException {
        System.err.println("Starting mysql server...");
        DirectMysqlDb directMysqlDb = new DirectMysqlDb(config);
        try (
                MySQLInstance mysql = directMysqlDb.start();
                GracefulShutdownHook ignored = new GracefulShutdownHook(Duration.ofMinutes(1))
        ) {
            System.err.println("Waiting while mysql server is ready to accept connections...");

            mysql.awaitConnectivity(Duration.ofSeconds(30));
            System.err.println("Ready.\n");

            System.err.println(
                    "Connect with: mysql" +
                            " --host=" + mysql.getHost() +
                            " --port=" + mysql.getPort() +
                            " --user=" + mysql.getUsername() +
                            " --password=" + mysql.getPassword() +
                            "\n"
            );

            if (mysql instanceof MySQLDockerContainer) {
                MySQLDockerContainer dockerMysql = (MySQLDockerContainer) mysql;
                System.err.println("Docker is in use. Container ID: " + dockerMysql.getContainerId());
                System.err.println("Enter with: docker exec -ti " + dockerMysql.getContainerId() + " bash");
            }

            System.err.println("Press CTRL-C to stop mysql and exit");
            Thread.currentThread().join(); // IS-NOT-COMPLETABLE-FUTURE-JOIN
        }
    }
}

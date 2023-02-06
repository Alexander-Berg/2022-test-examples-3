package ru.yandex.market.pricelabs;

import java.util.Optional;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import ru.yandex.market.pricelabs.misc.Utils;

/**
 * Позволяет переопределить параметры подключения к YT, PostgreSQL, MySQL и YQL в зависимости от переменных окружения.
 * Используется для тестов - можно использовать локальный экземпляр YT вместо стандартного кластера для ускорения
 * работы тестов.
 *
 */
@Slf4j
@Order(1)
public class YaMakeTestListener implements TestExecutionListener {

    public static final String YT_PROXY = System.getenv("YT_PROXY");
    public static final String PG_LOCAL_HOST = System.getenv("PG_LOCAL_HOST");
    public static final String PG_LOCAL_PORT = System.getenv("PG_LOCAL_PORT");
    public static final String MYSQL_LOCAL_PORT = System.getenv("MYSQL_LOCAL_PORT");
    public static final boolean YQL_DISABLED = Boolean.getBoolean("YQL_DISABLED");
    public static final String YQL_HOST = System.getenv("YQL_HOST");
    public static final String YQL_PORT = System.getenv("YQL_PORT");

    private boolean configured;

    @Override
    public void beforeTestClass(TestContext testContext) {
        if (configured) {
            return; // ---
        }

        Optional.ofNullable(YT_PROXY).ifPresent(YaMakeTestListener::initSandboxEnvironment);
        Optional.ofNullable(PG_LOCAL_PORT).ifPresent(port ->
                YaMakeTestListener.initEmbeddedPostgres(PG_LOCAL_HOST, Integer.parseInt(port)));
        Optional.ofNullable(MYSQL_LOCAL_PORT).ifPresent(port ->
                YaMakeTestListener.initEmbeddedMySQL(Integer.parseInt(port)));

        if (YQL_DISABLED) {
            disableEmbeddedYql();
        } else {
            Optional.ofNullable(YQL_PORT).ifPresent(port ->
                    YaMakeTestListener.initEmbeddedYql(YQL_HOST, Integer.parseInt(port)));
        }

        configured = true;
    }

    private static void initSandboxEnvironment(String proxy) {
        log.info("Using LOCAL YT: {}", proxy);
        // Запускаемся из тестового окружения Sandbox/Linux
        System.setProperty("YT_PROXY", proxy);
        System.setProperty("YT_REPLICAS", "");
        System.setProperty("YT_USERNAME", "root");
        System.setProperty("YT_TOKEN", "");
    }

    private static void initEmbeddedPostgres(@Nullable String host, int port) {
        log.info("Using LOCAL PostgreSQL port: {}", port);
        System.setProperty("JDBC_URL", "jdbc:postgresql://" + Utils.nvl(host, "localhost") + ":" + port + "/postgres");
        System.setProperty("JDBC_USER", "postgres");
        System.setProperty("JDBC_PASSWORD", "postgres");
        System.setProperty("JDBC_SSL_MODE", "disable");

    }

    private static void initEmbeddedMySQL(int port) {
        log.info("Using LOCAL MySQL port: {}", port);
        System.setProperty("MYSQL_JDBC_URL", "jdbc:mysql://localhost:" + port + "/mysql_unit?allowMultiQueries=true&" +
                "serverTimezone=Europe/Moscow&useUnicode=true&characterEncoding=utf-8&" +
                "sessionVariables=group_concat_max_len=100000");
        System.setProperty("MYSQL_JDBC_USER", "root");
        System.setProperty("MYSQL_JDBC_PASSWORD", "mysql");
    }

    private static void disableEmbeddedYql() {
        log.info("Disable LOCAL YQL");
        System.setProperty("YQL_URL", "localhost:localport");
        System.setProperty("YQL_DB", "plato");
        System.setProperty("YQL_TOKEN", "none");
    }

    private static void initEmbeddedYql(@Nullable String host, int port) {
        log.info("Using LOCAL YQL");
        System.setProperty("YQL_URL", Utils.nvl(host, "localhost") + ":" + port);
        System.setProperty("YQL_DB", "plato");
        System.setProperty("YQL_TOKEN", "none");
    }

}

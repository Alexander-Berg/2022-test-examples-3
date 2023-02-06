package ru.yandex.market;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

public class StaticOracleContainer {

    private static final OracleContainer ORACLE_CONTAINER = createContainer();

    public static OracleContainer getInstance() {
        return ORACLE_CONTAINER;
    }

    private static OracleContainer createContainer() {
        tempDirtyHack();

        final OracleCredential oracleCredential = loadOracleCredential();
        final var container = new OracleContainer()
                .withUsername(oracleCredential.getLogin())
                .withPassword(oracleCredential.getPassword())
                .withEnv("TZ", "Europe/Moscow")
                .withReuse(true)
                .withExposedPorts(1521, 5500)
                .waitingFor(Wait.forHealthcheck())
                .withSharedMemorySize(2147483648L);

        container.start();
        return container;
    }

    private static OracleCredential loadOracleCredential() {
        try {
            final Properties properties = new Properties();
            final InputStream is = StaticOracleContainer.class.getClassLoader()
                    .getResourceAsStream("config.properties");
            properties.load(is);
            return new OracleCredential(
                    properties.getProperty("oracle.username"),
                    properties.getProperty("oracle.password")
            );
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Testcontainers ожидает, что в user.home будет файл .testcontainers.properties с настройками для данный среды.
     * Переопределяем свойство testcontainers.reuse.enable, которое говорит о том, что среда поддерживает переиспользование
     * контейнеров.
     * Нужно для переспользования поднятого контейнера при запуске LARGE тестов в sandbox'е.
     */
    private static void tempDirtyHack() {
        try {
            final Field environmentProperties = TestcontainersConfiguration.class.getDeclaredField("environmentProperties");
            environmentProperties.setAccessible(true);
            ((Properties) environmentProperties.get(TestcontainersConfiguration.getInstance()))
                    .setProperty("testcontainers.reuse.enable", "true");
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static class OracleCredential {
        private final String login;
        private final String password;

        public OracleCredential(String login, String password) {
            this.login = login;
            this.password = password;
        }

        public String getLogin() {
            return login;
        }

        public String getPassword() {
            return password;
        }
    }
}

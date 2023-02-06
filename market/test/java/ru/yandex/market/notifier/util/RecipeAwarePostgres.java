package ru.yandex.market.notifier.util;

public class RecipeAwarePostgres {

    public static final String DEFAULT_PORT_ENV_NAME = "PG_LOCAL_PORT";

    private final String portEnvName;

    public RecipeAwarePostgres() {
        this(DEFAULT_PORT_ENV_NAME);
    }

    public RecipeAwarePostgres(String portEnvName) {
        this.portEnvName = portEnvName;
    }

    public int getPort() {
        return Integer.parseInt(System.getenv(portEnvName));
    }

    public void close() {
        // noop
    }
}

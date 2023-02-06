package ru.yandex.market.checkout.util;

public class RecipeAwarePostgres {

    public static final String DEFAULT_PORT_ENV_NAME = "PG_LOCAL_PORT";

    private final String portEnvName;

    public RecipeAwarePostgres() {
        this(DEFAULT_PORT_ENV_NAME);
    }

    public RecipeAwarePostgres(String portEnvName) {
        this.portEnvName = portEnvName;
    }

    /**
     * mimics {@link EmbeddedPostgres#getPort()}
     *
     * @return port
     */
    public int getPort() {
        return Integer.parseInt(System.getenv(portEnvName));
    }

    /**
     * mimics {@link EmbeddedPostgres#close()}
     */
    public void close() {
        // noop
    }
}

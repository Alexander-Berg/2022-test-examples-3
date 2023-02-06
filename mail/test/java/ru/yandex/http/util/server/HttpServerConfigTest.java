package ru.yandex.http.util.server;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;

public class HttpServerConfigTest {
    private static final String SERVER = "MyServer";
    private static final String PREFIX = "server";
    private static final int PORT = 8080;
    private static final int WORKERS_MIN = 8;
    private static final int WORKERS_PERCENT = 100;
    private static final int CONNECTIONS = 80;
    private static final int TIMEOUT = 5000;
    private static final String WORKERS_MIN_STRING = "\nserver.workers.min = ";
    private static final String WORKERS_PERCENT_STRING =
        "\nserver.workers.percent = ";
    private static final String CONNECTIONS_STRING = "\nserver.connections = ";

    private static final String CONFIG =
        "server.port = " + PORT
        + WORKERS_MIN_STRING + WORKERS_MIN
        + WORKERS_PERCENT_STRING + 0
        + CONNECTIONS_STRING + CONNECTIONS
        + "\nserver.timeout = " + TIMEOUT;

    @Test
    public void testParams() throws ConfigException, IOException {
        IniConfig properties = new IniConfig(new StringReader(CONFIG));
        HttpServerConfig config =
            new HttpServerConfigBuilder(properties.section(PREFIX))
                .name(SERVER)
                .build();
        Assert.assertEquals(SERVER, config.name());
        Assert.assertEquals(PORT, config.port());
        Assert.assertEquals(WORKERS_MIN, config.workers());
        Assert.assertEquals(CONNECTIONS, config.connections());
        Assert.assertEquals(TIMEOUT, config.timeout());
    }

    @Test
    public void testMinWorkers() throws ConfigException, IOException {
        IniConfig properties = new IniConfig(
            new StringReader(
                CONFIG.replace(
                    WORKERS_MIN_STRING + WORKERS_MIN,
                    WORKERS_MIN_STRING + 1)
                        .replace(
                            WORKERS_PERCENT_STRING + 0,
                            WORKERS_PERCENT_STRING + WORKERS_PERCENT)));
        HttpServerConfig config =
            new HttpServerConfigBuilder(properties.section(PREFIX))
                .name(SERVER)
                .build();
        Assert.assertEquals(SERVER, config.name());
        Assert.assertEquals(PORT, config.port());
        Assert.assertEquals(Runtime.getRuntime().availableProcessors(),
            config.workers());
        Assert.assertEquals(CONNECTIONS, config.connections());
        Assert.assertEquals(TIMEOUT, config.timeout());
    }

    @Test(expected = ConfigException.class)
    public void checkConnectionsPresence()
        throws ConfigException, IOException
    {
        IniConfig properties = new IniConfig(
            new StringReader(
                CONFIG.replace(CONNECTIONS_STRING + CONNECTIONS, "")));
        new HttpServerConfigBuilder(properties.section(PREFIX)).build();
    }

    @Test(expected = ConfigException.class)
    public void checkConnectionsNumberFormat()
        throws ConfigException, IOException
    {
        IniConfig properties = new IniConfig(
            new StringReader(
                CONFIG.replace(
                    CONNECTIONS_STRING + CONNECTIONS,
                    CONNECTIONS_STRING + "hello, world")));
        new HttpServerConfigBuilder(properties.section(PREFIX)).build();
    }

    @Test(expected = ConfigException.class)
    public void checkWorkersMinCount() throws ConfigException, IOException {
        IniConfig properties = new IniConfig(
            new StringReader(
                CONFIG.replace(
                    WORKERS_MIN_STRING + WORKERS_MIN,
                    WORKERS_MIN_STRING + 0)));
        new HttpServerConfigBuilder(properties.section(PREFIX)).build();
    }

    @Test(expected = ConfigException.class)
    public void checkWorkersPercent() throws ConfigException, IOException {
        IniConfig properties = new IniConfig(
            new StringReader(
                CONFIG.replace(
                    WORKERS_PERCENT_STRING + 0,
                    WORKERS_PERCENT_STRING + -1)));
        new HttpServerConfigBuilder(properties.section(PREFIX)).build();
    }
}


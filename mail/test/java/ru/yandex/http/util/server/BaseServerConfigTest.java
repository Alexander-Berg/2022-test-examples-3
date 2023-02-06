package ru.yandex.http.util.server;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;

public class BaseServerConfigTest {
    private static final int PORT = 8080;
    private static final int WORKERS = 8;
    private static final int CONNECTIONS = 80;
    private static final int TIMEOUT = 5000;
    private static final String SERVER_NAME = "MyServer";

    private static String getConfig() {
        return "server.port = " + PORT
            + "\nserver.workers.min = " + WORKERS
            + "\nserver.workers.percent = " + 0
            + "\nserver.connections = " + CONNECTIONS
            + "\nserver.timeout = " + TIMEOUT;
    }

    @Test
    public void test() throws ConfigException, IOException {
        IniConfig properties = new IniConfig(new StringReader(getConfig()));
        BaseServerConfig config = new BaseServerConfigBuilder(properties)
            .name(SERVER_NAME).build();
        Assert.assertEquals(SERVER_NAME, config.name());
        Assert.assertEquals(PORT, config.port());
        Assert.assertEquals(WORKERS, config.workers());
        Assert.assertEquals(CONNECTIONS, config.connections());
        Assert.assertEquals(TIMEOUT, config.timeout());
    }
}

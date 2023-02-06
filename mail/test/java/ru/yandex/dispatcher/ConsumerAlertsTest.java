package ru.yandex.dispatcher;

import java.io.File;
import java.nio.file.Files;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.dispatcher.consumer.ConsumerServer;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.filesystem.CloseableDeleter;

public class ConsumerAlertsTest extends TestBase {
    @Test
    public void testClipConsumerAlerts() throws Exception {
        try (CloseableDeleter tmpdir =
                new CloseableDeleter(
                    Files.createTempDirectory(testName.getMethodName())))
        {
            System.setProperty("INSTANCE_TAG_CTYPE", "prod");
            System.setProperty("CONSUMER_PORT", "0");
            System.setProperty("HOSTNAME", "localhost");
            System.setProperty(
                "CONFIG_DIRS",
                Paths.getSourcePath(
                    "mail/tools/nanny_helpers/nanny_service_base/files"));
            IniConfig ini =
                new IniConfig(
                    new File(
                        Paths.getSourcePath(
                            "mail/so/daemons/clip/clip_service/files"
                            + "/consumer.conf")));
            ini.section("searchmap").put(
                "file",
                resource("searchmap.txt").toString());
            ini.sections().remove("log");
            ini.sections().remove("accesslog");
            try (ConsumerServer consumer = new ConsumerServer(ini)) {
                ini.checkUnusedKeys();
                consumer.start();
                try (CloseableHttpClient client =
                        Configs.createDefaultClient();
                    CloseableHttpResponse response =
                        client.execute(
                            new HttpGet(
                                consumer.host() + "/generate-alerts-config")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                        new StringChecker(
                            loadResourceAsString("clip-alerts-config.ini")),
                        CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }

    @Test
    public void testOhioBackendConsumerAlerts() throws Exception {
        try (CloseableDeleter tmpdir =
                new CloseableDeleter(
                    Files.createTempDirectory(testName.getMethodName())))
        {
            System.setProperty("INSTANCE_TAG_CTYPE", "prod");
            System.setProperty("CONSUMER_PORT", "0");
            System.setProperty("HOSTNAME", "localhost");
            System.setProperty(
                "CONFIG_DIRS",
                Paths.getSourcePath(
                    "mail/tools/nanny_helpers/nanny_service_base/files"));
            System.setProperty("SEARCHMAP_PATH", "searchmap.txt");
            IniConfig ini =
                new IniConfig(
                    new File(
                        Paths.getSourcePath(
                            "mail/ohio/ohio_backend_service/files"
                            + "/consumer.conf")));
            ini.section("searchmap").put(
                "file",
                resource("searchmap.txt").toString());
            ini.sections().remove("log");
            ini.sections().remove("accesslog");
            try (ConsumerServer consumer = new ConsumerServer(ini)) {
                ini.checkUnusedKeys();
                consumer.start();
                try (CloseableHttpClient client =
                        Configs.createDefaultClient();
                    CloseableHttpResponse response =
                        client.execute(
                            new HttpGet(
                                consumer.host() + "/generate-alerts-config")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                        new StringChecker(
                            loadResourceAsString("ohio-alerts-config.ini")),
                        CharsetUtils.toString(response.getEntity()));
                }
            }
        }
    }
}


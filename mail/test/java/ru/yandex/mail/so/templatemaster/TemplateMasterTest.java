package ru.yandex.mail.so.templatemaster;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.http.HttpException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.GzipCompressingEntity;
import ru.yandex.json.parser.JsonException;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.Checker;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class TemplateMasterTest extends TestBase {
    private static final long OPERATION_TIMEOUT = 500;

    public TemplateMasterTest() {
        super(false, 0L);
    }

    @Test
    public void testTemplateFormation() throws Exception {
        try (TemplateMasterCluster cluster
                 = new TemplateMasterCluster(this);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();

            final String indexHandle = "/index?service=template_master"
                + "&shard=0&domain=calendar.yandex.ru&ctime=*";

            final String saveHandle = "/save_template?service=template_master"
                + "&shard=0&drop_domain=calendar.yandex.ru";

            cluster.producer().add(
                indexHandle,
                new StaticHttpResource(
                    new ProxyHandler(cluster.templateMaster().port())));
            cluster.producer().add(
                saveHandle,
                new ProxyHandler(cluster.templateMaster().port()));

            for (int sampleId = 0; sampleId <= 5; ++sampleId) {
                HttpPost post = new HttpPost(
                    cluster.TMUri() + "/route?domain=calendar.yandex.ru"
                        + "&attributes=%7B%22sn%22%3A%20" + sampleId + "%7D");
                post.setEntity(
                    new InputStreamEntity(
                        this.getClass().getResourceAsStream(
                            "/calendar/sample" + sampleId + ".eml")));
                HttpAssert.assertJsonResponse(
                    client,
                    post,
                    "{\"status\": \"NotFound\"}");
                waitForEquals(
                    () -> cluster.producer().accessCount(indexHandle),
                    sampleId + 1);
            }

            // wait for template to be made
            waitAndCheckLucene(
                "/search?text=url:*&get=*",
                new String(
                    this.getClass()
                        .getResourceAsStream("/expected_database.json")
                        .readAllBytes(),
                    StandardCharsets.UTF_8),
                cluster.searchBackend());

            Assert.assertEquals(6, cluster.producer().accessCount(indexHandle));
            Assert.assertEquals(1, cluster.producer().accessCount(saveHandle));

            HttpPost post = new HttpPost(
                cluster.TMUri()
                + "/route?domain=calendar.yandex.ru&attributes=%7b%22queue-id%22:%22oeFuNlcWQW-wlfCfjeJ%22,%22uids%22:%5b%22578481627%22%5d,%22from%22:%22info@calendar.yandex.ru%22%7d");
            post.setEntity(
                new GzipCompressingEntity(
                    new InputStreamEntity(
                        this.getClass().getResourceAsStream(
                            "/calendar/sample6.eml"),
                        ContentType.APPLICATION_JSON),
                    256));
            HttpAssert.assertJsonResponse(
                client,
                post,
                "{\n"
                    + "    \"delta\": [\n"
                    + "        [\"Оформить заказы сотрудников\"],\n"
                    + "        [\"» состоится завтра, 19-го января\"],\n"
                    + "        [\"<a href=\\\"https://calendar.yandex.ru"
                    + "?\\\" style=\\\"color:#9a9a9a\\\">\"]\n"
                    + "    ],\n"
                    + "    \"attributes\": \"<any value>\",\n"
                    + "    \"stable_sign\": 8091393070027658492,\n"
                    + "    \"status\": \"FoundInDb\"\n"
                    + "}");
        }
    }

    @Test
    public void testDryRun() throws Exception {
        try (TemplateMasterCluster cluster
                 = new TemplateMasterCluster(this);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            HttpPost post = new HttpPost(
                cluster.TMUri() + "/route?domain=calendar.yandex.ru&dry-run"
                    + "&attributes=%7B%7D");
            post.setEntity(
                new InputStreamEntity(
                    this.getClass().getResourceAsStream(
                        "/calendar/sample0.eml")));
            HttpAssert.assertJsonResponse(
                client,
                post,
                "{\"status\": \"NotFound\"}");
        }
    }

    private static <T> void waitForEquals(
        Supplier<T> actual,
        T expected)
        throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + OPERATION_TIMEOUT;
        T current;
        do {
            current = actual.get();
            if (Objects.equals(current, expected)) {
                return;
            }
            Thread.sleep(OPERATION_TIMEOUT >> 4);
        } while (System.currentTimeMillis() < deadline);
        Assert.assertEquals(current, expected);
    }

    private void waitAndCheckLucene(
        String request,
        String expectedResponse,
        TestSearchBackend lucene)
        throws
        JsonException,
        IOException,
        HttpException,
        InterruptedException
    {
        waitAndCheckLucene(
            request,
            expectedResponse,
            lucene,
            OPERATION_TIMEOUT);
    }

    private void waitAndCheckLucene(
        String request,
        String expectedResponse,
        TestSearchBackend lucene,
        long timeout)
        throws
        JsonException,
        IOException,
        HttpException,
        InterruptedException
    {
        long deadline = System.currentTimeMillis() + timeout;
        Checker checker = new JsonChecker(expectedResponse);
        while (true) {
            try {
                lucene.checkSearch(request, checker);
                break;
            } catch (AssertionError e) {
                if (System.currentTimeMillis() > deadline) {
                    throw e;
                }
                Thread.sleep(timeout >> 4);
            }
        }
    }
}

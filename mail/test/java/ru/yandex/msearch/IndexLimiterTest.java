package ru.yandex.msearch;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.util.YandexHttpStatus;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class IndexLimiterTest extends TestBase {
    private static final int MAX_REQUESTS = 6;
    //LifoWaitBlockingQueue capacity is a nextPowerOfTwo of INDEX_THREADS
    //so INDEX_THREADS must be a powerOfTwo for test to complete
    private static final int INDEX_THREADS = 4;
    private static final int DELAY = 5000;
    private static final String CONFIG_SUFFIX =
        "indexer.limit_index_requests = "
        + MAX_REQUESTS + "\n";


    public static Config config(final File root, final String suffix, final int shards)
        throws Exception
    {
        File indexDir = new File(root, "index");
        return new Config(new IniConfig(new StringReader(
            "http.port = 0\n"
            + "http.timeout = 10000\n"
            + "http.connections = 1000\n"
            + "http.workers.min = 20\n"
            + "search.port = 0\n"
            + "search.timeout = 10000\n"
            + "search.connections = 1000\n"
            + "search.workers.min = 20\n"
            + "indexer.port = 0\n"
            + "indexer.timeout = 10000\n"
            + "indexer.connections = 1000\n"
            + "indexer.workers.min = 20\n"
            + "index_threads = 2\n"
            + "shards = " + shards
            + "\nuse_journal = 1\n"
            + "index_path = " + indexDir.getCanonicalPath() + '\n'
            + "\nfull_log.level.min = all\n"
            + "yandex_codec.terms_writer_block_size = 8192\n"
            + "yandex_codec.group_field = __prefix\n"
            + "yandex_codec.fields_writer_buffer_size = 6144\n"
            + "field.keyword.tokenizer = keyword\n"
            + "field.keyword.store = true\n"
            + "field.keyword.prefixed = true\n"
            + "field.keyword.analyze = true\n"
            + "field.property.index = false\n"
            + "field.property.store = true\n"
            + "field.attribute.tokenizer = whitespace\n"
            + "field.attribute.filters = lowercase\n"
            + "field.attribute.prefixed = false\n"
            + "field.attribute.attribute = true\n"
            + "field.attribute.analyze = true\n"
            + "field.attribute.store = true\n"
            + "field.text.tokenizer = letter\n"
            + "field.text.filters = lowercase|yo|lemmer\n"
            + "field.text.prefixed = true\n"
            + "field.text.analyze = true\n"
            + "field.boolean.tokenizer = boolean\n"
            + "field.boolean.prefixed = true\n"
            + "field.boolean.analyze = true\n"
            + "field.boolean.attribute = true\n"
            + "field.boolean.store = true\n"
            + suffix)));
    }

    private static class DelayTask implements Callable<Void> {
        private final HttpGet delay;
        private final int port;
        private final CloseableHttpClient client;

        public DelayTask(final CloseableHttpClient client, int port) {
            this.client = client;
            this.port = port;
            delay = new HttpGet("http://localhost:"
                + port + "/delay?delay=" + DELAY);
        }

        @Override
        public Void call() throws IOException {
            HttpResponse response =
                client.execute(delay);
            Assert.assertEquals(
                "Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
            return null;
        }
    }

    @Test
    public void testIndexLimiterLimited() throws Exception {
        System.err.println("testIndexLimiter started");
        File root = Files.createTempDirectory("testIndexLimiter").toFile();
        ThreadPoolExecutor executor = null;
        try (Daemon daemon =
                new Daemon(config(root, CONFIG_SUFFIX, 1));
            CloseableHttpClient client = HttpClients.custom()
                .setMaxConnPerRoute(20)
                .setMaxConnTotal(30)
                .setRetryHandler(
                    new DefaultHttpRequestRetryHandler(0, false))
                .build())
        {

            executor =
                new ThreadPoolExecutor(
                    MAX_REQUESTS,
                    MAX_REQUESTS,
                    1,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(MAX_REQUESTS));

            final HttpGet delay = new HttpGet("http://localhost:"
                + daemon.jsonServerPort() + "/delay?delay=" + DELAY);
            final ArrayList<Future<Void>> delayedTasks =
                new ArrayList<Future<Void>>(MAX_REQUESTS);
            for (int i = 0; i < MAX_REQUESTS; i++) {
                delayedTasks.add(executor.submit(
                    new DelayTask(client, daemon.jsonServerPort())));
            }
            Thread.sleep(DELAY / 2);
            HttpResponse response = client.execute(delay);
            Assert.assertEquals("Expected 529, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                YandexHttpStatus.SC_REQUESTS_LIMIT_REACHED,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            for (Future<Void> f : delayedTasks) {
                f.get();
            }
        } finally {
            System.err.println("testIndexLimiter ended");
            SearchBackendTestBase.removeDirectory(root);
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void testIndexLimiterUnlimited() throws Exception {
        System.err.println("testIndexLimiter started");
        File root = Files.createTempDirectory("testIndexLimiter").toFile();
        ThreadPoolExecutor executor = null;
        try (Daemon daemon =
                new Daemon(config(root, "", 1));
            CloseableHttpClient client = HttpClients.custom()
                .setMaxConnPerRoute(20)
                .setMaxConnTotal(30)
                .setRetryHandler(
                    new DefaultHttpRequestRetryHandler(0, false))
                .build())
        {

            executor =
                new ThreadPoolExecutor(
                    MAX_REQUESTS,
                    MAX_REQUESTS,
                    1,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(MAX_REQUESTS));

            final HttpGet delay = new HttpGet("http://localhost:"
                + daemon.jsonServerPort() + "/delay?delay=" + DELAY);
            final ArrayList<Future<Void>> delayedTasks =
                new ArrayList<Future<Void>>(MAX_REQUESTS);
            for (int i = 0; i < MAX_REQUESTS; i++) {
                delayedTasks.add(executor.submit(
                    new DelayTask(client, daemon.jsonServerPort())));
            }
            Thread.sleep(DELAY/2);
            HttpResponse response = client.execute(delay);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                YandexHttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            for (Future<Void> f : delayedTasks) {
                f.get();
            }
        } finally {
            System.err.println("testIndexLimiter ended");
            SearchBackendTestBase.removeDirectory(root);
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

}


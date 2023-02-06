package ru.yandex.msearch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class JournalSizeTest extends TestBase {
    private static final String CONFIG_SUFFIX = "max_part_journal_size_mb = 2\n"
        + "[field.mid]\n"
        + "store=true\n"
        + "[field.body_text]\n"
        + "tokenizer = letter\n"
        + "prefixed = true\n"
        + "attribute = false\n"
        + "store = true\n"
        + "analyze = true\n";

    private long getJournalSize(final File root) throws Exception {
        long size = 0;
        for (File file
            : new File(root, "index/0/journal").listFiles())
        {
            size += file.length();
        }
        return size;
    }

    private long getJournalCount(final File root) throws Exception {
        int journalCount = 0;
        for (File file
            : new File(root, "index/0/journal").listFiles())
        {
            journalCount++;
        }
        return journalCount;
    }

    @Test
    public void testMaxPartJournalSize() throws Exception {
        System.err.println("testMaxPartJournalSize started");
        File root = Files.createTempDirectory("testMaxPartJournalSize").toFile();
        boolean success = false;
        try (Daemon daemon =
                new Daemon(SearchBackendTestBase.config(root, CONFIG_SUFFIX, 1));
            CloseableHttpClient client = HttpClients.createDefault())
        {

            StringBuilder ThreeMbBody = new StringBuilder(1024 * 1024 * 3);
            while (ThreeMbBody.length() < 1024 * 1024 * 3) {
                ThreeMbBody.append("abcdef ");
            }

            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                + "\"mid\":\"100500\","
                + "\"body_text\":\"" + ThreeMbBody.toString() + "\"}]}",
                StandardCharsets.UTF_8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&get=mid&text=mid:*"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker(
                    "{\"hitsCount\":1,\"hitsArray\":[{"
                    + "\"mid\":\"100500\"}]}"),
                text);

            //TODO: Make proper autoflush wait
            for (int i = 0; i < 20; i++) {
                Thread.sleep(1000);
                if (getJournalSize(root) == 0) {
                    break;
                }
            }
            Assert.assertEquals(1, getJournalCount(root));
            Assert.assertEquals(0, getJournalSize(root));

            StringBuilder OneMbBody = new StringBuilder(1024 * 1024 * 1);
            while (OneMbBody.length() < 1024 * 1024 * 1) {
                OneMbBody.append("abcdef ");
            }

            for (int i = 1; i <= 3; i++ ) {
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"mid\":\"10050" + i + "\","
                    + "\"body_text\":\"" + OneMbBody.toString() + "\"}]}",
                    StandardCharsets.UTF_8));
                response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
            }
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&get=mid&text=mid:*&sort=mid&asc"));
            text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            YandexAssert.check(
                new JsonChecker("{\"hitsCount\":4,\"hitsArray\":["
                + "{\"mid\":\"100500\"}"
                + ",{\"mid\":\"100501\"}"
                + ",{\"mid\":\"100502\"}"
                + ",{\"mid\":\"100503\"}"
                + "]}"), text);

            for (int i = 0; i < 20; i++) {
                Thread.sleep(1000);
                if (getJournalSize(root) == 1048751) {
                    break;
                }
            }
            Assert.assertEquals(1, getJournalCount(root));
            Assert.assertEquals(1048751, getJournalSize(root));
            success = true;
        } finally {
            System.err.println("testMaxPartJournalSize ended");
            if (success) {
                SearchBackendTestBase.removeDirectory(root);
            }
        }
    }
}


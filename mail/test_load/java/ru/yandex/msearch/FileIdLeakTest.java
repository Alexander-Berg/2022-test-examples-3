package ru.yandex.msearch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.lucene.store.BlockCompressedInputStreamBase;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class FileIdLeakTest extends TestBase {
    public void testFileIdLeak(final String fieldCodec) throws Exception {
        File root = Files.createTempDirectory("testFileIdLeak").toFile();
        int shards = 151;
        try (Daemon daemon =
                new Daemon(
                    SearchBackendTestBase.config(
                        root,
                        "\ndefault-field-codec = " + fieldCodec + '\n',
                        shards));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 30; ++i) {
                logger.info(
                    "Iteration #" + i+ ", max file id: "
                    + BlockCompressedInputStreamBase.maxFileId());
                sb.setLength(0);
                for (int j = 0; j < i; ++j) {
                    sb.append((char) ((j % 26) + 'a'));
                }
                String str = sb.toString();
                for (int j = 0; j < shards; ++j) {
                    StringBuilder text =
                        new StringBuilder(str.length() * 3 + 100);
                    text.append("{\"prefix\":");
                    text.append(j);
                    text.append(",\"docs\":[{\"text\":\"");
                    text.append(str);
                    text.append(' ');
                    text.append(j);
                    text.append(' ');
                    text.append(str);
                    text.append(' ');
                    text.append(i);
                    text.append(' ');
                    text.append(str);
                    text.append("\"}]}");
                    HttpPost post = new HttpPost("http://localhost:"
                        + daemon.jsonServerPort() + "/add");
                    post.setEntity(
                        new StringEntity(
                            new String(text),
                            StandardCharsets.UTF_8));
                    try (CloseableHttpResponse response =
                            client.execute(post))
                    {
                        HttpAssert.assertStatusCode(
                            HttpStatus.SC_OK,
                            response);
                    }
                }
                try (CloseableHttpResponse response =
                        client.execute(
                            new HttpGet(
                                "http://localhost:"
                                + daemon.jsonServerPort()
                                + "/flush?wait=true")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                }
                try (CloseableHttpResponse response =
                        client.execute(
                            new HttpGet(
                                "http://localhost:"
                                + daemon.searchPort()
                                + "/optimize?optimize=1")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                }
                YandexAssert.assertLess(
                    5000,
                    BlockCompressedInputStreamBase.maxFileId());
                Thread.sleep(100L);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testFileIdLeakYandexCodec() throws Exception {
        testFileIdLeak("Yandex");
    }

    @Test
    public void testFileIdLeakYandex2Codec() throws Exception {
        testFileIdLeak("Yandex2");
    }
}


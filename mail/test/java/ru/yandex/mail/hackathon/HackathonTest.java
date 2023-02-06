package ru.yandex.mail.hackathon;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.test.util.TestBase;

public class HackathonTest extends TestBase {
    @Ignore
    @Test
    public void testGet() throws Exception {
        try (HackathonCluster cluster = new HackathonCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(cluster.hackathon().host().toString())))
            {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_METHOD_NOT_ALLOWED,
                    response);
            }
        }
    }

    @Ignore
    @Test
    public void test() throws Exception {
        try (HackathonCluster cluster = new HackathonCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            HttpPost post =
                new HttpPost(cluster.hackathon().host().toString());

            post.setEntity(new StringEntity("Hello, world!"));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    response);
            }
        }
    }
}


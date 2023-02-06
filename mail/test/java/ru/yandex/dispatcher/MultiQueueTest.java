package ru.yandex.dispatcher;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.HttpResource;
import ru.yandex.http.test.SlowpokeHttpResource;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.util.TestBase;

public class MultiQueueTest extends TestBase {

    protected HttpEntity multipartEntity(
        final int shard,
        final int count,
        final String uri)
        throws Exception
    {
        MultipartEntityBuilder builder0 = MultipartEntityBuilder.create();
        builder0.setMimeSubtype("mixed");
        final String envelopeName = "envelope.json";
        for (int i = 0; i < count; i++) {
            builder0.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        String.valueOf(shard))
                    .addField(
                        YandexHeaders.URI,
                        uri + shard)
                    .setBody(
                        new ByteArrayBody(
                            new byte[0],
                            ContentType.APPLICATION_JSON,
                            null))
                    .setName(envelopeName)
                    .build());
        }

        return builder0.build();
    }

    @Test
    public void test() throws Exception {
        try (ZoolooserCluster cluster = new ZoolooserCluster(
            this,
            2,
            true,
            true))
        {
            final int timeout = 10000;
            String notify0 = "/notify?service=change_log_0&shard=";
            String notify1 = "/notify?service=change_log_1&shard=";

            SlowpokeHttpResource backendRes0 = new SlowpokeHttpResource(
                new StaticHttpResource(HttpStatus.SC_OK),
                100);
            SlowpokeHttpResource backendRes1 = new SlowpokeHttpResource(
                new StaticHttpResource(HttpStatus.SC_OK),
                100);
            cluster.backend().add(notify0 + "*", backendRes0);
            cluster.backend().add(notify1 + "*", backendRes1);

            // first we send 30 requests to first queue
            HttpPost post0 = new HttpPost(cluster.producer().host()
                + "/multipart?service=change_log_0");
            HttpPost post1 =
                new HttpPost(
                    cluster.producer().host()
                        + "/multipart?service=change_log_1");
            post0.setEntity(multipartEntity(5, 5, notify0));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post0);
            post1.setEntity(multipartEntity(5, 5, notify1));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post1);

            waitForRequests(backendRes0, (r) -> r == 5, timeout);
            waitForRequests(backendRes1, (r) -> r == 5, timeout);

            post0.setEntity(multipartEntity(5, 5, notify0));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post0);
            post0.setEntity(multipartEntity(6, 5, notify0));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post1);
            post0.setEntity(multipartEntity(5, 5, notify1));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post0);
            post0.setEntity(multipartEntity(7, 5, notify0));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post0);
            // now we have tootal 20 requests to change_log_0 and 5 already got
            // 10 requests to change_log_1 and 5 already got
            waitForRequests(backendRes0, (r) -> r >= 10, timeout);

            System.out.println("Stopping first queue node");
            cluster.queueNodes().get(0).close();
            System.out.println("Stopping first queue node finished");

            // now pushing to second more, total 15
            post1.setEntity(multipartEntity(8, 5, notify1));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post1);
            waitForRequests(backendRes1, (r) -> r == 15, timeout);
        }
    }

    protected void waitForRequests(
        final HttpResource resource,
        final Function<Integer, Boolean> func,
        final int timeout)
        throws Exception
    {
        int sleepTime = 100;
        int waiting = 0;
        while (!func.apply(resource.accessCount())) {
            Thread.sleep(sleepTime);
            waiting += sleepTime;
            if (waiting > timeout) {
                throw new TimeoutException(
                    "Timeout waiting requests, now we have "
                        + resource.accessCount());
            }
        }
    }
    //CSON: MagicNumber
}

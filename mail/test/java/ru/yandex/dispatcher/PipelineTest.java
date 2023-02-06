package ru.yandex.dispatcher;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.test.util.TestBase;

public class PipelineTest extends TestBase {
    private static final int TIMEOUT = 15000;

    @Test
    public void testMultipart() throws Exception {
        try (ZoolooserCluster cluster = new ZoolooserCluster(this)) {
            Thread.sleep(1000);

            ByteArrayBody emptyBody = new ByteArrayBody(
                new byte[0],
                ContentType.APPLICATION_JSON,
                null);

            // multiprefix mixed
            String getUri = "/getRequest1";
            String postUri = "/postRequest";
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMimeSubtype("mixed");
            final String envelopeName = "envelope.json";
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(YandexHeaders.ZOO_SHARD_ID, "5")
                    .addField(YandexHeaders.URI, getUri + "?service=change_log_0&prefix=5")
                    .addField(
                        YandexHeaders.ZOO_HTTP_METHOD,
                        HttpGet.METHOD_NAME)
                    .setBody(emptyBody)
                    .setName(envelopeName)
                    .build());
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        "10")
                    .addField(YandexHeaders.URI, postUri + "?service=change_log_0&prefix=10")
                    .setBody(emptyBody)
                    .setName(envelopeName)
                    .build());
            HttpPost post1 =
                new HttpPost(
                    cluster.producer().host()
                        + "/multipart?service=change_log_0");

            post1.setEntity(builder.build());

            StaticHttpResource getRes = new StaticHttpResource(new StaticHttpItem(HttpStatus.SC_OK));
            StaticHttpResource postRes = new StaticHttpResource(new StaticHttpItem(HttpStatus.SC_OK));

            cluster.backend().register(
                new Pattern<>(getUri, true),
                getRes.next(),
                RequestHandlerMapper.GET);

            cluster.backend().register(
                new Pattern<>(postUri, true),
                postRes.next(),
                RequestHandlerMapper.POST);

            cluster.backend().add("/delete?updatePosition*", new StaticHttpResource(HttpStatus.SC_OK));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post1);
            waitForRequests((r) -> getRes.accessCount() ==  r, 1, TIMEOUT);
            waitForRequests((r) -> postRes.accessCount() ==  r, 1, TIMEOUT);
            // single prefix
            builder = MultipartEntityBuilder.create();
            builder.setMimeSubtype("mixed");
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(YandexHeaders.URI, postUri + "?service=change_log_0&prefix=5")
                    .addField(
                        YandexHeaders.ZOO_HTTP_METHOD,
                        HttpPost.METHOD_NAME)
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        "5")
                    .setBody(emptyBody)
                    .setName(envelopeName)
                    .build());
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(YandexHeaders.URI, getUri + "?service=change_log_0&prefix=5")
                    .addField(
                        YandexHeaders.ZOO_HTTP_METHOD,
                        HttpGet.METHOD_NAME)
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        "5")
                    .setBody(emptyBody)
                    .setName(envelopeName)
                    .build());
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(YandexHeaders.URI, postUri + "?service=change_log_0&prefix=5")
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        "5")
                    .setBody(emptyBody)
                    .setName(envelopeName)
                    .build());
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(YandexHeaders.URI, getUri + "?service=change_log_0&prefix=5")
                    .addField(
                        YandexHeaders.ZOO_HTTP_METHOD,
                        HttpGet.METHOD_NAME)
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        "5")
                    .setBody(emptyBody)
                    .setName(envelopeName)
                    .build());
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(YandexHeaders.URI, getUri + "?service=change_log_0&prefix=5")
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        "5")
                    .addField(
                        YandexHeaders.ZOO_HTTP_METHOD,
                        HttpGet.METHOD_NAME)
                    .setBody(emptyBody)
                    .setName(envelopeName)
                    .build());

            HttpPost post2 =
                new HttpPost(
                    cluster.producer().host()
                        + "/multipart?service=change_log_0");
            post2.setEntity(builder.build());

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post2);
            waitForRequests((r) -> getRes.accessCount() ==  r, 4, TIMEOUT);
            waitForRequests((r) -> postRes.accessCount() ==  r, 3, TIMEOUT);
        }
    }

    @Test
    public void test() throws Exception {
        try (ZoolooserCluster cluster = new ZoolooserCluster(this)) {
            Thread.sleep(1000);
            String notify1 = "/notify?service=change_log_0&shard=5";
            String notify2 = "/notify?service=change_log_0&shard=10";

            HttpGet get1 =
                new HttpGet(cluster.producer().host().toString() + notify1);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get1);
            HttpGet get2 =
                new HttpGet(cluster.producer().host().toString() + notify2);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get2);
            String delete1Uri = "/delete?updatePosition&prefix=5"
                + "&shard=5&service=change_log_0&position=0";
            String delete2Uri = "/delete?updatePosition&prefix=10"
                + "&shard=10&service=change_log_0&position=0";
            // delete1 should not be called
            cluster.backend().add(
                delete1Uri,
                new StaticHttpResource(HttpStatus.SC_NOT_IMPLEMENTED));
            cluster.backend().add(
                delete2Uri,
                new ChainedHttpResource(
                    new StaticHttpItem(HttpStatus.SC_INTERNAL_SERVER_ERROR),
                    new StaticHttpItem(HttpStatus.SC_OK)));

            String backendNotify1 = notify1 + "&zoo-queue-id=0&deadline=*";
            cluster.backend().add(
                backendNotify1,
                new ChainedHttpResource(
                    new StaticHttpItem(HttpStatus.SC_INTERNAL_SERVER_ERROR),
                    new StaticHttpItem(HttpStatus.SC_INTERNAL_SERVER_ERROR),
                    new StaticHttpItem(HttpStatus.SC_OK)));

            String backendNotify2 = notify2 + '*';
            cluster.backend().add(
                backendNotify2,
                HttpStatus.SC_NOT_IMPLEMENTED);

            // notify2 not implemented, commiting position
            waitForRequests(cluster.backend(), delete2Uri, 2, TIMEOUT);
            waitForRequests(cluster.backend(), backendNotify1, 3, TIMEOUT);
            waitForRequests(cluster.backend(), backendNotify2, 1, TIMEOUT);

            Assert.assertEquals(
                1,
                cluster.backend().accessCount(backendNotify2));
            // notify1 2 fails 1 - success
            Assert.assertEquals(
                3,
                cluster.backend().accessCount(backendNotify1));

            //test multipart
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMimeSubtype("mixed");
            final String envelopeName = "envelope.json";
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        "5")
                    .addField(
                        YandexHeaders.URI,
                        "/ping200?service=change_log_0&prefix=5")
                    .setBody(
                        new ByteArrayBody(
                            new byte[0],
                            ContentType.APPLICATION_JSON,
                            null))
                    .setName(envelopeName)
                    .build());
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        "10")
                    .addField(
                        YandexHeaders.URI,
                        "/ping400?service=change_log_0&prefix=10")
                    .setBody(new ByteArrayBody(
                        new byte[0],
                        ContentType.APPLICATION_JSON,
                        null))
                    .setName(envelopeName)
                    .build());
            builder.addPart(
                FormBodyPartBuilder
                    .create()
                    .addField(
                        YandexHeaders.ZOO_SHARD_ID,
                        "3")
                    .addField(
                        YandexHeaders.URI,
                        "/ping200?service=change_log_0&prefix=3")
                    .setBody(new ByteArrayBody(
                        new byte[0],
                        ContentType.APPLICATION_JSON,
                        null))
                    .setName(envelopeName)
                    .build());

            HttpPost post1 =
                new HttpPost(
                    cluster.producer().host()
                        + "/multipart?service=change_log_0");

            post1.setEntity(builder.build());

            String pingShard5 =
                "/ping200?service=change_log_0"
                    + "&prefix=5&zoo-queue-id=1&deadline=*";
            cluster.backend().add(pingShard5, HttpStatus.SC_OK);

            String pingShard10 =
                "/ping400?service=change_log_0"
                    + "&prefix=10&zoo-queue-id=1&deadline=*";
            String deleteShard10 = "/delete?updatePosition&prefix=10"
                + "&shard=10&service=change_log_0&position=1";
            cluster.backend().add(pingShard10, HttpStatus.SC_BAD_REQUEST);
            cluster.backend().add(deleteShard10, HttpStatus.SC_OK);

            String pingShard3 =
                "/ping200?service=change_log_0&prefix=3"
                    + "&zoo-queue-id=0&deadline=*";
            cluster.backend().add(pingShard3, HttpStatus.SC_OK);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post1);
            waitForRequests(cluster.backend(), pingShard5, 1, TIMEOUT);
            waitForRequests(cluster.backend(), pingShard10, 1, TIMEOUT);
            waitForRequests(cluster.backend(), deleteShard10, 1, TIMEOUT);
            waitForRequests(cluster.backend(), pingShard3, 1, TIMEOUT);
        }
    }

    //CSOFF: ParameterNumber
    protected void waitForRequests(
        final StaticServer server,
        final String uri,
        final int reqs,
        final int timeout)
        throws Exception
    {
        waitForRequests((r) -> server.accessCount(uri) == r, reqs, timeout);
    }
    //CSON: ParameterNumber

    //CSOFF: MagicNumber
    protected void waitForRequests(
        final Function<Integer, Boolean> func,
        final int reqs,
        final int timeout)
        throws Exception
    {
        int sleepTime = 100;
        int waiting = 0;
        while (!func.apply(reqs)) {
            Thread.sleep(sleepTime);
            waiting += sleepTime;
            if (waiting > timeout) {
                throw new TimeoutException("Timeout waiting requests");
            }
        }
    }
    //CSON: MagicNumber
}

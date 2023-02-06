package ru.yandex.logbroker2;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.test.util.TestBase;

public class MessageSenderTest extends TestBase {
    private static final byte[] SOURCE_ID =
        "some id".getBytes(StandardCharsets.UTF_8);

    public MessageSenderTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        try (MessageSenderCluster cluster =
                new MessageSenderCluster(
                    this,
                    "empty.conf",
                    "/test",
                    1,
                    false))
        {
            String uri1=
                "/test?topic=some-topic&partition=1&offset=15&seqNo=25"
                + "&message-create-time=1234567890"
                + "&message-write-time=1234567891";
            cluster.targetServer().add(
                uri1,
                new StaticHttpResource(new ExpectingHttpItem("Привет, мир")));

            String uri2=
                "/test?topic=some-topic&partition=1&offset=16&seqNo=26"
                + "&message-create-time=1234567895"
                + "&message-write-time=1234567896";
            cluster.targetServer().add(
                uri2,
                new StaticHttpResource(new ExpectingHttpItem("Hello, world")));

            cluster.start();

            LBTopicContext topicContext = cluster.topicContext();

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Привет, мир".getBytes(StandardCharsets.UTF_8),
                        15L,
                        new MessageMeta(
                            SOURCE_ID,
                            25L,
                            1234567890L,
                            1234567891L,
                            "127.0.0.2",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Hello, world".getBytes(StandardCharsets.UTF_8),
                        16L,
                        new MessageMeta(
                            SOURCE_ID,
                            26L,
                            1234567895L,
                            1234567896L,
                            "127.0.0.3",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            cluster.messageSender().sendMessages(topicContext).get();

            // For non-batching requests returned Future will point only to
            // first request, so we should wait for other requests to complete
            Thread.sleep(1000L);

            logger.info("Checking access count");
            Assert.assertEquals(
                1,
                cluster.targetServer().accessCount(uri1));
            Assert.assertEquals(
                1,
                cluster.targetServer().accessCount(uri2));
        }
    }

    @Test
    public void testBatching() throws Exception {
        try (MessageSenderCluster cluster =
                new MessageSenderCluster(
                    this,
                    "empty.conf",
                    "/test?param",
                    5,
                    false))
        {
            String uri1=
                "/test?param&topic=some-topic&partition=1&offset=15&seqNo=25"
                + "&message-create-time=1234567890"
                + "&message-write-time=1234567891";
            cluster.anotherServer().add(
                uri1,
                new StaticHttpResource(new ExpectingHttpItem("Привет, мир")));

            String uri2=
                "/test?param&topic=some-topic&partition=1&offset=16&seqNo=26"
                + "&message-create-time=1234567895"
                + "&message-write-time=1234567896";
            cluster.anotherServer().add(
                uri2,
                new StaticHttpResource(new ExpectingHttpItem("Hello, world")));

            String uri3=
                "/test?param&topic=some-topic&partition=1&offset=17&seqNo=27"
                + "&message-create-time=1234567898"
                + "&message-write-time=1234567899";
            cluster.anotherServer().add(
                uri3,
                new StaticHttpResource(new ExpectingHttpItem("Hello again")));

            cluster.targetServer().add(
                "*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(
                        cluster.anotherServer().port(),
                        "prefix")));

            cluster.start();

            LBTopicContext topicContext = cluster.topicContext();

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Привет, мир".getBytes(StandardCharsets.UTF_8),
                        15L,
                        new MessageMeta(
                            SOURCE_ID,
                            25L,
                            1234567890L,
                            1234567891L,
                            "127.0.0.2",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Hello, world".getBytes(StandardCharsets.UTF_8),
                        16L,
                        new MessageMeta(
                            SOURCE_ID,
                            26L,
                            1234567895L,
                            1234567896L,
                            "127.0.0.3",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Hello again".getBytes(StandardCharsets.UTF_8),
                        17L,
                        new MessageMeta(
                            SOURCE_ID,
                            27L,
                            1234567898L,
                            1234567899L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            cluster.messageSender().sendMessages(topicContext).get();

            logger.info("Checking access count");
            Assert.assertEquals(
                1,
                cluster.anotherServer().accessCount(uri1));
            Assert.assertEquals(
                1,
                cluster.anotherServer().accessCount(uri2));
            Assert.assertEquals(
                1,
                cluster.anotherServer().accessCount(uri3));
        }
    }

    @Test
    public void testFields() throws Exception {
        try (MessageSenderCluster cluster =
                new MessageSenderCluster(
                    this,
                    "ohio_direct.conf",
                    "/test?param",
                    1,
                    false))
        {
            String data1 =
                "{\"dt\":1,\"postauth_dt\":\"1111\",\"passport_id\":5598601,"
                + "\"payload\":\"something\", \"purchase_token\":\"a\"}";
            String uri1=
                "/test?param&topic=some-topic&partition=1&offset=55&seqNo=65"
                + "&message-create-time=1234567990"
                + "&message-write-time=1234567991"
                + "&prefix=5598601&purchase_token=a";
            cluster.targetServer().add(
                uri1,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(data1),
                        "prefix",
                        "5598601")));

            String data2 =
                "{\"dt\":2,\"postauth_dt\":\"1112\",\"passport_id\":5598602,"
                + "\"payload\":\"something here\", \"purchase_token\":\"b\"}";
            String uri2=
                "/test?param&topic=some-topic&partition=1&offset=56&seqNo=66"
                + "&message-create-time=1234567995"
                + "&message-write-time=1234567996"
                + "&prefix=5598602&purchase_token=b";
            cluster.targetServer().add(
                uri2,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(data2),
                        "prefix",
                        "5598602")));

            cluster.start();

            LBTopicContext topicContext = cluster.topicContext();

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Привет, мир".getBytes(StandardCharsets.UTF_8),
                        15L,
                        new MessageMeta(
                            SOURCE_ID,
                            25L,
                            1234567890L,
                            1234567891L,
                            "127.0.0.2",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Hello, world".getBytes(StandardCharsets.UTF_8),
                        16L,
                        new MessageMeta(
                            SOURCE_ID,
                            26L,
                            1234567895L,
                            1234567896L,
                            "127.0.0.3",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Hello again".getBytes(StandardCharsets.UTF_8),
                        17L,
                        new MessageMeta(
                            SOURCE_ID,
                            27L,
                            1234567898L,
                            1234567899L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        data1.getBytes(StandardCharsets.UTF_8),
                        55L,
                        new MessageMeta(
                            SOURCE_ID,
                            65L,
                            1234567990L,
                            1234567991L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        data2.getBytes(StandardCharsets.UTF_8),
                        56L,
                        new MessageMeta(
                            SOURCE_ID,
                            66L,
                            1234567995L,
                            1234567996L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "{\"postauth_dt\":1}".getBytes(StandardCharsets.UTF_8),
                        57L,
                        new MessageMeta(
                            SOURCE_ID,
                            67L,
                            1234567898L,
                            1234567899L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "{\"passport_id\":1}".getBytes(StandardCharsets.UTF_8),
                        58L,
                        new MessageMeta(
                            SOURCE_ID,
                            68L,
                            1234567898L,
                            1234567899L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            cluster.messageSender().sendMessages(topicContext).get();

            // For non-batching requests returned Future will point only to
            // first request, so we should wait for other requests to complete
            Thread.sleep(1000L);

            logger.info("Checking access count");
            Assert.assertEquals(
                1,
                cluster.targetServer().accessCount(uri1));
            Assert.assertEquals(
                1,
                cluster.targetServer().accessCount(uri2));
            Assert.assertEquals(7, topicContext.messagesProcessedCount());
            Assert.assertEquals(2, topicContext.requiredFieldMissingCount());
            Assert.assertEquals(3, topicContext.parseFailedCount());
        }
    }

    @Test
    public void testFieldsBatching() throws Exception {
        try (MessageSenderCluster cluster =
                new MessageSenderCluster(
                    this,
                    "ohio_direct.conf",
                    "/test?param",
                    100,
                    false))
        {
            String data1 =
                "{\"dt\":1,\"postauth_dt\":\"1111\",\"passport_id\":5598601,"
                + "\"payload\":\"something\", \"purchase_token\":\"a\"}";
            String uri1=
                "/test?param&topic=some-topic&partition=1&offset=55&seqNo=65"
                + "&message-create-time=1234567990"
                + "&message-write-time=1234567991"
                + "&prefix=5598601&purchase_token=a";
            cluster.anotherServer().add(
                uri1,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(data1),
                        "prefix",
                        "5598601")));

            String data2 =
                "{\"dt\":2,\"postauth_dt\":\"1112\",\"passport_id\":5598602,"
                + "\"payload\":\"something here\", \"purchase_token\":\"b\"}";
            String uri2=
                "/test?param&topic=some-topic&partition=1&offset=56&seqNo=66"
                + "&message-create-time=1234567995"
                + "&message-write-time=1234567996"
                + "&prefix=5598602&purchase_token=b";
            cluster.anotherServer().add(
                uri2,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ExpectingHttpItem(data2),
                        "prefix",
                        "5598602")));

            cluster.targetServer().add(
                "*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(
                        cluster.anotherServer().port(),
                        "prefix")));

            cluster.start();

            LBTopicContext topicContext = cluster.topicContext();

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Привет, мир".getBytes(StandardCharsets.UTF_8),
                        15L,
                        new MessageMeta(
                            SOURCE_ID,
                            25L,
                            1234567890L,
                            1234567891L,
                            "127.0.0.2",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Hello, world".getBytes(StandardCharsets.UTF_8),
                        16L,
                        new MessageMeta(
                            SOURCE_ID,
                            26L,
                            1234567895L,
                            1234567896L,
                            "127.0.0.3",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "Hello again".getBytes(StandardCharsets.UTF_8),
                        17L,
                        new MessageMeta(
                            SOURCE_ID,
                            27L,
                            1234567898L,
                            1234567899L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        data1.getBytes(StandardCharsets.UTF_8),
                        55L,
                        new MessageMeta(
                            SOURCE_ID,
                            65L,
                            1234567990L,
                            1234567991L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        data2.getBytes(StandardCharsets.UTF_8),
                        56L,
                        new MessageMeta(
                            SOURCE_ID,
                            66L,
                            1234567995L,
                            1234567996L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "{\"postauth_dt\":1}".getBytes(StandardCharsets.UTF_8),
                        57L,
                        new MessageMeta(
                            SOURCE_ID,
                            67L,
                            1234567898L,
                            1234567899L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        "{\"passport_id\":1}".getBytes(StandardCharsets.UTF_8),
                        58L,
                        new MessageMeta(
                            SOURCE_ID,
                            68L,
                            1234567898L,
                            1234567899L,
                            "127.0.0.4",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));

            cluster.messageSender().sendMessages(topicContext).get();

            logger.info("Checking access count");
            Assert.assertEquals(
                1,
                cluster.anotherServer().accessCount(uri1));
            Assert.assertEquals(
                1,
                cluster.anotherServer().accessCount(uri2));
            Assert.assertEquals(7, topicContext.messagesProcessedCount());
            Assert.assertEquals(2, topicContext.requiredFieldMissingCount());
            Assert.assertEquals(3, topicContext.parseFailedCount());
        }
    }
}


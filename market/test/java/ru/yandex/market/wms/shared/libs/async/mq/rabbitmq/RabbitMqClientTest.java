package ru.yandex.market.wms.shared.libs.async.mq.rabbitmq;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.shared.libs.async.configuration.RabbitMqAdminClientConfiguration;
import ru.yandex.market.wms.shared.libs.async.mq.MqAdminClient;
import ru.yandex.market.wms.shared.libs.async.mq.rabbit.RabbitMqAdminClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RabbitMqClientTest {
    private static final String BASE = "async/mq/rabbitmq/";

    private MockWebServer mws;
    private MqAdminClient client;

    @BeforeEach
    public void beforeEach() throws Exception {
        mws = new MockWebServer();
        mws.start();

        URI baseUri = new URI("http", null, "localhost", mws.getPort(), null, null, null);

        RabbitMqAdminClientConfiguration rmqClientConfig = new RabbitMqAdminClientConfiguration();
        client = rmqClientConfig.mqAdminClient(baseUri.toString(), "username", "password", "amq.direct");

        Field warehouseIdField = RabbitMqAdminClient.class.getDeclaredField("warehouseId");
        warehouseIdField.setAccessible(true);
        warehouseIdField.set(RabbitMqAdminClient.class, "0");
    }

    @AfterEach
    public void afterEach() throws IOException {
        mws.shutdown();
    }

    private void addEmptyResponse(MockWebServer server) throws IOException {
        MockResponse resp = new MockResponse().setResponseCode(200);
        server.enqueue(resp);
    }

    private void addResponse(MockWebServer server, String path) throws IOException {
        MockResponse resp = new MockResponse()
                .setResponseCode(200)
                .setHeader("content-type", "application/json")
                .setBody(getFileContent(path));
        server.enqueue(resp);
    }

    private String getFileContent(String path) throws IOException {
        return IOUtils.toString(
                RabbitMqClientTest.class.getClassLoader().getResource(BASE + path),
                StandardCharsets.UTF_8);
    }

    private void assertRequest(String method, String path, String reqFilePath, RecordedRequest req) throws IOException {
        assertEquals(method, req.getMethod());
        assertEquals(path, req.getPath());
        assertEquals(getFileContent(reqFilePath), req.getBody().readUtf8());
    }

    @Test
    @Disabled
    public void initialQueuesFetch() throws Exception {
        addResponse(mws, "get_queues/response.json");

        List<String> expectedQueues =
                Arrays.asList(
                        "rmq_661_pushDimension",
                        "rmq_661_pushReferenceItems",
                        "rmq_661_putReferenceItems",
                        "rmq_661_transport-unit-tracking"
                );
        assertEquals(expectedQueues, client.fetchKnownDestinations());
    }

    @Test
    public void createBaseQueue() throws Exception {
        addEmptyResponse(mws);
        addEmptyResponse(mws);

        client.createDestination("test_queue");

        assertRequest(
                "PUT",
                "/api/queues/WMS/test_queue",
                "create_base_queue/request_queue.json",
                mws.takeRequest());
        assertRequest(
                "POST",
                "/api/bindings/WMS/e/amq.direct/q/test_queue",
                "create_base_queue/request_binding.json",
                mws.takeRequest());
    }

    @Test
    public void createDelayQueue() throws Exception {
        addEmptyResponse(mws);
        addEmptyResponse(mws);

        client.createDestination("test_queue_delay");

        assertRequest(
                "PUT",
                "/api/queues/WMS/test_queue_delay",
                "create_delay_queue/request_queue.json",
                mws.takeRequest());
        assertRequest(
                "POST",
                "/api/bindings/WMS/e/amq.direct/q/test_queue_delay",
                "create_delay_queue/request_binding.json",
                mws.takeRequest());
    }

    @Test
    public void createDeadQueue() throws Exception {
        addEmptyResponse(mws);
        addEmptyResponse(mws);

        client.createDestination("test_queue_dead");

        assertRequest(
                "PUT",
                "/api/queues/WMS/test_queue_dead",
                "create_dead_queue/request_queue.json",
                mws.takeRequest());
        assertRequest(
                "POST",
                "/api/bindings/WMS/e/amq.direct/q/test_queue_dead",
                "create_dead_queue/request_binding.json",
                mws.takeRequest());
    }

    @Test
    public void createDeadQueueWithTtl() throws Exception {
        addEmptyResponse(mws);
        addEmptyResponse(mws);

        client.createDestination("test_queue_dead?ttl=7200");

        assertRequest(
                "PUT",
                "/api/queues/WMS/test_queue_dead",
                "create_dead_queue/request_queue_with_ttl.json",
                mws.takeRequest());
        assertRequest(
                "POST",
                "/api/bindings/WMS/e/amq.direct/q/test_queue_dead",
                "create_dead_queue/request_binding.json",
                mws.takeRequest());
    }
}

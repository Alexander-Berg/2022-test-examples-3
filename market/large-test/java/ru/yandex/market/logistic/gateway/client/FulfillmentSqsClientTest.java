package ru.yandex.market.logistic.gateway.client;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;

import ru.yandex.market.logistic.gateway.client.config.ProxySettings;
import ru.yandex.market.logistic.gateway.client.config.SqsProperties;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaskResultConsumer;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistic.gateway.utils.FulfillmentDtoFactory;

public class FulfillmentSqsClientTest extends AbstractSqsClientTest {

    private static final int BIG_PAYLOAD_COMMENT_LEN = 300 * 1024;

    private static final String PROCESS_ID = "processIdABC123";

    private static final ClientRequestMeta CLIENT_REQUEST_META = new ClientRequestMeta(PROCESS_ID);

    private static final CreateOrderRestrictedData RESTRICTED_DATA = FulfillmentDtoFactory.createOrderRestrictedData();

    private FulfillmentClient fulfillmentClient;

    @Before
    public void setup() {
        fulfillmentClient = FulfillmentClientFactory.getFulfillmentClient(new SqsProperties()
            .setRegion(DEFAULT_REGION)
            .setS3EndpointHost(getS3ConnectionString())
            .setSqsEndpointHost(getSqsConnectionString())
            .setS3BucketName(BUCKET_NAME)
            .setS3AccessKey("")
            .setS3SecretKey("")
            .setSqsAccessKey("")
            .setSqsSecretKey("")
            .setSqsSessionToken("")
            .setProxySettings(new ProxySettings(DockerClientFactory.instance().dockerHostIpAddress(),
                sqsContainer.getMappedPort(SQS_PORT))),
            null,
            TaskResultConsumer.MDB);
    }

    @Test
    public void testCreateOrderSuccessSent() throws GatewayApiException {
        Order order = FulfillmentDtoFactory.createOrder();
        Partner partner = FulfillmentDtoFactory.createPartner();

        fulfillmentClient.createOrder(order, partner);
    }

    @Test
    public void testCreateOrderSuccessSentWithProcessId() throws GatewayApiException {
        Order order = FulfillmentDtoFactory.createOrder();
        Partner partner = FulfillmentDtoFactory.createPartner();

        fulfillmentClient.createOrder(order, partner, RESTRICTED_DATA, CLIENT_REQUEST_META);
    }

    @Test
    public void testCreateOrderWithBigPayloadSuccessSent() throws GatewayApiException {
        Order order = FulfillmentDtoFactory.createOrderBuilder()
            .setComment(RandomStringUtils.randomAlphanumeric(BIG_PAYLOAD_COMMENT_LEN))
            .build();
        Partner partner = FulfillmentDtoFactory.createPartner();

        fulfillmentClient.createOrder(order, partner);
    }

    @Test
    public void testCreateOrderWithBigPayloadSuccessSentWithProcessId() throws GatewayApiException {
        Order order = FulfillmentDtoFactory.createOrderBuilder()
            .setComment(RandomStringUtils.randomAlphanumeric(BIG_PAYLOAD_COMMENT_LEN))
            .build();
        Partner partner = FulfillmentDtoFactory.createPartner();

        fulfillmentClient.createOrder(order, partner, RESTRICTED_DATA, CLIENT_REQUEST_META);
    }
}

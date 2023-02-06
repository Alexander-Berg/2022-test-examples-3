package ru.yandex.market.logistic.gateway.service.flow;

import java.time.Clock;

import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.logistic.gateway.AbstractTestContainers;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.DeliveryClientFactory;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClientFactory;
import ru.yandex.market.logistic.gateway.client.config.ProxySettings;
import ru.yandex.market.logistic.gateway.client.config.SqsProperties;
import ru.yandex.market.logistic.gateway.config.AmazonSQSCustomClient;
import ru.yandex.market.logistic.gateway.config.AwsConfig;
import ru.yandex.market.logistic.gateway.config.properties.FlowMapProperties;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.delivery.CreateOrderExecutorResponse;
import ru.yandex.market.logistic.gateway.model.fulfillment.CreateInboundExecutorResponse;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.repository.SqsMessageRepository;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateOrderErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateOrderExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CreateOrderSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateInboundErrorExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateInboundExecutor;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.async.CreateInboundSuccessExecutor;
import ru.yandex.market.logistic.gateway.service.util.SqsDeduplicationAspect;
import ru.yandex.market.logistic.gateway.utils.FulfillmentDtoFactory;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {"flow.retryProperties[DS_CREATE_ORDER].limit = 3",
    "flow.retryProperties[DS_CREATE_ORDER].intervals = 2"})
public abstract class RequestFlowTest extends AbstractTestContainers {

    protected static final int SQS_OPERATION_TIMEOUT_MILLIS = 30_000;
    protected static final int SQS_RETRY_TIMEOUT_MILLIS = 60_000;

    protected final SoftAssertions assertions = new SoftAssertions();

    @SpyBean
    protected FlowService flowService;

    @SpyBean
    protected SqsDeduplicationAspect sqsDeduplicationAspect;

    @SpyBean
    protected FlowMapProperties flowProps;

    @MockBean
    protected TvmClient tvmClient;

    @MockBean
    protected CreateOrderExecutor createOrderExecutor;

    @MockBean
    protected CreateOrderSuccessExecutor createOrderSuccessExecutor;

    @MockBean
    protected CreateOrderErrorExecutor createOrderErrorExecutor;

    @MockBean
    protected CreateInboundExecutor createInboundExecutor;

    @MockBean
    protected CreateInboundSuccessExecutor createInboundSuccessExecutor;

    @MockBean
    protected CreateInboundErrorExecutor createInboundErrorExecutor;

    @Autowired
    protected AwsConfig awsConfig;

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    protected ClientTaskRepository clientTaskRepository;

    @Autowired
    protected SqsMessageRepository sqsMessageRepository;

    @Autowired
    protected TransactionTemplate readWriteTransactionTemplate;

    @Autowired
    protected ThreadPoolTaskExecutor highPriorityTaskExecutor;

    @Autowired
    protected AmazonSQSCustomClient jmsAmazonSQSCustomClient;

    @Autowired
    protected JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    @Autowired
    protected Clock clock;

    protected TaskMessage createOrderTaskMessage;

    protected TaskMessage emptyTaskMessage;

    public DeliveryClient getDeliveryClient() {
        return DeliveryClientFactory.getDeliveryClient(getSqsProperties(), null, null, clock);
    }

    public FulfillmentClient getFulfillmentClient() {
        return FulfillmentClientFactory.getFulfillmentClient(getSqsProperties(), null, null, clock);
    }

    private SqsProperties getSqsProperties() {
        return (new SqsProperties())
            .setRegion(awsConfig.getRegion())
            .setS3EndpointHost(awsConfig.getS3EndpointHost())
            .setS3AccessKey(awsConfig.getS3AccessKey())
            .setS3SecretKey(awsConfig.getS3SecretKey())
            .setS3BucketName(awsConfig.getS3BucketName())
            .setSqsAccessKey(awsConfig.getSqsAccessKey())
            .setSqsSecretKey(awsConfig.getSqsSecretKey())
            .setSqsSessionToken(awsConfig.getSqsSessionToken())
            .setSqsEndpointHost(awsConfig.getSqsEndpointHost())
            .setProxySettings(new ProxySettings(awsConfig.getProxyHost(), awsConfig.getProxyPort()));
    }

    @Before
    public void setup() throws Exception {

        RequestContextHolder.clearContext();

        emptyTaskMessage = new TaskMessage("");
        createOrderTaskMessage = new TaskMessage(
            mapper.writeValueAsString(new CreateOrderExecutorResponse("111", null, "1", null)));

        when(createOrderExecutor.execute(any()))
            .thenReturn(createOrderTaskMessage);

        CreateOrderResponse response = new CreateOrderResponse.CreateOrderResponseBuilder()
            .setOrderId(new ResourceId.ResourceIdBuilder().setYandexId("111").build())
            .build();

        when(createOrderSuccessExecutor.execute(any()))
            .thenReturn(new TaskMessage(mapper.writeValueAsString(response)));

        when(createOrderErrorExecutor.execute(any()))
            .thenReturn(new TaskMessage());

        when(createInboundExecutor.execute(any()))
            .thenReturn(new TaskMessage(mapper.writeValueAsString(
                new CreateInboundExecutorResponse(
                    FulfillmentDtoFactory.createResourceId("111"), null))));

        when(createInboundSuccessExecutor.execute(any())).thenReturn(new TaskMessage());
        when(createInboundErrorExecutor.execute(any())).thenReturn(new TaskMessage());
    }

    @After
    public void tearDown() {
        assertions.assertAll();

        purgeSqsQueue("ds-create-order");
        purgeSqsQueue("ds-create-order-success");
        purgeSqsQueue("ds-create-order-error");
        purgeSqsQueue("ff-create-inbound");
        purgeSqsQueue("ff-create-inbound-success");
        purgeSqsQueue("ff-create-inbound-error");
    }

    private void purgeSqsQueue(String queueName) {
        jmsAmazonSQSCustomClient.purgeQueue(new PurgeQueueRequest(
            String.format("http://localhost:%d/queue/%s/", SQS_PORT, queueName)));
    }
}

package ru.yandex.market.loyalty.admin.tms.checkouter;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import NKikimrClient.TGRpcServerGrpc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.yandex.persqueue.PersQueueServiceGrpc;
import com.yandex.ydb.persqueue.Persqueue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.stubbing.StubberImpl;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;
import ru.yandex.kikimr.persqueue.proxy.ProxyConfig;
import ru.yandex.kikimr.proto.Msgbus;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logbroker.consumer.LogbrokerReader;
import ru.yandex.market.logbroker.consumer.StreamListenerFactory;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConsumerParams;
import ru.yandex.market.logbroker.consumer.config.builder.LogbrokerConsumerBuilder;
import ru.yandex.market.logbroker.consumer.impl.StreamListenerFactoryImpl;
import ru.yandex.market.logbroker.consumer.util.LbReaderOffsetDao;
import ru.yandex.market.logbroker.consumer.util.impl.SimpleJdbcLbReaderOffsetDao;
import ru.yandex.market.loyalty.admin.config.logbroker.CheckouterEventsLogbrokerConsumerConfiguration;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_RECEIVED;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;

@TestPropertySource(
        locations = "classpath:test.properties"
)
@Import(CheckouterEventsLogbrokerConsumerConfiguration.class)
@SpyBean(CheckouterEventProcessor.class)
@TestFor({CheckouterEventLogbrokerProcessor.class, CheckouterEventProcessor.class})
@ActiveProfiles("test")
@EnableAspectJAutoProxy
public class CheckouterEventLogbrokerProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String IN_PROCESS_SERVER = InProcessServerBuilder.generateName();
    private static final String TEST_TOPIC = "/test/topic";
    private static final String TEST_CONSUMER = "market-loyalty/order-events-1";
    private static final StreamConsumerConfig DEFAULT_CONFIG = StreamConsumerConfig.builder(
            Collections.singleton(TEST_TOPIC),
            TEST_CONSUMER
    )
            // reader с нулевым буфером, чтобы лучше контролировать происходящее
            .configureReader(reader -> reader.setMaxInflightReads(0).setMaxUnconsumedReads(0))
            .configureRetries(retry -> retry.enable().setPolicy(10, TimeUnit.MILLISECONDS))
            .setExecutor(MoreExecutors.newDirectExecutorService())
            .build();

    @Mock
    private TGRpcServerGrpc.TGRpcServerImplBase serverImpl;
    @Mock
    private PersQueueServiceGrpc.PersQueueServiceImplBase persQ;
    @Mock
    private StreamObserver<Persqueue.ReadRequest> outboundStreamObserver;

    @Autowired
    private CheckouterEventProcessor processor;
    @Autowired
    private LogbrokerReader lbReader;
    @Autowired
    private List<LogbrokerConsumerBuilder<?>> logbrokerConsumerBuilder;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper checkouterAnnotationObjectMapper;


    private StreamObserver<Persqueue.ReadResponse> inboundStreamObserver;

    private Server server;
    private ManagedChannel channel;
    private ProxyBalancer proxyBalancer;
    private StreamListener listener;

    @Before
    public void setUp() throws IOException, InterruptedException {
        inboundStreamObserver = null;
        // start server mock
        server = InProcessServerBuilder.forName(IN_PROCESS_SERVER)
                .directExecutor()
                .addService(serverImpl)
                .addService(persQ)
                .build();
        server.start();
        channel = InProcessChannelBuilder.forName(IN_PROCESS_SERVER)
                .directExecutor()
                .build();
        proxyBalancer = spy(new ProxyBalancer(channel));

        List<? extends LogbrokerConsumerParams<?>> params = logbrokerConsumerBuilder.stream()
                .flatMap(b -> b.getParams().stream())
                .collect(Collectors.toList());

        LbReaderOffsetDao lbReaderOffsetDao = mock(SimpleJdbcLbReaderOffsetDao.class);
        when(lbReaderOffsetDao.getLbReaderOffsets(any())).thenReturn(Map.of());

        LogbrokerConsumerParams<?> firstReader = params.get(0);
        StreamListenerFactory factory = new StreamListenerFactoryImpl(
                lbReaderOffsetDao,
                firstReader.getParser(),
                new CheckouterEventsLogbrokerConsumerConfiguration.TypedTransactionTemplate(transactionManager),
                firstReader.getEventsConsumer(),
                firstReader.getStreamListenerExceptionStrategy(),
                firstReader.getDlqConfig()
        );

        listener = spy(factory.listener(lbReader));
        doCallRealMethod().when(listener).onRead(any(), any());
        doCallRealMethod().when(listener).onInit(any());

        doAnswer(invocation -> { // first try returns error
            StreamObserver<Msgbus.TResponse> observer = invocation.getArgument(1);
            observer.onError(Status.UNAVAILABLE.asException());
            return null;
        }).when(serverImpl).chooseProxy(any(), any());

        doCallRealMethod()
                .doReturn(CompletableFuture.completedFuture(new InProcessProxyConfig(IN_PROCESS_SERVER)))
                .when(proxyBalancer).chooseProxy();

        prepareRead(makeResponder(2));

        LogbrokerClientFactory clientFactory = new LogbrokerClientFactory(proxyBalancer);
        clientFactory
                .streamConsumer(DEFAULT_CONFIG)
                .startConsume(listener);
    }

    @After
    public void tearDown() {
        server.shutdown();
        server = null;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void logbrokerEnabledConsumerEventProcessTest() throws InterruptedException {
        configurationService.set(ConfigurationService.CHECKOUTER_LOGBROKER_ENABLED, true);

        ArgumentCaptor<Collection<OrderHistoryEventWithError>> captor = ArgumentCaptor.forClass(Collection.class);
        doNothing().when(processor).processCheckouterEvents(captor.capture());

        var finishTime = clock.instant().plus(5, ChronoUnit.SECONDS);
        var success = false;

        while (clock.instant().isBefore(finishTime) && !success) {
            success = captor.getAllValues().stream()
                    .mapToLong(Collection::size)
                    .sum() == 2;
            Thread.sleep(100);
        }

        assertTrue(success);
    }

    @Repeat(5)
    @Test
    @SuppressWarnings("unchecked")
    @Ignore
    public void logbrokerDisabledConsumerEventProcessTest() throws InterruptedException {
        configurationService.set(ConfigurationService.CHECKOUTER_LOGBROKER_ENABLED, false);
        configurationService.set(ConfigurationService.CHECKOUTER_LAST_EVENT_ID, 1L);

        ArgumentCaptor<Collection<OrderHistoryEventWithError>> captor = ArgumentCaptor.forClass(Collection.class);
        doNothing().when(processor).processCheckouterEvents(captor.capture());

        Thread.sleep(500);

        assertEquals(1, captor.getAllValues()
                .stream()
                .mapToLong(Collection::size)
                .sum());

        assertEquals(Long.valueOf(1L), captor.getAllValues()
                .stream()
                .flatMap(Collection::stream)
                .map(it -> it.orderHistoryEvent.get().getId())
                .findFirst()
                .orElse(0L));
    }

    /**
     * Мок сессии чтения, использующий переданный объект для обработки sendRead'ов.
     */
    private void prepareRead(StreamObserver<Persqueue.ReadRequest> responder) {
        when(persQ.readSession(any())).thenAnswer(invocation -> {
            inboundStreamObserver = invocation.getArgument(0);
            inboundStreamObserver.onNext(buildInit("1"));
            inboundStreamObserver.onNext(buildLock(1, 1L));
            inboundStreamObserver.onNext(buildLock(2, 2L));
            inboundStreamObserver.onNext(buildRelease(1, 1L));
            inboundStreamObserver.onNext(buildLock(3, 3L));
            return responder;
        });
    }

    /**
     * Возвращает StreamObserver, который на последовательные sendRead N раз отвечает удачным сообщением,
     * потом отвечать перестаёт.
     */
    private StreamObserver<Persqueue.ReadRequest> makeResponder(int count) {
        return makeResponder(Stream.iterate(1, idx -> idx + 1).limit(count)
                .map(this::data).collect(Collectors.toList()));
    }

    /**
     * Возвращает StreamObserver, который на последовательные sendRead отвечает по очереди
     * переданным ответами по одному, а потом отвечать перестаёт.
     */
    private StreamObserver<Persqueue.ReadRequest> makeResponder(Collection<Answer<?>> answers) {
        Stubber stubber = new StubberImpl(null);
        for (Answer<?> answer : answers) {
            stubber = stubber.doAnswer(answer);
        }
        stubber.doNothing().when(outboundStreamObserver).onNext(any());
        return outboundStreamObserver;
    }

    private Answer<?> data(int idx) {
        Order order = CheckouterUtils.defaultOrder(OrderStatus.DELIVERY)
                .setOrderSubstatus(DELIVERY_SERVICE_RECEIVED)
                .setDeliveryType(DeliveryType.DELIVERY)
                .setNoAuth(true)
                .addItem(defaultOrderItem().build())
                .build();

        order.setId(Long.valueOf(CheckouterEventsLogbrokerConsumerConfiguration.CONSUMER_COUNT));
        Order orderBefore = order.clone();

        order.setStatus(OrderStatus.DELIVERY);
        order.setSubstatus(DELIVERY_SERVICE_RECEIVED);

        OrderHistoryEvent event = CheckouterUtils.getEvent(
                orderBefore, order, HistoryEventType.ORDER_STATUS_UPDATED, clock
        );
        event.setId(Long.valueOf(idx));

        byte[] eventJson = new byte[0];
        checkouterAnnotationObjectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        try {
            eventJson = checkouterAnnotationObjectMapper.writeValueAsBytes(event);
        } catch (JsonProcessingException e) {
            // ignored
        }
        byte[] json = eventJson;

        return inv -> {
            inboundStreamObserver.onNext(
                    Persqueue.ReadResponse.newBuilder()
                            .setData(Persqueue.ReadResponse.Data.newBuilder()
                                    .addMessageBatch(Persqueue.ReadResponse.Data.MessageBatch.newBuilder()
                                            .setTopic(TEST_TOPIC)
                                            .addMessage(Persqueue.ReadResponse.Data.Message.newBuilder()
                                                    .setMeta(Persqueue.MessageMeta.newBuilder().setSeqNo(idx))
                                                    .setData(ByteString.copyFrom(json)))
                                    ))
                            .build()
            );
            return null;
        };
    }

    private Persqueue.ReadResponse buildInit(String sessionId) {
        return Persqueue.ReadResponse.newBuilder()
                .setInit(Persqueue.ReadResponse.Init.newBuilder().setSessionId(sessionId))
                .build();
    }

    private Persqueue.ReadResponse buildLock(int partition, long generation) {
        return Persqueue.ReadResponse.newBuilder()
                .setLock(Persqueue.ReadResponse.Lock.newBuilder()
                        .setTopic(TEST_TOPIC)
                        .setPartition(partition)
                        .setGeneration(generation))
                .build();
    }

    private Persqueue.ReadResponse buildRelease(int partition, long generation) {
        return Persqueue.ReadResponse.newBuilder()
                .setRelease(Persqueue.ReadResponse.Release.newBuilder()
                        .setTopic(TEST_TOPIC)
                        .setPartition(partition)
                        .setGeneration(generation))
                .build();
    }

    static class InProcessProxyConfig extends ProxyConfig {

        private final String serverName;

        public InProcessProxyConfig(String serverName) {
            super(null, 0, 0);
            this.serverName = serverName;
        }

        @Override
        public ManagedChannelBuilder<?> toChannelBuilder() {
            return InProcessChannelBuilder.forName(serverName);
        }
    }
}

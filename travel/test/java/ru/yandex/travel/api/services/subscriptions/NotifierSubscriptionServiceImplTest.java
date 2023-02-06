package ru.yandex.travel.api.services.subscriptions;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.grpc.Context;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.misc.ExceptionUtils;
import ru.yandex.travel.api.endpoints.subscriptions.req_rsp.SubscribeReqV1;
import ru.yandex.travel.api.endpoints.subscriptions.req_rsp.SubscriptionStatusReqV1;
import ru.yandex.travel.api.models.common.TravelVertical;
import ru.yandex.travel.api.services.notifier.NotifierClientFactory;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.grpc.AppCallIdGenerator;
import ru.yandex.travel.notifier.subscriptions.v1.GetStatusReq;
import ru.yandex.travel.notifier.subscriptions.v1.GetStatusRsp;
import ru.yandex.travel.notifier.subscriptions.v1.SubscribeReq;
import ru.yandex.travel.notifier.subscriptions.v1.SubscribeRsp;
import ru.yandex.travel.notifier.subscriptions.v1.SubscriptionsServiceGrpc;
import ru.yandex.travel.notifier.subscriptions.v1.UnsubscribeReq;
import ru.yandex.travel.notifier.subscriptions.v1.UnsubscribeRsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class NotifierSubscriptionServiceImplTest {
    @MockBean
    public NotifierClientFactory notifierClientFactory;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

    @Autowired
    private NotifierSubscriptionServiceImpl notifierSubscriptionService;

    @Before
    public void setUp() throws IOException {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();
        // Use a mutable service registry for later registering the service impl for each test case.
        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .fallbackHandlerRegistry(serviceRegistry).directExecutor().build().start());
        var stub = createServerAndFutureStub();
        when(notifierClientFactory.createRoundRobinStub()).thenReturn(stub);
    }

    @Test
    public void testSubscribe() {
        SubscribeReqV1 request = SubscribeReqV1.builder()
                .email("test@email.com")
                .source("booking_page")
                .travelVerticalName(TravelVertical.AVIA)
                .timezone("UTC")
                .nationalVersion("ru")
                .passportId("passportId")
                .yandexUid("yandexUid")
                .language("ru")
                .build();
        AtomicReference<SubscribeReq> receivedRequestRef = new AtomicReference<>();
        var service = new SubscriptionsServiceGrpc.SubscriptionsServiceImplBase() {
            @Override
            public void subscribe(SubscribeReq req, StreamObserver<SubscribeRsp> responseObserver) {
                receivedRequestRef.set(req);
                responseObserver.onNext(SubscribeRsp.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
        serviceRegistry.addService(service);

        var httpHeaders = new CommonHttpHeaders(
                Map.of(
                        CommonHttpHeaders.HeaderType.USER_IS_PLUS, "true",
                        CommonHttpHeaders.HeaderType.EXPERIMENTS, "{\"a\":\"enabled\"}"
                )
        );

        callWithAppCallIdGenerator(() -> notifierSubscriptionService.subscribe(request, httpHeaders));
        SubscribeReq receivedReq = receivedRequestRef.get();
        assertThat(receivedReq.getEmail()).isEqualTo(request.getEmail());
        assertThat(receivedReq.getIsPlusUser()).isEqualTo(true);
        assertThat(receivedReq.getExperimentsMap()).isEqualTo(Map.of("a", "enabled"));
    }

    @Test
    public void testFailedSubscribe() {
        SubscribeReqV1 request = SubscribeReqV1.builder()
                .email("test@email.com")
                .source("booking_page")
                .travelVerticalName(TravelVertical.AVIA)
                .timezone("UTC")
                .nationalVersion("ru")
                .language("ru")
                .build();

        var service = new SubscriptionsServiceGrpc.SubscriptionsServiceImplBase() {
            @Override
            public void subscribe(SubscribeReq req, StreamObserver<SubscribeRsp> responseObserver) {
                responseObserver.onError(new RuntimeException());
            }
        };
        serviceRegistry.addService(service);

        assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> notifierSubscriptionService.subscribe(request, CommonHttpHeaders.get()).get());
    }

    @Test
    public void testUnsubscribe() {
        String hash = "123";
        AtomicReference<UnsubscribeReq> receivedRequestRef = new AtomicReference<>();
        var service = new SubscriptionsServiceGrpc.SubscriptionsServiceImplBase() {
            @Override
            public void unsubscribe(UnsubscribeReq request, StreamObserver<UnsubscribeRsp> responseObserver) {
                receivedRequestRef.set(request);
                responseObserver.onNext(UnsubscribeRsp.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
        serviceRegistry.addService(service);

        callWithAppCallIdGenerator(() -> notifierSubscriptionService.unsubscribe(hash));
        UnsubscribeReq receivedReq = receivedRequestRef.get();
        assertThat(receivedReq.getHash()).isEqualTo(hash);
    }

    @Test
    public void testFailedUnsubscribe() {
        String hash = "123";
        var service = new SubscriptionsServiceGrpc.SubscriptionsServiceImplBase() {
            @Override
            public void unsubscribe(UnsubscribeReq request, StreamObserver<UnsubscribeRsp> responseObserver) {
                responseObserver.onError(new RuntimeException());
            }
        };
        serviceRegistry.addService(service);

        assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> notifierSubscriptionService.unsubscribe(hash).get());
    }

    @Test
    public void testGetStatus() throws ExecutionException, InterruptedException {
        SubscriptionStatusReqV1 request = SubscriptionStatusReqV1.builder().email("test@email.com").build();
        AtomicReference<GetStatusReq> receivedRequestRef = new AtomicReference<>();
        var returnedStatus = GetStatusRsp.newBuilder()
                .setIsSubscribed(true)
                .setVertical("vertical")
                .setSource("source")
                .setSubscribedAt("2021-01-01T00:00:00.000Z")
                .build();
        var service = new SubscriptionsServiceGrpc.SubscriptionsServiceImplBase() {
            @Override
            public void getStatus(GetStatusReq request, StreamObserver<GetStatusRsp> responseObserver) {
                receivedRequestRef.set(request);
                responseObserver.onNext(returnedStatus);
                responseObserver.onCompleted();
            }
        };
        serviceRegistry.addService(service);

        var response = callWithAppCallIdGenerator(() -> notifierSubscriptionService.getStatus(request)).get();
        GetStatusReq receivedReq = receivedRequestRef.get();
        assertThat(receivedReq.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getIsSubscribed()).isEqualTo(returnedStatus.getIsSubscribed());
        assertThat(response.getVertical()).isEqualTo(returnedStatus.getVertical());
        assertThat(response.getSource()).isEqualTo(returnedStatus.getSource());
        assertThat(response.getSubscribedAt()).isEqualTo(returnedStatus.getSubscribedAt());
    }


    @Test
    public void testFailedGetStatus() {
        SubscriptionStatusReqV1 request = SubscriptionStatusReqV1.builder().email("test@email.com").build();
        var service = new SubscriptionsServiceGrpc.SubscriptionsServiceImplBase() {
            @Override
            public void getStatus(GetStatusReq request, StreamObserver<GetStatusRsp> responseObserver) {
                responseObserver.onError(new RuntimeException());
            }
        };
        serviceRegistry.addService(service);

        assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> notifierSubscriptionService.getStatus(request).get());
    }

    private SubscriptionsServiceGrpc.SubscriptionsServiceFutureStub createServerAndFutureStub() {
        try {

            String serverName = InProcessServerBuilder.generateName();

            grpcCleanup.register(
                    InProcessServerBuilder.forName(serverName)
                            .fallbackHandlerRegistry(serviceRegistry)
                            .directExecutor()
                            .build()
                            .start()
            );

            InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName);
            return SubscriptionsServiceGrpc.newFutureStub(channelBuilder.build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <R> R callWithAppCallIdGenerator(Supplier<R> func) {
        try {
            return Context.current().withValue(AppCallIdGenerator.KEY, AppCallIdGenerator.newInstance()).call(
                    func::get
            );
        } catch (Exception e) {
            throw ExceptionUtils.throwException(e);
        }
    }
}

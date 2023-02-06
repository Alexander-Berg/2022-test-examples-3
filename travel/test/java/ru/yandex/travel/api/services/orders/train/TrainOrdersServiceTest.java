package ru.yandex.travel.api.services.orders.train;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.grpc.Context;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.misc.ExceptionUtils;
import ru.yandex.travel.api.endpoints.trains_booking_flow.req_rsp.CreateOrderReqV2;
import ru.yandex.travel.api.endpoints.trains_booking_flow.req_rsp.CreateOrderRspV1;
import ru.yandex.travel.api.endpoints.trains_booking_flow.req_rsp.OrderInfoRspV1;
import ru.yandex.travel.api.endpoints.trains_booking_flow.req_rsp.OrderStatusRspV1;
import ru.yandex.travel.api.models.train.TrainOrderStatus;
import ru.yandex.travel.api.services.dictionaries.country.CountryDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.readable_timezone.TrainReadableTimezoneDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.settlement.TrainSettlementDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.station.TrainStationDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.station_code.TrainStationCodeDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.station_express_alias.TrainStationExpressAliasDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.time_zone.TrainTimeZoneDataProvider;
import ru.yandex.travel.api.services.orders.OrchestratorClientFactory;
import ru.yandex.travel.api.services.orders.TrainOrdersService;
import ru.yandex.travel.api.services.train.TrainOfferService;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.grpc.AppCallIdGenerator;
import ru.yandex.travel.orders.commons.proto.EOrderType;
import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.proto.OrderInterfaceV1Grpc;
import ru.yandex.travel.orders.proto.TCreateOrderReq;
import ru.yandex.travel.orders.proto.TCreateOrderRsp;
import ru.yandex.travel.orders.proto.TGetOrderInfoReq;
import ru.yandex.travel.orders.proto.TGetOrderInfoRsp;
import ru.yandex.travel.orders.proto.TOrderInfo;
import ru.yandex.travel.orders.proto.TOrderServiceInfo;
import ru.yandex.travel.orders.proto.TReserveReq;
import ru.yandex.travel.orders.proto.TReserveRsp;
import ru.yandex.travel.orders.proto.TServiceInfo;
import ru.yandex.travel.orders.workflow.order.generic.proto.EOrderState;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.train.model.ErrorCode;
import ru.yandex.travel.train.model.ErrorInfo;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.trains.proto.TTrainServiceOffer;
import ru.yandex.travel.workflow.EWorkflowState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class TrainOrdersServiceTest {
    @MockBean
    public OrchestratorClientFactory orchestratorClientFactory;

    @MockBean
    private TrainOfferService trainOfferService;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

    @Autowired
    private TrainOrdersService trainOrdersService;

    @Before
    public void setUp() throws IOException {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();
        // Use a mutable service registry for later registering the service impl for each test case.
        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .fallbackHandlerRegistry(serviceRegistry).directExecutor().build().start());
        OrderInterfaceV1Grpc.OrderInterfaceV1FutureStub stub = createServerAndFutureStub();
        when(orchestratorClientFactory.createFutureStubForTrains()).thenReturn(stub);
        when(orchestratorClientFactory.createFutureStubForTrainsReadOnly()).thenReturn(stub);
    }

    @Test
    @Ignore
    public void testCreateOrderV2() throws ExecutionException, InterruptedException {
        String orderId = UUID.randomUUID().toString();
        CreateOrderReqV2 request = TrainTestHelpers.createOrderRequestV2();
        TTrainServiceOffer offer = TrainTestHelpers.createOffer();
        when(trainOfferService.get(eq(request.getOfferId()))).thenReturn(CompletableFuture.completedFuture(offer));
        AtomicReference<TCreateOrderReq> receivedRequestRef = new AtomicReference<>();
        AtomicBoolean reserveCalled = new AtomicBoolean(false);
        OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase service = new OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase() {
            @Override
            public void createOrder(TCreateOrderReq request, StreamObserver<TCreateOrderRsp> responseObserver) {
                receivedRequestRef.set(request);
                responseObserver.onNext(TCreateOrderRsp.newBuilder()
                        .setNewOrder(TOrderInfo.newBuilder().setOrderId(orderId))
                        .build());
                responseObserver.onCompleted();
            }

            @Override
            public void reserve(TReserveReq request, StreamObserver<TReserveRsp> responseObserver) {
                reserveCalled.set(true);
                responseObserver.onNext(TReserveRsp.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
        serviceRegistry.addService(service);

        CreateOrderRspV1 response = callWithAppCallIdGenerator(() -> trainOrdersService.createOrder(request)).get();
        TCreateOrderReq receivedReq = receivedRequestRef.get();
        assertThat(receivedReq.getDeduplicationKey()).isEqualTo(request.getDeduplicationKey());
        assertThat(reserveCalled).isTrue();
        assertThat(response.getId()).isEqualTo(orderId);
    }

    @Test
    public void testFailedCreate() {
        CreateOrderReqV2 request = TrainTestHelpers.createOrderRequestV2();
        TTrainServiceOffer offer = TrainTestHelpers.createOffer();
        when(trainOfferService.get(eq(request.getOfferId()))).thenReturn(CompletableFuture.completedFuture(offer));

        AtomicBoolean reserveCalled = new AtomicBoolean(false);
        OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase service = new OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase() {
            @Override
            public void createOrder(TCreateOrderReq request, StreamObserver<TCreateOrderRsp> responseObserver) {
                responseObserver.onError(new RuntimeException());
            }

            @Override
            public void reserve(TReserveReq request, StreamObserver<TReserveRsp> responseObserver) {
                reserveCalled.set(true);
                responseObserver.onNext(TReserveRsp.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
        serviceRegistry.addService(service);

        assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> trainOrdersService.createOrder(request).get());
        assertThat(reserveCalled).isFalse();
    }

    @Test
    public void testGetOrderStatus() throws ExecutionException, InterruptedException {
        String orderId = UUID.randomUUID().toString();
        var reservedTo = LocalDateTime.of(2019, 8, 7, 11, 40).toInstant(ZoneOffset.UTC);
        var pendingTill = LocalDateTime.of(2019, 8, 7, 11, 30).toInstant(ZoneOffset.UTC);
        OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase service = new OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase() {
            @Override
            public void getOrderInfo(TGetOrderInfoReq request, StreamObserver<TGetOrderInfoRsp> responseObserver) {
                TrainReservation payload = new TrainReservation();
                payload.setMaxPendingTill(pendingTill);
                payload.setErrorInfo(new ErrorInfo());
                payload.getErrorInfo().setCode(ErrorCode.UNKNOWN_PARTNER_ERROR);
                responseObserver.onNext(TGetOrderInfoRsp.newBuilder()
                        .setResult(TOrderInfo.newBuilder()
                                .setOrderType(EOrderType.OT_GENERIC)
                                .setExpiresAt(ProtoUtils.fromInstant(reservedTo))
                                .setOrderId(request.getOrderId())
                                .setWorkflowState(EWorkflowState.WS_RUNNING)
                                .setGenericOrderState(EOrderState.OS_WAITING_CONFIRMATION)
                                .addService(testService(payload, EOrderItemState.IS_CONFIRMING))
                                .build())
                        .build());
                responseObserver.onCompleted();
            }
        };
        serviceRegistry.addService(service);

        OrderStatusRspV1 response = callWithAppCallIdGenerator(() -> trainOrdersService.getOrderStatus(orderId)).get();
        assertThat(response.getStatus()).isEqualTo(TrainOrderStatus.WAITING_CONFIRMATION);
        assertThat(response.getReservedTo()).isEqualTo(reservedTo);
        assertThat(response.getMaxPendingTill()).isEqualTo(pendingTill);
    }

    @Test
    public void testGetOrderInfo() throws ExecutionException, InterruptedException {
        String orderId = UUID.randomUUID().toString();
        var reservedTo = LocalDateTime.of(2019, 8, 7, 11, 40).toInstant(ZoneOffset.UTC);
        var pendingTill = LocalDateTime.of(2019, 8, 7, 11, 30).toInstant(ZoneOffset.UTC);
        OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase service = new OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase() {
            @Override
            public void getOrderInfo(TGetOrderInfoReq request, StreamObserver<TGetOrderInfoRsp> responseObserver) {
                TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMING);
                payload.setMaxPendingTill(pendingTill);
                responseObserver.onNext(TGetOrderInfoRsp.newBuilder()
                        .setResult(TOrderInfo.newBuilder()
                                .setOrderType(EOrderType.OT_GENERIC)
                                .setExpiresAt(ProtoUtils.fromInstant(reservedTo))
                                .setOrderId(request.getOrderId())
                                .setWorkflowState(EWorkflowState.WS_RUNNING)
                                .setGenericOrderState(EOrderState.OS_WAITING_CONFIRMATION)
                                .addService(testService(payload, EOrderItemState.IS_CONFIRMING))
                                .build())
                        .build());
                responseObserver.onCompleted();
            }
        };
        serviceRegistry.addService(service);

        OrderInfoRspV1 response =
                callWithAppCallIdGenerator(() -> trainOrdersService.getOrderInfo(orderId, false)).get();
        assertThat(response.getStatus()).isEqualTo(TrainOrderStatus.WAITING_CONFIRMATION);
        assertThat(response.getReservedTo()).isEqualTo(reservedTo);
        assertThat(response.getMaxPendingTill()).isEqualTo(pendingTill);
        assertThat(response.getPassengers().size()).isEqualTo(1);
        assertThat(response.getPassengers().get(0).getTickets().size()).isEqualTo(1);
        assertThat(response.getPassengers().get(0).getTickets().get(0).getRzhdStatus()).isNull();
        assertThat(response.getPassengers().get(0).getTickets().get(0).getBookedTariffCode()).isNull();
        assertThat(response.getPassengers().get(0).getTickets().get(0).getTariffInfo()).isNull();
        assertThat(response.getPassengers().get(0).getTickets().get(0).isDiscountDenied()).isTrue();
        assertThat(response.getTrainInfo().getStartStationTitle()).isEqualTo("Москва");
    }

    @Test
    public void testGetOrderInfoNewState() throws ExecutionException, InterruptedException {
        String orderId = UUID.randomUUID().toString();
        OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase service = new OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase() {
            @Override
            public void getOrderInfo(TGetOrderInfoReq request, StreamObserver<TGetOrderInfoRsp> responseObserver) {
                TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_NEW);
                responseObserver.onNext(TGetOrderInfoRsp.newBuilder()
                        .setResult(TOrderInfo.newBuilder()
                                .setOrderType(EOrderType.OT_GENERIC)
                                .setOrderId(request.getOrderId())
                                .setWorkflowState(EWorkflowState.WS_RUNNING)
                                .setGenericOrderState(EOrderState.OS_NEW)
                                .addService(testService(payload, EOrderItemState.IS_NEW))
                                .build())
                        .build());
                responseObserver.onCompleted();
            }
        };
        serviceRegistry.addService(service);

        OrderInfoRspV1 response =
                callWithAppCallIdGenerator(() -> trainOrdersService.getOrderInfo(orderId, false)).get();
        assertThat(response.getStatus()).isEqualTo(TrainOrderStatus.WAITING_RESERVATION);
        assertThat(response.getPassengers().size()).isEqualTo(1);
        assertThat(response.getPassengers().get(0).getTickets().size()).isEqualTo(0);
    }


    private OrderInterfaceV1Grpc.OrderInterfaceV1FutureStub createServerAndFutureStub() {
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
            return OrderInterfaceV1Grpc.newFutureStub(channelBuilder.build());
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

    private TOrderServiceInfo testService(TrainReservation payload, EOrderItemState state) {
        return TOrderServiceInfo.newBuilder()
                .setServiceType(EServiceType.PT_TRAIN)
                .setServiceInfo(TServiceInfo.newBuilder()
                        .setPayload(ProtoUtils.toTJson(payload))
                        .setGenericOrderItemState(state)
                        .build())
                .build();
    }

    @TestConfiguration
    static class TrainDictTestConfiguration {
        @Bean
        @Primary
        public CountryDataProvider countryDataProvider() {
            return MockTrainDictionaryHelper.countryDataProvider();
        }

        @Bean
        @Primary
        public TrainReadableTimezoneDataProvider trainReadableTimezoneDataProvider() {
            return MockTrainDictionaryHelper.trainReadableTimezoneDataProvider();
        }

        @Bean
        @Primary
        public TrainSettlementDataProvider trainSettlementDataProvider() {
            return MockTrainDictionaryHelper.trainSettlementDataProvider();
        }

        @Bean
        @Primary
        public TrainStationDataProvider trainStationDataProvider() {
            return MockTrainDictionaryHelper.trainStationDataProvider();
        }

        @Bean
        @Primary
        public TrainStationCodeDataProvider trainStationCodeDataProvider() {
            return MockTrainDictionaryHelper.trainStationCodeDataProvider();
        }

        @Bean
        @Primary
        public TrainStationExpressAliasDataProvider trainStationExpressAliasDataProvider() {
            return MockTrainDictionaryHelper.trainStationExpressAliasDataProvider();
        }

        @Bean
        @Primary
        public TrainTimeZoneDataProvider trainTimeZoneDictionary() {
            return MockTrainDictionaryHelper.trainTimeZoneDataProvider();
        }
    }
}

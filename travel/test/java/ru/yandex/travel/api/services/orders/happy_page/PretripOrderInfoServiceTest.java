package ru.yandex.travel.api.services.orders.happy_page;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

import ru.yandex.travel.api.endpoints.booking_flow.DtoMapper;
import ru.yandex.travel.api.endpoints.booking_flow.model.ConfirmationInfo;
import ru.yandex.travel.api.endpoints.booking_flow.model.GeoHotelInfo;
import ru.yandex.travel.api.endpoints.booking_flow.model.HotelOrderDto;
import ru.yandex.travel.api.endpoints.booking_flow.model.PartnerHotelInfoDto;
import ru.yandex.travel.api.endpoints.booking_flow.model.RequestInfo;
import ru.yandex.travel.api.endpoints.generic_booking_flow.req_rsp.GetGenericOrderRspV1;
import ru.yandex.travel.api.models.train.Passenger;
import ru.yandex.travel.api.models.train.PlaceWithType;
import ru.yandex.travel.api.models.train.Station;
import ru.yandex.travel.api.models.train.Ticket;
import ru.yandex.travel.api.models.train.TrainInfo;
import ru.yandex.travel.api.services.hotels_booking_flow.HotelOrdersService;
import ru.yandex.travel.api.services.hotels_booking_flow.models.HotelOrder;
import ru.yandex.travel.api.services.orders.GenericModelMapService;
import ru.yandex.travel.api.services.orders.OrchestratorClientFactory;
import ru.yandex.travel.api.services.orders.TrainDictionaryMapService;
import ru.yandex.travel.api.services.orders.happy_page.model.HotelHappyPageOrder;
import ru.yandex.travel.api.services.orders.happy_page.model.HotelOrderInfo;
import ru.yandex.travel.api.services.orders.happy_page.model.TrainHappyPageOrder;
import ru.yandex.travel.api.services.orders.notifier.model.OrderInfoPayload;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.commons.proto.EOrderType;
import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.proto.OrderNoAuthInterfaceV1Grpc;
import ru.yandex.travel.orders.proto.TGetOrderInfoReq;
import ru.yandex.travel.orders.proto.TGetOrderInfoRsp;
import ru.yandex.travel.orders.proto.TOrderInfo;
import ru.yandex.travel.orders.proto.TOrderServiceInfo;
import ru.yandex.travel.orders.proto.TServiceInfo;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.train.model.TrainReservationRequestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class PretripOrderInfoServiceTest {
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();
    @MockBean
    private DtoMapper dtoMapper;
    @MockBean
    private HappyPageMapper mapper;
    @MockBean
    private GenericModelMapService genericModelMapService;
    @MockBean
    private OrchestratorClientFactory orchestratorClientFactory;
    @MockBean
    private HotelOrdersService hotelOrdersService;
    @MockBean
    private TrainDictionaryMapService trainDictionaryMapService;
    @Autowired
    private PretripOrderInfoService pretripOrderInfoService;

    @Before
    public void setUp() throws IOException {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();
        // Use a mutable service registry for later registering the service impl for each test case.
        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .fallbackHandlerRegistry(serviceRegistry).directExecutor().build().start());
        var stub = createServerAndFutureStub();
        when(orchestratorClientFactory.createOrderNoAuthFutureStub()).thenReturn(stub);
    }

    @Test
    public void testHotelOrder() throws InterruptedException, ExecutionException, TimeoutException {
        var service = new OrderNoAuthInterfaceV1Grpc.OrderNoAuthInterfaceV1ImplBase() {
            @Override
            public void getOrderInfo(TGetOrderInfoReq request, StreamObserver<TGetOrderInfoRsp> responseObserver) {
                var result = TGetOrderInfoRsp.newBuilder()
                        .setResult(TOrderInfo.newBuilder()
                                .setOrderId(request.getOrderId())
                                .setDisplayOrderType(EDisplayOrderType.DT_HOTEL))
                        .build();
                responseObserver.onNext(result);
                responseObserver.onCompleted();
            }
        };
        var uuid = UUID.randomUUID();
        serviceRegistry.addService(service);
        var checkIn = new PartnerHotelInfoDto.Checkin();
        checkIn.setBeginTime("check-in start");
        checkIn.setEndTime("check-in end");
        var checkOut = new PartnerHotelInfoDto.Checkout();
        checkOut.setTime("check-out time");
        when(hotelOrdersService.getOrderFromProto(any())).thenReturn(HotelOrder.builder().id(uuid).build());
        when(dtoMapper.buildOrderDto(any(), anyBoolean())).thenReturn(new HotelOrderDto());
        when(mapper.mapHotelOrder(any())).thenReturn(
                HotelHappyPageOrder.builder()
                        .id(uuid)
                        .yandexOrderId("pretty")
                        .orderInfo(
                                HotelOrderInfo.builder()
                                        .basicHotelInfo(
                                                GeoHotelInfo.builder()
                                                        .name("name")
                                                        .address("address")
                                                        .phone("phone")
                                                        .imageUrlTemplate("image")
                                                        .build())
                                        .partnerHotelInfo(
                                                PartnerHotelInfoDto.builder()
                                                        .checkin(checkIn)
                                                        .checkout(checkOut)
                                                        .build())
                                        .requestInfo(
                                                RequestInfo.builder()
                                                        .checkinDate(LocalDate.of(2021, 3, 12))
                                                        .checkoutDate(LocalDate.of(2021, 3, 14))
                                                        .build())
                                        .build())
                        .confirmationInfo(
                                ConfirmationInfo.builder()
                                        .documentUrl("voucher url")
                                        .build())
                        .build());
        CompletableFuture<OrderInfoPayload> response = pretripOrderInfoService.getOrderInfo(uuid);
        var result = response.get(10, TimeUnit.SECONDS);
        assertThat(result.getHotelOrderInfos()).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(uuid);
        assertThat(result.getPrettyId()).isEqualTo("pretty");
        assertThat(result.getHotelOrderInfos().size()).isEqualTo(1);

        var expected = new OrderInfoPayload.HotelOrderInfo();
        expected.setName("name");
        expected.setAddress("address");
        expected.setPhone("phone");
        expected.setCheckInBeginTime("check-in start");
        expected.setCheckInEndTime("check-in end");
        expected.setCheckOutTime("check-out time");
        expected.setDocumentUrl("voucher url");
        expected.setImageUrlTemplate("image");
        expected.setCheckInDate(LocalDate.of(2021, 3, 12));
        expected.setCheckOutDate(LocalDate.of(2021, 3, 14));
        assertThat(result.getHotelOrderInfos().get(0)).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testTrainOrder() throws InterruptedException, ExecutionException, TimeoutException {
        when(trainDictionaryMapService.createStationInfoByCode(any())).thenReturn(Station.builder().build());
        var service = new OrderNoAuthInterfaceV1Grpc.OrderNoAuthInterfaceV1ImplBase() {
            @Override
            public void getOrderInfo(TGetOrderInfoReq request, StreamObserver<TGetOrderInfoRsp> responseObserver) {
                var result = TGetOrderInfoRsp.newBuilder()
                        .setResult(TOrderInfo.newBuilder()
                                .setOrderId(request.getOrderId())
                                .setOrderType(EOrderType.OT_GENERIC)
                                .addService(TOrderServiceInfo.newBuilder()
                                        .setServiceType(EServiceType.PT_TRAIN)
                                        .setServiceInfo(TServiceInfo.newBuilder()
                                                .setPayload(ProtoUtils.toTJson(TrainReservation.builder()
                                                        .passengers(List.of(TrainPassenger.builder()
                                                                .birthday(LocalDate.of(2000, 2, 2))
                                                                .build()))
                                                        .reservationRequestData(new TrainReservationRequestData())
                                                        .departureTime(Instant.parse("2019-01-01T01:01:01Z"))
                                                        .build()))
                                                .build())
                                        .build())
                                .setDisplayOrderType(EDisplayOrderType.DT_TRAIN))
                        .build();
                responseObserver.onNext(result);
                responseObserver.onCompleted();
            }
        };
        var uuid = UUID.randomUUID();
        serviceRegistry.addService(service);
        when(mapper.mapTrainModel(any())).thenReturn(
                TrainHappyPageOrder.builder()
                        .id(uuid)
                        .prettyId("pretty")
                        .stationFrom(Station.builder().id(2000003).build())
                        .stationTo(Station.builder().id(9602494).build())
                        .trainInfo(TrainInfo.builder()
                                .startSettlementTitle("start")
                                .endSettlementTitle("end")
                                .brandTitle("trans-world")
                                .trainNumber("014")
                                .build())
                        .departure(Instant.parse("2021-02-16T21:00:00Z"))
                        .arrival(Instant.parse("2021-02-17T21:00:00Z"))
                        .carNumber("04")
                        .carType("platzkarte")
                        .compartmentGender("test")
                        .passenger(createPassenger("020"))
                        .passenger(createPassenger("010"))
                        .build());
        when(genericModelMapService.getOrderFromInfo(any())).thenReturn(new GetGenericOrderRspV1());
        CompletableFuture<OrderInfoPayload> response = pretripOrderInfoService.getOrderInfo(uuid);
        var result = response.get(10, TimeUnit.SECONDS);
        assertThat(result.getTrainOrderInfos()).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(uuid);
        assertThat(result.getPrettyId()).isEqualTo("pretty");
        assertThat(result.getTrainOrderInfos().size()).isEqualTo(1);

        var expected = new OrderInfoPayload.TrainOrderInfo();
        expected.setStationFromId("2000003");
        expected.setStationToId("9602494");
        expected.setTrainStartSettlementTitle("start");
        expected.setTrainEndSettlementTitle("end");
        expected.setDeparture(Instant.parse("2021-02-16T21:00:00Z"));
        expected.setArrival(Instant.parse("2021-02-17T21:00:00Z"));
        expected.setTrainNumber("014");
        expected.setBrandTitle("trans-world");
        expected.setCarNumber("04");
        expected.setCarType("platzkarte");
        expected.setCompartmentGender("test");
        expected.setPlaceNumbers(List.of("010", "020"));
        assertThat(result.getTrainOrderInfos().get(0)).isEqualToComparingFieldByField(expected);
    }

    private Passenger createPassenger(String placeNumber) {
        var result = new Passenger();
        var ticket = new Ticket();
        var place = new PlaceWithType();
        place.setNumber(placeNumber);
        ticket.setPlaces(List.of(place));
        result.setTickets(List.of(ticket));
        return result;
    }

    private OrderNoAuthInterfaceV1Grpc.OrderNoAuthInterfaceV1FutureStub createServerAndFutureStub() {
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
            return OrderNoAuthInterfaceV1Grpc.newFutureStub(channelBuilder.build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

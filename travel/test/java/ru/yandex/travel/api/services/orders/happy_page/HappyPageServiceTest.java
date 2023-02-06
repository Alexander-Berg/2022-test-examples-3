package ru.yandex.travel.api.services.orders.happy_page;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

import ru.yandex.geobase6.LinguisticsItem;
import ru.yandex.geobase6.RegionHash;
import ru.yandex.travel.api.endpoints.booking_flow.DtoMapper;
import ru.yandex.travel.api.endpoints.booking_flow.model.HotelOrderDto;
import ru.yandex.travel.api.endpoints.travel_orders.req_rsp.OrderHappyPageRspV1;
import ru.yandex.travel.api.services.hotels.geobase.GeoBase;
import ru.yandex.travel.api.services.hotels_booking_flow.HotelOrdersService;
import ru.yandex.travel.api.services.hotels_booking_flow.models.HotelOrder;
import ru.yandex.travel.api.services.orders.OrchestratorClientFactory;
import ru.yandex.travel.api.services.orders.OrderType;
import ru.yandex.travel.api.services.orders.happy_page.model.HotelHappyPageOrder;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.proto.OrderInterfaceV1Grpc;
import ru.yandex.travel.orders.proto.TGetOrderInfoReq;
import ru.yandex.travel.orders.proto.TGetOrderInfoRsp;
import ru.yandex.travel.orders.proto.TOrderInfo;
import ru.yandex.travel.testing.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class HappyPageServiceTest {
    private static final UUID TEST_ID = UUID.fromString("0-0-0-0-123");

    @MockBean
    private DtoMapper dtoMapper;
    @MockBean
    private HappyPageMapper mapper;
    @MockBean
    private OrchestratorClientFactory orchestratorClientFactory;
    @MockBean
    private HotelOrdersService hotelOrdersService;
    @MockBean
    private GeoBase geoBase;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

    @Autowired
    private HappyPageService happyPageService;

    @Before
    public void setUp() throws IOException {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();
        // Use a mutable service registry for later registering the service impl for each test case.
        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .fallbackHandlerRegistry(serviceRegistry).directExecutor().build().start());
        OrderInterfaceV1Grpc.OrderInterfaceV1FutureStub stub = createServerAndFutureStub();
        when(orchestratorClientFactory.createFutureStubForHappyPage()).thenReturn(stub);
    }

    @Test
    public void testSuccessfulCompletion() {
        OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase service = new OrderInterfaceV1Grpc.OrderInterfaceV1ImplBase() {
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
        serviceRegistry.addService(service);
        when(hotelOrdersService.getOrderFromProto(any())).thenReturn(HotelOrder.builder().id(TEST_ID).build());
        when(dtoMapper.buildOrderDto(any(), anyBoolean())).thenReturn(new HotelOrderDto());
        when(mapper.mapHotelOrder(any())).thenReturn(HotelHappyPageOrder.builder().id(TEST_ID).build());
        RegionHash regionHash = new RegionHash();
        regionHash.putInteger("type", 54);
        regionHash.putDouble("latitude", 56.838011);
        regionHash.putDouble("longitude", 60.597465);
        when(geoBase.getRegionById(anyInt(), any())).thenReturn(regionHash);
        when(geoBase.getLinguistics(anyInt(), any()))
                .thenReturn(new LinguisticsItem("n", "g", "d",
                        "p", "to", "l", "",
                        "ab", "ac", "in"));
        CompletableFuture<OrderHappyPageRspV1> response =
                happyPageService.getOrderHappyPage(UUID.randomUUID(), "", CommonHttpHeaders.get(),
                        UserCredentials.get());
        TestUtils.waitForState("Happy page info fetched", Duration.ofSeconds(1), Duration.ofMillis(200),
                () -> response.isDone() && !response.isCompletedExceptionally());
        var result = response.join();
        assertThat(result.getOrder()).isNotNull();
        assertThat(result.getOrder()).isInstanceOf(HotelHappyPageOrder.class);
        assertThat(((HotelHappyPageOrder) result.getOrder()).getId()).isEqualTo(TEST_ID);
        assertThat(result.getOrderType()).isEqualByComparingTo(OrderType.HOTEL);
        assertThat(result.getCrossSale().getBlocks().size()).isEqualTo(2);
    }

    //
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
}

//package ru.yandex.market.sc.core.domain.place;
//
//import java.time.Clock;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//import lombok.RequiredArgsConstructor;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.boot.test.mock.mockito.SpyBean;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceRepository;
//import ru.yandex.market.sc.core.domain.order.AcceptService;
//import ru.yandex.market.sc.core.domain.order.OrderCommandService;
//import ru.yandex.market.sc.core.domain.order.OrderLockRepository;
//import ru.yandex.market.sc.core.domain.order.SortService;
//import ru.yandex.market.sc.core.domain.order.model.FFApiOrderUpdateRequest;
//import ru.yandex.market.sc.core.domain.order.model.OrderCreateRequest;
//import ru.yandex.market.sc.core.domain.order.model.OrderIdResponse;
//import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
//import ru.yandex.market.sc.core.domain.place.misc.PlaceHistoryTestHelper;
//import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
//import ru.yandex.market.sc.core.domain.postponed.repository.PostponedOperationRepository;
//import ru.yandex.market.sc.core.domain.sortable.repository.SortableBarcodeRepository;
//import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
//import ru.yandex.market.sc.core.domain.user.repository.User;
//import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
//import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseRepository;
//import ru.yandex.market.sc.core.external.ff.PushOrdersStatusesService;
//import ru.yandex.market.sc.core.test.EmbeddedDbTest;
//import ru.yandex.market.sc.core.test.TestFactory;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static ru.yandex.market.sc.core.test.TestFactory.ffOrder;
//
//@EmbeddedDbTest
//@RequiredArgsConstructor(onConstructor = @__(@Autowired))
//class FFApiPlaceServiceTest {
//
//    // TODO: Важно, сейчас проверкой правильного назначения routeSo занимается SortableFlowSwitcherExtension
//
//    private final OrderCommandService orderCommandService;
//    private final PlaceCommandService placeCommandService;
//    private final FFApiPlaceService ffApiPlaceService;
//    private final AcceptService acceptService;
//    private final SortService sortService;
//    private final ScOrderRepository orderRepository;
//    private final PlaceRepository placeRepository;
//    private final TestFactory testFactory;
//    private final TransactionTemplate transactionTemplate;
//    private final UserRepository userRepository;
//    private final WarehouseRepository warehouseRepository;
//    private final DeliveryServiceRepository deliveryServiceRepository;
//    private final PostponedOperationRepository postponedOperationRepository;
//    private final SortableBarcodeRepository sortableBarcodeRepository;
//    private final JdbcTemplate jdbcTemplate;
//    private final PlaceHistoryTestHelper placeHistoryHelper;
//
//    @MockBean
//    Clock clock;
//
//    @SpyBean
//    OrderLockRepository orderLockRepository;
//
//    @SpyBean
//    PushOrdersStatusesService pushOrdersStatusesService;
//
//    SortingCenter sortingCenter;
//    User user;
//
//
//    @BeforeEach
//    void init() {
//        sortingCenter = testFactory.storedSortingCenter(1234L);
//        user = testFactory.storedUser(sortingCenter, 123L);
//        testFactory.setupMockClock(clock);
//        placeHistoryHelper.startPlaceHistoryCollection();
//    }
//
//    @Nested
//    class UpdatePlacesTests {
//        @Test
//        void updateShipmentDateTime() {
//            var orderRequest = ffOrder(sortingCenter.getToken());
//            testFactory.storedDeliveryService(orderRequest.getDelivery().getDeliveryId().getYandexId(), true);
//            OrderIdResponse id = orderCommandService.createOrder(new OrderCreateRequest(sortingCenter, orderRequest), user);
//            assertThat(orderRepository.findByIdOrThrow(id.getId()).getShipmentDate()).isNull();
//            assertThat(orderRepository.findByIdOrThrow(id.getId()).getShipmentDateTime()).isNull();
//            placeHistoryHelper.startPlaceHistoryCollection();
//
//            FFApiOrderUpdateRequest request = new FFApiOrderUpdateRequest(sortingCenter, ffOrder(
//                    "ff_create_order_shipment_date_updated.xml",
//                    sortingCenter.getToken()));
//            //Обновляем заказы, чтобы сверка полей заказа и коробки не упала
//            orderCommandService.updateShipmentDateTime(request, user);
//            //нужно так же обновить коробки, чтобы протестировать новый поток
//            ffApiPlaceService.updatePlaceRoutes(request, user);
//
//            placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
//            var order = orderRepository.findByIdOrThrow(id.getId());
//            assertThat(order.getShipmentDate()).isEqualTo(LocalDate.parse("2012-12-22"));
//            assertThat(order.getShipmentDateTime()).isEqualTo(LocalDateTime.parse("2012-12-22T23:59:59"));
//            assertThat(order.getIncomingRouteDate()).isEqualTo(LocalDate.parse("2012-12-22"));
//            assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.parse("2012-12-22"));
//        }
//    }
//
//}
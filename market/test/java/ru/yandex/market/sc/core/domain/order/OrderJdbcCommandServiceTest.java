package ru.yandex.market.sc.core.domain.order;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.logistic_point.model.TargetLogisticPoint;
import ru.yandex.market.sc.core.domain.order.jdbc.TargetLogisticPointJdbcRepository;
import ru.yandex.market.sc.core.domain.order.model.BatchUpdateRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderUpdateHistoryItem;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderUpdateHistoryRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.model.order.registry.BatchRegistryDto;
import ru.yandex.market.sc.internal.model.order.registry.BoxesBatchRegistryDto;
import ru.yandex.market.sc.internal.model.order.registry.LogisticPointsBatchRegistryDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderUpdateEvent.UPDATE_COURIER_AND_DATE;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
class OrderJdbcCommandServiceTest {

    public static final long COURIER_ID = 11L;
    public static final String BATCH_REGISTRY_ID = "tpl_123";
    public static final long BATCH_REGISTRY_LOGISTIC_POINT_ID = 1L;
    public static final LogisticPointsBatchRegistryDto LOGISTIC_POINTS_BATCH_REGISTRY_DTO =
            new LogisticPointsBatchRegistryDto(BATCH_REGISTRY_LOGISTIC_POINT_ID, "name", "address");
    public static final String BATCH_REGISTRY_ORDER_EXTERNAL_ID_1 = "3";
    public static final String BATCH_REGISTRY_ORDER_EXTERNAL_ID_2 = "4";
    @Autowired
    OrderJdbcCommandService orderJdbcCommandService;
    @Autowired
    TestFactory testFactory;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    ScOrderUpdateHistoryRepository scOrderUpdateHistoryRepository;
    SortingCenter sortingCenter;
    User user;
    @Autowired
    Clock clock;
    @Autowired
    private TargetLogisticPointJdbcRepository targetLogisticPointJdbcRepository;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(1234L);
        user = testFactory.storedUser(sortingCenter, 123L);
    }

    @Test
    void batchUpdateOrders() {
        OrderLike order1 = testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        OrderLike order2 = testFactory.createForToday(order(sortingCenter).externalId("2").build()).get();

        var courierDto = new CourierDto(11L, "courier", null);

        var response = orderJdbcCommandService.batchUpdateOrderShipmentDateAndCourier(
                BatchUpdateRequest.builder()
                        .sortingCenter(sortingCenter)
                        .externalIds(List.of(order1.getExternalId(), order2.getExternalId()))
                        .courierDto(courierDto)
                        .newShipmentDate(LocalDate.now(clock).plusDays(1))
                        .isLastBatch(false)
                        .batchRegistries(null)
                        .build(),
                user
        );



        assertThat(response.getSuccessUpdatedOrders()).containsExactlyInAnyOrder(order1.getExternalId(),
                order2.getExternalId());
        assertThat(response.getFailUpdatedOrders()).isEmpty();

        transactionTemplate.execute(ts -> {
            var actualOrder1 = scOrderRepository.findByIdOrThrow(order1.getId());
            assertThat(actualOrder1).extracting(o -> Objects.requireNonNull(o.getCourier()).getId())
                    .isEqualTo(courierDto.getId());
            assertThat(actualOrder1).extracting(OrderLike::getShipmentDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder1).extracting(OrderLike::getOutgoingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder1).extracting(OrderLike::getIncomingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(scOrderUpdateHistoryRepository.findAllByOrder(actualOrder1))
                    .extracting(ScOrderUpdateHistoryItem::getEvent).contains(UPDATE_COURIER_AND_DATE);

            var actualOrder2 = scOrderRepository.findByIdOrThrow(order2.getId());
            assertThat(actualOrder2).extracting(o -> Objects.requireNonNull(o.getCourier()).getId())
                    .isEqualTo(courierDto.getId());
            assertThat(actualOrder2).extracting(OrderLike::getShipmentDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder2).extracting(OrderLike::getOutgoingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder2).extracting(OrderLike::getIncomingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(scOrderUpdateHistoryRepository.findAllByOrder(actualOrder2))
                    .extracting(ScOrderUpdateHistoryItem::getEvent).contains(UPDATE_COURIER_AND_DATE);

            return null;
        });
    }

    @Test
    void dontBatchUpdateForFakeCourierWhenShipped() {
        OrderLike order1 = testFactory.createForToday(order(sortingCenter).externalId("1").build()).accept().sort()
                .ship().get();
        OrderLike order2 = testFactory.createForToday(order(sortingCenter).externalId("2").build()).accept().sort()
                .ship().makeReturn().get();

        var response = orderJdbcCommandService.batchUpdateOrderShipmentDateAndCourier(
                BatchUpdateRequest.builder()
                        .sortingCenter(sortingCenter)
                        .externalIds(List.of(order1.getExternalId(), order2.getExternalId()))
                        .courierDto(testFactory.fakeCourier())
                        .newShipmentDate(LocalDate.now(clock).plusDays(1))
                        .isLastBatch(false)
                        .batchRegistries(null)
                        .build(),
                user
        );

        assertThat(response.getSuccessUpdatedOrders()).isEmpty();
        assertThat(response.getFailUpdatedOrders()).containsExactlyInAnyOrder(order1.getExternalId(),
                order2.getExternalId());

        transactionTemplate.execute(ts -> {
            var actualOrder1 = scOrderRepository.findByIdOrThrow(order1.getId());
            assertThat(actualOrder1).extracting(o -> Objects.requireNonNull(o.getCourier()).getId())
                    .isEqualTo(testFactory.defaultCourier().getId());
            assertThat(actualOrder1).extracting(OrderLike::getShipmentDate).isEqualTo(LocalDate.now(clock));
            assertThat(actualOrder1).extracting(OrderLike::getOutgoingRouteDate).isEqualTo(LocalDate.now(clock));
            assertThat(actualOrder1).extracting(OrderLike::getIncomingRouteDate).isEqualTo(LocalDate.now(clock));

            var actualOrder2 = scOrderRepository.findByIdOrThrow(order2.getId());
            assertThat(actualOrder2).extracting(o -> Objects.requireNonNull(o.getCourier()).getId())
                    .isEqualTo(testFactory.defaultCourier().getId());
            assertThat(actualOrder2).extracting(OrderLike::getShipmentDate).isEqualTo(LocalDate.now(clock));
            assertThat(actualOrder2).extracting(OrderLike::getOutgoingRouteDate).isEqualTo(LocalDate.now(clock));
            assertThat(actualOrder2).extracting(OrderLike::getIncomingRouteDate).isEqualTo(LocalDate.now(clock));
            return null;
        });
    }

    @Test
    void batchUpdateOrdersWithBatchRegistry() {
        OrderLike order1 = testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        OrderLike order2 = testFactory.createForToday(order(sortingCenter).externalId("2").build()).get();
        OrderLike order3 =
                testFactory.createForToday(order(sortingCenter).externalId(BATCH_REGISTRY_ORDER_EXTERNAL_ID_1).build()).get();
        OrderLike order4 =
                testFactory.createForToday(order(sortingCenter).externalId(BATCH_REGISTRY_ORDER_EXTERNAL_ID_2).build()).get();

        var courierDto = new CourierDto(COURIER_ID, "courier", null);

        List<BatchRegistryDto> batchRegistryDtoList = buildListBatchRegistries();
        var response = orderJdbcCommandService.batchUpdateOrderShipmentDateAndCourier(
                BatchUpdateRequest.builder()
                        .sortingCenter(sortingCenter)
                        .externalIds(List.of(
                                order1.getExternalId(), order2.getExternalId(),
                                order3.getExternalId(), order4.getExternalId()))
                        .courierDto(courierDto)
                        .newShipmentDate(LocalDate.now(clock).plusDays(1))
                        .isLastBatch(false)
                        .batchRegistries(batchRegistryDtoList)
                        .build(),
                user
        );

        assertThat(response.getSuccessUpdatedOrders()).containsExactlyInAnyOrder(
                order1.getExternalId(),
                order2.getExternalId(),
                order3.getExternalId(),
                order4.getExternalId()
        );
        assertThat(response.getFailUpdatedOrders()).isEmpty();

        transactionTemplate.execute(ts -> {
            var actualOrder1 = scOrderRepository.findByIdOrThrow(order1.getId());
            assertThat(actualOrder1).extracting(o -> Objects.requireNonNull(o.getCourier()).getId())
                    .isEqualTo(courierDto.getId());
            assertThat(actualOrder1).extracting(OrderLike::getShipmentDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder1).extracting(OrderLike::getOutgoingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder1).extracting(OrderLike::getIncomingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(scOrderUpdateHistoryRepository.findAllByOrder(actualOrder1))
                    .extracting(ScOrderUpdateHistoryItem::getEvent).contains(UPDATE_COURIER_AND_DATE);

            var actualOrder2 = scOrderRepository.findByIdOrThrow(order2.getId());
            assertThat(actualOrder2).extracting(o -> Objects.requireNonNull(o.getCourier()).getId())
                    .isEqualTo(courierDto.getId());
            assertThat(actualOrder2).extracting(OrderLike::getShipmentDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder2).extracting(OrderLike::getOutgoingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder2).extracting(OrderLike::getIncomingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(scOrderUpdateHistoryRepository.findAllByOrder(actualOrder2))
                    .extracting(ScOrderUpdateHistoryItem::getEvent).contains(UPDATE_COURIER_AND_DATE);

            var actualOrder3 = scOrderRepository.findByIdOrThrow(order3.getId());
            assertEquals(actualOrder3.getBatchRegister(), BATCH_REGISTRY_ID);
            assertEquals(actualOrder3.getExternalId(), BATCH_REGISTRY_ORDER_EXTERNAL_ID_1);

            var actualOrder4 = scOrderRepository.findByIdOrThrow(order4.getId());
            assertEquals(actualOrder4.getBatchRegister(), BATCH_REGISTRY_ID);
            assertEquals(actualOrder4.getExternalId(), BATCH_REGISTRY_ORDER_EXTERNAL_ID_2);

            return null;
        });
    }

    /**
     * Тестирует обновление информаци о заказах
     * и сохранение информации о заказах, которые будут отсортированы в мешках,
     * в то же время логистические точки доставки уже существуют и обновляются из рееста батчей.
     */
    @Test
    void batchUpdateOrdersWithBatchRegistryAndExistLogicPoint() {
        OrderLike order1 = testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();
        OrderLike order2 = testFactory.createForToday(order(sortingCenter).externalId("2").build()).get();
        OrderLike order3 =
                testFactory.createForToday(order(sortingCenter).externalId(BATCH_REGISTRY_ORDER_EXTERNAL_ID_1).build()).get();
        OrderLike order4 =
                testFactory.createForToday(order(sortingCenter).externalId(BATCH_REGISTRY_ORDER_EXTERNAL_ID_2).build()).get();
        saveTargetLogisticPoint(List.of(LOGISTIC_POINTS_BATCH_REGISTRY_DTO));
        var courierDto = new CourierDto(COURIER_ID, "courier", null);

        List<BatchRegistryDto> batchRegistryDtoList = buildListBatchRegistries();
        var response = orderJdbcCommandService.batchUpdateOrderShipmentDateAndCourier(
                BatchUpdateRequest.builder()
                        .sortingCenter(sortingCenter)
                        .externalIds(List.of(
                                order1.getExternalId(), order2.getExternalId(),
                                order3.getExternalId(), order4.getExternalId()))
                        .courierDto(courierDto)
                        .newShipmentDate(LocalDate.now(clock).plusDays(1))
                        .isLastBatch(false)
                        .batchRegistries(batchRegistryDtoList)
                        .build(),
                user
        );

        assertThat(response.getSuccessUpdatedOrders()).containsExactlyInAnyOrder(
                order1.getExternalId(),
                order2.getExternalId(),
                order3.getExternalId(),
                order4.getExternalId()
        );
        assertThat(response.getFailUpdatedOrders()).isEmpty();

        transactionTemplate.execute(ts -> {
            var actualOrder1 = scOrderRepository.findByIdOrThrow(order1.getId());
            assertThat(actualOrder1).extracting(o -> Objects.requireNonNull(o.getCourier()).getId())
                    .isEqualTo(courierDto.getId());
            assertThat(actualOrder1).extracting(OrderLike::getShipmentDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder1).extracting(OrderLike::getOutgoingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder1).extracting(OrderLike::getIncomingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(scOrderUpdateHistoryRepository.findAllByOrder(actualOrder1))
                    .extracting(ScOrderUpdateHistoryItem::getEvent).contains(UPDATE_COURIER_AND_DATE);

            var actualOrder2 = scOrderRepository.findByIdOrThrow(order2.getId());
            assertThat(actualOrder2).extracting(o -> Objects.requireNonNull(o.getCourier()).getId())
                    .isEqualTo(courierDto.getId());
            assertThat(actualOrder2).extracting(OrderLike::getShipmentDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder2).extracting(OrderLike::getOutgoingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(actualOrder2).extracting(OrderLike::getIncomingRouteDate).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(scOrderUpdateHistoryRepository.findAllByOrder(actualOrder2))
                    .extracting(ScOrderUpdateHistoryItem::getEvent).contains(UPDATE_COURIER_AND_DATE);

            var actualOrder3 = scOrderRepository.findByIdOrThrow(order3.getId());
            assertEquals(actualOrder3.getBatchRegister(), BATCH_REGISTRY_ID);
            assertEquals(actualOrder3.getExternalId(), BATCH_REGISTRY_ORDER_EXTERNAL_ID_1);

            var actualOrder4 = scOrderRepository.findByIdOrThrow(order4.getId());
            assertEquals(actualOrder4.getBatchRegister(), BATCH_REGISTRY_ID);
            assertEquals(actualOrder4.getExternalId(), BATCH_REGISTRY_ORDER_EXTERNAL_ID_2);

            return null;
        });
    }

    private void saveTargetLogisticPoint(List<LogisticPointsBatchRegistryDto> logisticPointsBatchRegistryDtos) {
        List<TargetLogisticPoint> targetLogisticPoints = logisticPointsBatchRegistryDtos.stream()
                .map(dto -> new TargetLogisticPoint(dto.getId(), dto.getName(), dto.getAddressString()))
                .toList();

        targetLogisticPointJdbcRepository.createOrUpdateTargetLogisticPoints(targetLogisticPoints);
    }

    @NotNull
    private List<BatchRegistryDto> buildListBatchRegistries() {
        return List.of(
                BatchRegistryDto.builder()
                        .id(BATCH_REGISTRY_ID)
                        .courierUid(COURIER_ID)
                        .logisticPoints(List.of(LOGISTIC_POINTS_BATCH_REGISTRY_DTO))
                        .boxes(List.of(
                                new BoxesBatchRegistryDto(
                                        BATCH_REGISTRY_ORDER_EXTERNAL_ID_1,
                                        BATCH_REGISTRY_LOGISTIC_POINT_ID
                                ),
                                new BoxesBatchRegistryDto(
                                        BATCH_REGISTRY_ORDER_EXTERNAL_ID_2,
                                        BATCH_REGISTRY_LOGISTIC_POINT_ID
                                )
                        ))
                        .build()
        );
    }
}

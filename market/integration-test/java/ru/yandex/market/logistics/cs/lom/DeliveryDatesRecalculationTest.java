package ru.yandex.market.logistics.cs.lom;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.cs.config.FeaturePropertiesConfiguration;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceCounterBatchProducer;
import ru.yandex.market.logistics.cs.domain.entity.Event;
import ru.yandex.market.logistics.cs.domain.entity.ServiceCounter;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.logbroker.checkouter.SimpleCombinatorRoute;
import ru.yandex.market.logistics.cs.repository.ServiceCounterRepository;
import ru.yandex.market.logistics.cs.service.EventService;
import ru.yandex.market.logistics.cs.service.ServiceCounterBatchPayloadService;
import ru.yandex.market.logistics.cs.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.DeliveryRoute;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.PointType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.logistics.cs.util.RouteBuilder.combinatorRoute;
import static ru.yandex.market.logistics.cs.util.RouteBuilder.item;
import static ru.yandex.market.logistics.cs.util.RouteBuilder.route;
import static ru.yandex.market.logistics.cs.util.RouteBuilder.segment;
import static ru.yandex.market.logistics.cs.util.RouteBuilder.service;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.CUTOFF;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.DELIVERY;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.HANDING;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.INBOUND;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.LAST_MILE;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.MOVEMENT;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.PROCESSING;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.SHIPMENT;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.SORT;

@DisplayName("Тестирование пересчета дат доставки")
class DeliveryDatesRecalculationTest extends AbstractLomTest {

    private static final String PARCEL_ID = "1991";
    private static final Integer ITEMS_COUNT = 50;

    @Autowired
    private ServiceCounterRepository serviceCounterRepository;

    @Autowired
    private FeaturePropertiesConfiguration features;

    @SpyBean
    private ServiceCounterBatchProducer serviceCounterBatchProducer;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ServiceCounterBatchPayloadService serviceCounterBatchPayloadService;

    @Autowired
    private EventService eventService;

    @AfterEach
    void checkEventIsProcessed() {
        Mockito.verifyNoMoreInteractions(lomClient);
        eventRepository.findAll().forEach(event -> assertTrue(event.isProcessed()));
    }

    // TODO: Because of DELIVERY-34353. Remove after a full non-experimental release
    @BeforeEach
    void makeFeaturesDefault() {
        features.setDeliveryDatesRecalculationFilterEnabled(false);
        features.setDeliveryDatesRecalculationEnabled(true);
        features.setDeliveryDatesRecalculationPartnerWhiteList(Set.of());
    }

    @DisplayName("Проверяем работу алгоритма, вычисляющего разницу в маршрутах")
    @Test
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/diff_calculation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testRouteDiffCalculation() {
        // Setup
        var oldRoute = combinatorRoute(
            segment(10)
                .partnerId(12345)
                .type(PointType.WAREHOUSE)
                .partnerType(PartnerType.DROPSHIP)
                .services(
                    service(1).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(2).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(3).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(4).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(5).code(PROCESSING).start(fixDateTime()).items(items())
                )
        );
        var newRoute = combinatorRoute(
            segment(10)
                .partnerId(12345)
                .type(PointType.WAREHOUSE)
                .partnerType(PartnerType.DROPSHIP)
                .services(
                    service(2).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(6).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(3).code(PROCESSING).start(fixDateTime()).items(item(0).quantity(75)),
                    service(7).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(8).code(PROCESSING).start(fixDateTime()).items(items())
                )
        );

        // Action
        enqueueChangeRouteEvent(oldRoute, newRoute);
    }

    @DisplayName("Проверяем работу алгоритма, вычисляющего разницу в маршрутах. Старый маршрут отсутствует")
    @Test
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/diff_calculation_without_old_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testRouteDiffCalculationButThereIsNoOldRoute() {
        // Setup
        var newRoute = combinatorRoute(
            segment(10)
                .partnerId(12345)
                .type(PointType.WAREHOUSE)
                .partnerType(PartnerType.DROPSHIP)
                .services(
                    service(1).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(2).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(3).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(4).code(PROCESSING).start(fixDateTime()).items(items()),
                    service(5).code(PROCESSING).start(fixDateTime()).items(items())
                )
        );

        // Action
        enqueueChangeRouteEvent(null, newRoute);
    }

    private void enqueueChangeRouteEvent(
        @Nullable SimpleCombinatorRoute oldRoute,
        SimpleCombinatorRoute newRoute
    ) {
        serviceCounterBatchPayloadService.enqueueChangeRouteEvent(
            Event.builder().id(1L).type(EventType.CHANGE_ROUTE).build(),
            Optional.ofNullable(oldRoute),
            newRoute
        );
    }

    @DisplayName("Пробуем пересчитать роут на отменённый заказ")
    @Test
    void testRecalculateCancelledEvent() {
        // Setup
        runInTransaction(() -> initializeEventAndServiceCounters(EventType.NEW));
        runInTransaction(() -> initializeEventAndServiceCounters(EventType.CANCELLED));
        Mockito.when(lomClient.getRouteByUuid(LOM_ROUTE_UUID)).thenReturn(routeResponse());

        // Action
        lomEventConsumer.accept(createLomEvent(PlatformClient.BERU, createDiff()));

        // Assertion
        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());

        Mockito.verify(eventService, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(serviceCounterBatchProducer);
        Mockito.verify(lomClient, Mockito.atLeastOnce()).getRouteByUuid(LOM_ROUTE_UUID);
    }

    @DisplayName("Содержимое роута не изменилось")
    @Test
    void testRouteWasNotSemanticallyChanged() {
        // Setup
        runInTransaction(() -> initializeEventAndServiceCounters(EventType.NEW));
        Mockito.when(lomClient.getRouteByUuid(LOM_ROUTE_UUID)).thenReturn(Optional.of(initialRoute()));

        // Action
        lomEventConsumer.accept(createLomEvent(PlatformClient.BERU, createDiff()));

        // Assertion
        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());

        Mockito.verifyNoInteractions(serviceCounterBatchProducer);
        Mockito.verify(lomClient).getRouteByUuid(LOM_ROUTE_UUID);
    }

    @DisplayName("Ивент CHANGE_ROUTE не создается")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("routeUuidWasNotChangedArguments")
    void testRouteUuidWasNotChanged(String caseName, @Nullable String oldRouteUuid) {
        // Setup
        runInTransaction(() -> initializeEventAndServiceCounters(EventType.NEW));

        // Action
        lomEventConsumer.accept(createLomEvent(PlatformClient.BERU, createDiff(LOM_ROUTE_UUID, oldRouteUuid)));

        // Assertion
        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());

        Mockito.verifyNoInteractions(serviceCounterBatchProducer);
    }

    @Nonnull
    private static Stream<Arguments> routeUuidWasNotChangedArguments() {
        return Stream.of(
            Arguments.of("Ивент создания заказа игнорируется", null),
            Arguments.of("Нет фактического изменения идентификатора маршрута", null)
        );
    }

    @DisplayName("Выполняем пересчет роута для существующего эвента NEW")
    @Test
    @DatabaseSetup("/repository/lom/capacity_setup.xml")
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/real_route_recalculation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/first_change_route_recalculation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testFirstChangeRouteEventRecalculation() {
        // Setup
        runInTransaction(() -> initializeEventAndServiceCounters(EventType.NEW));
        Mockito.when(lomClient.getRouteByUuid(LOM_ROUTE_UUID)).thenReturn(routeResponse());

        // Action
        lomEventConsumer.accept(createLomEvent(PlatformClient.BERU, createDiff()));

        // Assertion
        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
        Mockito.verify(lomClient).getRouteByUuid(LOM_ROUTE_UUID);
    }

    // TODO: Because of DELIVERY-34353. Remove after a full non-experimental release
    @DisplayName("Тестируем работу пустого фильтра дескрипторов для пересчёта")
    @Test
    @DatabaseSetup("/repository/lom/capacity_setup.xml")
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/real_route_recalculation_but_all_descriptors_are_filtered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/first_change_route_recalculation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testRecalculationWithEmptyServiceFilter() {
        // Setup
        features.setDeliveryDatesRecalculationFilterEnabled(true);
        runInTransaction(() -> initializeEventAndServiceCounters(EventType.NEW));
        Mockito.when(lomClient.getRouteByUuid(LOM_ROUTE_UUID)).thenReturn(routeResponse());

        // Action
        lomEventConsumer.accept(createLomEvent(PlatformClient.BERU, createDiff()));

        // Assertion
        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
        Mockito.verify(lomClient).getRouteByUuid(LOM_ROUTE_UUID);
    }

    // TODO: Because of DELIVERY-34353. Remove after a full non-experimental release
    @DisplayName("Тестируем работу фильтра дескрипторов для пересчёта")
    @Test
    @DatabaseSetup("/repository/lom/capacity_setup.xml")
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/real_route_recalculation_with_enabled_filter.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/first_change_route_recalculation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testRecalculationWithServiceFilter() {
        // Setup
        features.setDeliveryDatesRecalculationFilterEnabled(true);
        features.setDeliveryDatesRecalculationPartnerWhiteList(Set.of(200L));  // Яндекс.Маркет DS
        runInTransaction(() -> initializeEventAndServiceCounters(EventType.NEW));
        Mockito.when(lomClient.getRouteByUuid(LOM_ROUTE_UUID)).thenReturn(routeResponse());

        // Action
        lomEventConsumer.accept(createLomEvent(PlatformClient.BERU, createDiff()));

        // Assertion
        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
        Mockito.verify(lomClient).getRouteByUuid(LOM_ROUTE_UUID);
    }

    // TODO: Because of DELIVERY-34353. Remove after a full non-experimental release
    @DisplayName("Тестируем работу фильтра дескрипторов для пересчёта, но разрешенные сервисы не изменились")
    @Test
    @DatabaseSetup("/repository/lom/capacity_setup.xml")
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/real_route_recalculation_but_all_descriptors_are_filtered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/first_change_route_recalculation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testRecalculationWithFilterButChosenServicesDoNotChange() {
        // Setup
        features.setDeliveryDatesRecalculationFilterEnabled(true);
        features.setDeliveryDatesRecalculationPartnerWhiteList(Set.of(100L));  // Основной склад
        runInTransaction(() -> initializeEventAndServiceCounters(EventType.NEW));
        Mockito.when(lomClient.getRouteByUuid(LOM_ROUTE_UUID)).thenReturn(routeResponse());

        // Action
        lomEventConsumer.accept(createLomEvent(PlatformClient.BERU, createDiff()));

        // Assertion
        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
        Mockito.verify(lomClient).getRouteByUuid(LOM_ROUTE_UUID);
    }

    @DisplayName("Выполняем пересчет роута для существующего эвента CHANGE_ROUTE")
    @Test
    @DatabaseSetup("/repository/lom/capacity_setup.xml")
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/real_route_recalculation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/lom/ddr/after/second_change_route_recalculation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSecondChangeRouteEventRecalculation() {
        // Setup
        runInTransaction(() -> initializeEventAndServiceCounters(EventType.CHANGE_ROUTE));
        Mockito.when(lomClient.getRouteByUuid(LOM_ROUTE_UUID)).thenReturn(routeResponse());

        // Action
        lomEventConsumer.accept(createLomEvent(PlatformClient.BERU, createDiff()));

        // Assertion
        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
        Mockito.verify(lomClient).getRouteByUuid(LOM_ROUTE_UUID);
    }

    private CombinatorRoute initialRoute() {
        var combinatorRoute = new CombinatorRoute();
        combinatorRoute.setRoute(getRealRoute());
        return combinatorRoute;
    }

    private Optional<CombinatorRoute> routeResponse() {
        var route = new CombinatorRoute();
        route.setRoute(getRealRecalculatedRoute());
        return Optional.of(route);
    }

    @Nonnull
    private JsonNode createDiff() {
        return createDiff(LOM_ROUTE_UUID, OLD_LOM_ROUTE_UUID);
    }

    @Nonnull
    @SneakyThrows
    private JsonNode createDiff(String newRouteUuid, @Nullable String oldRouteUuid) {
        return objectMapper.readTree(String.format(
            "[{\"op\": \"replace\", \"path\": \"/routeUuid\", \"value\": \"%s\", \"fromValue\": %s}]",
            newRouteUuid,
            Optional.ofNullable(oldRouteUuid).map(s -> "\"" + s + "\"").orElse(null)
        ));
    }

    private void initializeEventAndServiceCounters(EventType eventType) {
        var event = buildEvent(eventType);
        event = eventRepository.save(event);

        var shouldCreateMap = Map.of(
            DateTimeUtils.nowDayUtc(), Set.of(1, 2, 3, 4, 5, 9),
            DateTimeUtils.nowDayUtc().plusDays(1), Set.of(6, 7, 8, 10, 11, 12, 13, 14)
        );

        // Imitate Capacity Storage main flow and
        // create required service counters for the event above
        for (var entry : shouldCreateMap.entrySet()) {
            var day = entry.getKey();
            var serviceIds = entry.getValue();
            for (int i = 1; i <= 14; ++i) {
                var shouldCreate = serviceIds.contains(i);
                if (shouldCreate) {
                    var counter = ServiceCounter.builder()
                        .serviceId((long) i)
                        .day(day)
                        .eventId(event.getId())
                        .serviceVersion(1L)
                        .itemCount(ITEMS_COUNT)
                        .realItemCount(ITEMS_COUNT)
                        .orderCount(1)
                        .build();
                    serviceCounterRepository.save(counter);
                }
            }
        }

    }

    @SneakyThrows
    private Event buildEvent(EventType eventType) {
        var key = String.join("_", BARCODE, PARCEL_ID);
        var route = objectMapper.valueToTree(initialRoute());
        return Event.builder()
            .key(key)
            .type(eventType)
            .route(route)
            .eventTimestamp(DateTimeUtils.nowUtc())
            .maxServiceTime(DateTimeUtils.nowUtc())
            .dummy(false)
            .processed(true)
            .build();
    }

    private DeliveryRoute getRealRecalculatedRoute() {
        return route(
            segment(10)
                .type(PointType.WAREHOUSE)
                .partnerName("Основной склад")
                .partnerType(PartnerType.DROPSHIP)
                .partnerId(100)
                .services(
                    service(1).code(CUTOFF).start(today("00:00").plusDays(0)).items(items()),     // NOW
                    service(2).code(PROCESSING).start(today("10:00").plusDays(0)).items(items()), // NOW
                    service(3).code(SHIPMENT).start(today("10:00").plusDays(0)).items(items())    // NOW
                ),
            segment(20)
                .type(PointType.MOVEMENT)
                .partnerName("Основной склад")
                .partnerType(PartnerType.DROPSHIP)
                .partnerId(100)
                .services(
                    service(4).code(MOVEMENT).start(today("10:00").plusDays(0)).items(items()),   // NOW
                    service(5).code(SHIPMENT).start(today("20:00").plusDays(0)).items(items())    // NOW
                ),
            segment(30)
                .type(PointType.WAREHOUSE)
                .partnerName("Яндекс.Маркет DS")
                .partnerType(PartnerType.SORTING_CENTER)
                .partnerId(200)
                .services(
                    service(6).code(INBOUND).start(today("20:00").plusDays(0)).items(items()),    // NOW+2d from below
                    service(7).code(SORT).start(today("11:55").plusDays(1)).items(items()),       // NOW+2d from below
                    service(8).code(SHIPMENT).start(today("13:55").plusDays(1)).items(items())    // NOW+2d from below
                ),
            segment(40)
                .type(PointType.MOVEMENT)
                .partnerName("МК Тарный")
                .partnerType(PartnerType.DELIVERY)
                .partnerId(300)
                .services(
                    service(9).code(INBOUND).start(today("13:55").plusDays(1)).items(items()),    // NOW+1d
                    service(10).code(MOVEMENT).start(today("00:00").plusDays(2)).items(items()),  // NOW+2d
                    service(11).code(SHIPMENT).start(today("04:00").plusDays(2)).items(items())   // NOW+2d
                ),
            segment(50)
                .type(PointType.LINEHAUL)
                .partnerName("МК Тарный")
                .partnerType(PartnerType.DELIVERY)
                .partnerId(300)
                .services(
                    service(12).code(DELIVERY).start(today("04:00").plusDays(2)).items(items()),  // NOW+2d
                    service(13).code(LAST_MILE).start(today("04:00").plusDays(2)).items(items())  // NOW+2d
                ),
            segment(60)
                .type(PointType.HANDING)
                .partnerName("МК Тарный")
                .partnerType(PartnerType.DELIVERY)
                .partnerId(300)
                .services(
                    service(14).code(HANDING).start(today("09:00").plusDays(2)).items(items())    // NOW+2d
                )
        );
    }

    private LocalDateTime fixDateTime() {
        return LocalDateTime.of(2022, 5, 22, 0, 0);
    }

    private void runInTransaction(Runnable runnable) {
        new TransactionTemplate(transactionManager).execute(status -> {
            runnable.run();
            return null;
        });
    }

}

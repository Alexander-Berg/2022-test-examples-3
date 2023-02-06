package ru.yandex.market.checkout.checkouter.lavka;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YaLavkaHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryResultProvider;
import ru.yandex.market.checkout.util.yalavka.YaLavkaDeliveryServiceConfigurer;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.taxi.model.DeliveryOptionTimeIntervals;
import ru.yandex.market.common.taxi.model.DeliveryOptionsCheckRequest;
import ru.yandex.market.common.taxi.model.GeoCoordinates;
import ru.yandex.market.common.taxi.model.ResourceClassName;
import ru.yandex.market.common.taxi.model.ResourceFeature;
import ru.yandex.market.common.taxi.model.ResourcePhysicalFeature;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static ru.yandex.common.util.collections.CollectionUtils.expectedSingleResult;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.ON_DEMAND;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.ON_DEMAND_YALAVKA;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaDeliveryOptionsPolicy.CHECK;
import static ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaDeliveryOptionsPolicy.DISABLE_ALWAYS;
import static ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaDeliveryOptionsPolicy.DO_NOTHING;
import static ru.yandex.market.checkout.helpers.YaLavkaHelper.LAVKA_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.helpers.YaLavkaHelper.lavkaOption;
import static ru.yandex.market.checkout.helpers.YaLavkaHelper.normalOption;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

class YaLavkaOrderCreateFlowTest extends AbstractWebTestBase {

    private static final LocalTime RECEPTION_START = LocalTime.parse("08:00:00");
    private static final LocalTime RECEPTION_END = LocalTime.parse("12:00:00");

    @Autowired
    private YaLavkaDeliveryServiceConfigurer yaLavkaDSConfigurer;
    @Autowired
    private CheckouterOrderHistoryEventsApi orderHistoryEventsClient;
    @Autowired
    private YaLavkaHelper yaLavkaHelper;

    private static void checkTimeIntervals(List<DeliveryOptionTimeIntervals> timeIntervals, Integer... dayOffsets) {
        assertEquals(dayOffsets.length, timeIntervals.size());
        Iterator<DeliveryOptionTimeIntervals> it = timeIntervals.stream()
                .sorted(Comparator.comparingLong(
                        interval -> expectedSingleResult(interval.getReceptionIntervals()).getFrom()))
                .iterator();
        LocalDate date = LocalDate.now();
        for (int dayOffset : dayOffsets) {
            DeliveryOptionTimeIntervals interval = it.next();
            assertEquals(
                    LocalDateTime.of(date.plusDays(dayOffset), RECEPTION_START).atZone(ZoneId.systemDefault())
                            .toEpochSecond(),
                    expectedSingleResult(interval.getReceptionIntervals()).getFrom()
            );
            assertEquals(
                    LocalDateTime.of(date.plusDays(dayOffset), RECEPTION_END).atZone(ZoneId.systemDefault())
                            .toEpochSecond(),
                    expectedSingleResult(interval.getReceptionIntervals()).getTo()
            );
        }
    }

    private static void checkGpsMatching(String buyerAddressGps, GeoCoordinates coordinates) {
        assertThat(coordinates.getLongitude() + "," + coordinates.getLatitude(),
                equalTo(buyerAddressGps));
    }

    private static void checkBoxAndResourceFeatureMatching(
            List<ParcelBox> boxes,
            List<ResourceFeature> features
    ) {
        ParcelBox box = expectedSingleResult(boxes);
        ResourceFeature feature = expectedSingleResult(features);
        assertThat(feature.getClassName(), equalTo(ResourceClassName.PHYSICAL));
        ResourcePhysicalFeature physicalFeature = feature.getResourcePhysicalFeature();
        assertTrue(physicalFeature.isRotatable());
        assertThat(physicalFeature.getWeight(), equalTo(box.getWeight().intValue())); // г, г
        assertThat(physicalFeature.getX(), equalTo(box.getDepth().intValue() * 10)); // мм, см
        assertThat(physicalFeature.getY(), equalTo(box.getHeight().intValue() * 10)); // мм, см
        assertThat(physicalFeature.getZ(), equalTo(box.getWidth().intValue() * 10)); // мм, см
    }

    @BeforeEach
    void setUp() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
        yaLavkaDSConfigurer.reset();
    }

    @Test
    void shouldNotDisableLavkaOptionsWhenDoNothing() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, DO_NOTHING);
        yaLavkaDSConfigurer.configureOrderReservationRequest(HttpStatus.OK);
        Parameters parameters = yaLavkaHelper.buildParameters(false,
                normalOption(1),
                normalOption(2),
                lavkaOption(1),
                normalOption(3),
                lavkaOption(2)
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            List<Long> deliveryOptionsDsIds = YaLavkaHelper.getDeliveryOptions(multiCart).stream()
                    .map(Delivery::getDeliveryServiceId)
                    .collect(Collectors.toList());
            assertEquals(List.of(
                    MOCK_DELIVERY_SERVICE_ID,
                    MOCK_DELIVERY_SERVICE_ID,
                    LAVKA_DELIVERY_SERVICE_ID,
                    MOCK_DELIVERY_SERVICE_ID,
                    LAVKA_DELIVERY_SERVICE_ID
            ), deliveryOptionsDsIds);
            YaLavkaHelper.assertHasNoErrors(multiCart);
        });

        YaLavkaHelper.assertHasNoErrors(multiOrder);
        yaLavkaDSConfigurer.assertNoCheckRequests();
    }

    @Test
    void shouldDisableLavkaOptionsWhenDisableAlways() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, DISABLE_ALWAYS);
        Parameters parameters = yaLavkaHelper.buildParameters(false,
                normalOption(1),
                normalOption(2),
                lavkaOption(1),
                normalOption(3),
                lavkaOption(2)
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            List<Long> deliveryOptionsDsIds = YaLavkaHelper.getDeliveryOptions(multiCart).stream()
                    .map(Delivery::getDeliveryServiceId)
                    .collect(Collectors.toList());
            assertEquals(List.of(
                    MOCK_DELIVERY_SERVICE_ID,
                    MOCK_DELIVERY_SERVICE_ID,
                    MOCK_DELIVERY_SERVICE_ID
            ), deliveryOptionsDsIds);
            YaLavkaHelper.assertHasNoErrors(multiCart);
        });

        YaLavkaHelper.assertHasNoErrors(multiOrder);
        yaLavkaDSConfigurer.assertNoInteractions();
    }

    @Test
    void shouldDisableLavkaOptionsWhenRequestError() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, CHECK);
        yaLavkaDSConfigurer.configureUnsuccessfulCheckDeliveryOptionsRequest();
        Parameters parameters = yaLavkaHelper.buildParameters(false,
                normalOption(1),
                normalOption(2),
                lavkaOption(1),
                normalOption(3),
                lavkaOption(2)
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            List<Long> deliveryOptionsDsIds = YaLavkaHelper.getDeliveryOptions(multiCart).stream()
                    .map(Delivery::getDeliveryServiceId)
                    .collect(Collectors.toList());
            assertEquals(List.of(
                    MOCK_DELIVERY_SERVICE_ID,
                    MOCK_DELIVERY_SERVICE_ID,
                    MOCK_DELIVERY_SERVICE_ID
            ), deliveryOptionsDsIds);
            YaLavkaHelper.assertHasNoErrors(multiCart);
        });

        YaLavkaHelper.assertHasNoErrors(multiOrder);
        yaLavkaDSConfigurer.assertHasCheckRequests();
    }

    @Test
    void shouldCheckYaLavkaOptions() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, CHECK);
        yaLavkaDSConfigurer.configureCheckDeliveryOptionsRequest(1, 2);
        Parameters parameters = yaLavkaHelper.buildParameters(false,
                normalOption(1),
                normalOption(2),
                lavkaOption(1), // Not available
                normalOption(3),
                lavkaOption(2), // Available
                lavkaOption(3), // Available
                normalOption(4)
        );

        List<Delivery> deliveryOptions = new ArrayList<>();

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            List<? extends Delivery> options = YaLavkaHelper.getDeliveryOptions(multiCart);
            List<Long> deliveryOptionsDsIds = options.stream()
                    .map(Delivery::getDeliveryServiceId)
                    .collect(Collectors.toList());
            assertEquals(List.of(
                    MOCK_DELIVERY_SERVICE_ID,
                    MOCK_DELIVERY_SERVICE_ID,
                    MOCK_DELIVERY_SERVICE_ID,
                    MOCK_DELIVERY_SERVICE_ID,
                    LAVKA_DELIVERY_SERVICE_ID,
                    LAVKA_DELIVERY_SERVICE_ID
            ), deliveryOptionsDsIds);
            YaLavkaHelper.assertHasNoErrors(multiCart);
            deliveryOptions.addAll(options);
        });
        YaLavkaHelper.assertHasNoErrors(multiOrder);

        yaLavkaDSConfigurer.assertHasCheckRequests();
        DeliveryOptionsCheckRequest request = yaLavkaDSConfigurer.getCheckRequestBody();

        Order order = multiOrder.getCarts().get(0);
        checkGpsMatching(order.getDelivery().getBuyerAddress().getGps(), request.getCoordinates());
        checkBoxAndResourceFeatureMatching(
                order.getDelivery().getParcels().get(0).getBoxes(),
                request.getResourceFeatures()
        );
        checkTimeIntervals(request.getTimeIntervals(), 1, 2, 3);
    }

    @Test
    void shouldSetDeliveryFeaturesForLavkaDelivery() {
        yaLavkaDSConfigurer.configureOrderReservationRequest(
                HttpStatus.OK,
                "{\n" +
                        "    \"request_id\": \"3045a971-9ff8-4b6f-8258-eb1fe02852bb\",\n" +
                        "    \"nodes_info\": [\n" +
                        "        {\n" +
                        "            \"operator_id\": \"lavka\",\n" +
                        "            \"operator_station_id\": \"83788ecf-7c21-4f60-9ea4-04602d5e6e1d\",\n" +
                        "            \"station_id\": \"83788ecf-7c21-4f60-9ea4-04602d5e6e1d\"\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}"
        );
        Parameters parameters = yaLavkaHelper.buildParameters(true,
                normalOption(1),
                lavkaOption(1)
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            List<? extends Delivery> options = YaLavkaHelper.getDeliveryOptions(multiCart);
            assertEquals(2, options.size());

            Delivery normalOption = options.get(0);
            assertEquals(MOCK_DELIVERY_SERVICE_ID, normalOption.getDeliveryServiceId());
            assertNull(normalOption.getFeatures());

            Delivery lavkaOption = options.get(1);
            assertEquals(LAVKA_DELIVERY_SERVICE_ID, lavkaOption.getDeliveryServiceId());

            // Проверяем наличие delivery.features в корзине
            assertEquals(Set.of(ON_DEMAND), lavkaOption.getFeatures());

            // Чистим features, так как с фронта они не передаются
            lavkaOption.setFeatures(null);

            YaLavkaHelper.assertHasNoErrors(multiCart);
        });
        YaLavkaHelper.assertHasNoErrors(multiOrder);

        Set<DeliveryFeature> expectedFeatures = Set.of(ON_DEMAND, ON_DEMAND_YALAVKA);

        // Проверяем наличие delivery.features в созданном заказе
        Order order = CollectionUtils.expectedSingleResult(multiOrder.getOrders());
        Delivery delivery = order.getDelivery();
        assertEquals(LAVKA_DELIVERY_SERVICE_ID, delivery.getDeliveryServiceId());
        assertEquals(expectedFeatures, delivery.getFeatures());

        // Проверяем наличие delivery.features в событиях
        List<OrderHistoryEvent> events = getOrderEvents(order.getId());
        assertTrue(events.size() > 0);
        events.forEach(event -> assertEquals(expectedFeatures, event.getOrderAfter().getDelivery().getFeatures()));

        // Проверяем наличие delivery.features в ручке /orders/by-uid/{userId}/recent
        order = CollectionUtils.expectedSingleResult(client.getOrdersByUserRecent(order.getBuyer().getUid(),
                new Color[]{BLUE}, new OrderStatus[]{order.getStatus()}, 1,
                new OptionalOrderPart[]{OptionalOrderPart.DELIVERY}, null));
        delivery = order.getDelivery();
        assertEquals(expectedFeatures, delivery.getFeatures());
    }

    @Test
    void shouldNotSetDeliveryFeaturesForNormalDelivery() {
        Parameters parameters = yaLavkaHelper.buildParameters(false,
                normalOption(1)
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            List<? extends Delivery> options = YaLavkaHelper.getDeliveryOptions(multiCart);
            assertEquals(1, options.size());

            Delivery normalOption = options.get(0);
            assertEquals(MOCK_DELIVERY_SERVICE_ID, normalOption.getDeliveryServiceId());
            assertNull(normalOption.getFeatures());

            YaLavkaHelper.assertHasNoErrors(multiCart);
        });
        YaLavkaHelper.assertHasNoErrors(multiOrder);

        Order order = CollectionUtils.expectedSingleResult(multiOrder.getOrders());
        Delivery delivery = order.getDelivery();
        assertNull(delivery.getFeatures());

        List<OrderHistoryEvent> events = getOrderEvents(order.getId());
        assertTrue(events.size() > 0);
        events.forEach(event -> assertNull(event.getOrderAfter().getDelivery().getFeatures()));
    }

    @Test
    void shouldNotReserveWhenNotLavkaDelivery() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, CHECK);
        yaLavkaDSConfigurer.configureCheckDeliveryOptionsRequest(0);
        Parameters parameters = yaLavkaHelper.buildParameters(false, normalOption(1));

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        YaLavkaHelper.assertHasNoErrors(multiOrder);
        yaLavkaDSConfigurer.assertNoInteractions();
    }

    @Test
    void shouldReserveWhenLavkaDelivery() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, CHECK);
        yaLavkaDSConfigurer.configureCheckDeliveryOptionsRequest(0);
        yaLavkaDSConfigurer.configureOrderReservationRequest(HttpStatus.OK);
        Parameters parameters = yaLavkaHelper.buildParameters(true, lavkaOption(1));

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        YaLavkaHelper.assertHasNoErrors(multiOrder);
        yaLavkaDSConfigurer.assertHasCheckRequests();
        yaLavkaDSConfigurer.assertHasOrderReserveRequests();
    }

    @Test
    void shouldProduceOrderCreateErrorWhenReservationFailed() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, CHECK);
        yaLavkaDSConfigurer.configureCheckDeliveryOptionsRequest(0);
        yaLavkaDSConfigurer.configureOrderReservationRequest(HttpStatus.BAD_REQUEST);
        Parameters parameters = yaLavkaHelper.buildParameters(true, lavkaOption(1));
        parameters.setCheckOrderCreateErrors(false);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            YaLavkaHelper.assertHasNoErrors(multiCart);
        });
        YaLavkaHelper.assertHasErrors(multiOrder);
        yaLavkaDSConfigurer.assertHasCheckRequests();
        yaLavkaDSConfigurer.assertHasOrderReserveRequests();
    }

    @Test
    void shouldDisableLavkaDeliveryForNoAuth() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, CHECK);
        yaLavkaDSConfigurer.configureCheckDeliveryOptionsRequest(0);
        yaLavkaDSConfigurer.configureOrderReservationRequest(HttpStatus.OK);
        Parameters parameters = yaLavkaHelper.buildParameters(false,
                normalOption(1),
                lavkaOption(1)
        );
        // Назначаем muid (пользователь без паспортного аккаунта)
        parameters.getBuyer().setUid(1_152_921_504_606_846_976L);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            List<? extends Delivery> options = YaLavkaHelper.getDeliveryOptions(multiCart);

            // Проверяем, что есть только одна опция доставки - НЕ Яндекс.Лавки
            assertEquals(1, options.size());
            assertEquals(MOCK_DELIVERY_SERVICE_ID, options.get(0).getDeliveryServiceId());
            assertTrue(expectedSingleResult(multiCart.getCarts()).isNoAuth());
            YaLavkaHelper.assertHasNoErrors(multiCart);
        });
        YaLavkaHelper.assertHasNoErrors(multiOrder);
        Order order = expectedSingleResult(multiOrder.getOrders());
        assertTrue(order.isNoAuth());
        assertEquals(MOCK_DELIVERY_SERVICE_ID, order.getDelivery().getDeliveryServiceId());
        yaLavkaDSConfigurer.assertNoInteractions();
    }

    @Test
    void shouldCreateLavkaDeliveryOrderWithCombinator() throws Exception {
        try {
            checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                    Collections.<String>emptySet()));
            checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, CHECK);
            yaLavkaDSConfigurer.configureCheckDeliveryOptionsRequest(0);
            yaLavkaDSConfigurer.configureOrderReservationRequest(
                    HttpStatus.OK,
                    "{\n" +
                            "    \"request_id\": \"3045a971-9ff8-4b6f-8258-eb1fe02852bb\",\n" +
                            "    \"nodes_info\": [\n" +
                            "        {\n" +
                            "            \"operator_id\": \"lavka\",\n" +
                            "            \"operator_station_id\": \"83788ecf-7c21-4f60-9ea4-04602d5e6e1d\",\n" +
                            "            \"station_id\": \"83788ecf-7c21-4f60-9ea4-04602d5e6e1d\"\n" +
                            "        }\n" +
                            "    ]\n" +
                            "}"
            );
            Parameters parameters = yaLavkaHelper.buildParameters(true, true, lavkaOption(1));
            parameters.setMinifyOutlets(true);

            MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
                reportMock.resetRequests();
            });
            YaLavkaHelper.assertHasNoErrors(multiOrder);
            yaLavkaDSConfigurer.assertNoCheckRequests();
            yaLavkaDSConfigurer.assertHasOrderReserveRequests();

            ServeEvent deliveryRouteServeEvent = reportMock.getServeEvents().getRequests().stream()
                    .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                            && req.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route"))
                    .findFirst().get();

            assertEquals("on-demand",
                    getOnlyElement(deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-subtype")
                            .values()));

            assertLinesMatch(List.of(".*\\.0900-.*\\.2100"),
                    deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-interval").values());

            JSONAssert.assertEquals(
                    DeliveryResultProvider.ROUTE,
                    multiOrder.getOrders().get(0).getDelivery().getParcels().get(0).getRoute().toString(),
                    JSONCompareMode.NON_EXTENSIBLE
            );
        } finally {
            checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(false,
                    Collections.<String>emptySet()));
        }
    }

    @Test
    void shouldCreateDeferredCourierDeliveryOrderWithCombinator() {
        try {
            checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                    Collections.<String>emptySet()));

            Parameters parameters = yaLavkaHelper.buildParametersForDeferredCourier(1);

            MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
                reportMock.resetRequests();
            });
            YaLavkaHelper.assertHasNoErrors(multiOrder);

            ServeEvent deliveryRouteServeEvent = reportMock.getServeEvents().getRequests().stream()
                    .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                            && req.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route"))
                    .findFirst().get();

            assertEquals("deferred-courier",
                    getOnlyElement(deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-subtype")
                            .values()));

            assertLinesMatch(List.of(".*\\.1000-.*\\.1100"),
                    deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-interval").values());

            JSONAssert.assertEquals(
                    DeliveryResultProvider.ROUTE,
                    multiOrder.getOrders().get(0).getDelivery().getParcels().get(0).getRoute().toString(),
                    JSONCompareMode.NON_EXTENSIBLE
            );
        } finally {
            checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(false,
                    Collections.<String>emptySet()));
        }
    }

    public static Stream<Arguments> timeSlotParameterizedData() {
        return Stream.of(
                new Object[] {10, 11, DeliveryFeature.DEFERRED_COURIER_ONE_HOUR_INTERVAL},
                new Object[] {10, 18, DeliveryFeature.DEFERRED_COURIER_WIDE_INTERVAL}
        ).map(Arguments::of);
    }

    @ParameterizedTest(name = "Интервал доставки с {0} до {1}: заказ содержит фичу {2}")
    @MethodSource("timeSlotParameterizedData")
    void shouldCreateWideSlotDeferredCourierDeliveryOrder(
            int fromTime, int toTime, DeliveryFeature timeSpecifiedFeature) {
        try {
            checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                    Collections.<String>emptySet()));

            Parameters parameters = yaLavkaHelper.buildParametersForDeferredCourier(
                    ActualDeliveryProvider.builder().addDelivery(
                            MOCK_DELIVERY_SERVICE_ID,
                            1,
                            null,
                            Duration.ofHours(23),
                            null,
                            null,
                            1,
                            1,
                            ado -> {
                                ado.setIsDeferredCourier(false);
                                ado.setTimeIntervals(singletonList(new DeliveryTimeInterval(
                                        LocalTime.of(fromTime, 0),
                                        LocalTime.of(toTime, 0))));
                            })
            );

            MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
                reportMock.resetRequests();
            });
            YaLavkaHelper.assertHasNoErrors(multiOrder);

            ServeEvent deliveryRouteServeEvent = reportMock.getServeEvents().getRequests().stream()
                    .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                            && req.getRequest().getQueryParams().get("place").firstValue().equals("delivery_route"))
                    .findFirst().get();

            assertEquals("ordinary",
                    getOnlyElement(
                            deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-subtype").values()));

            assertLinesMatch(List.of(".*\\." + fromTime + "00-.*\\." + toTime + "00"),
                    deliveryRouteServeEvent.getRequest().getQueryParams().get("delivery-interval").values());

            Order order = multiOrder.getOrders().get(0);
            JSONAssert.assertEquals(
                    DeliveryResultProvider.ROUTE,
                    order.getDelivery().getParcels().get(0).getRoute().toString(),
                    JSONCompareMode.NON_EXTENSIBLE
            );
            assertTrue(OrderTypeUtils.isDeferredCourierDelivery(order));
            assertTrue(order.getDelivery().containsFeature(timeSpecifiedFeature));
        } finally {
            checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(false,
                    Collections.<String>emptySet()));
        }
    }

    @Test
    void shouldInterruptCheckRequestOnTimeout() throws Exception {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, CHECK);

        // Конфигурируем вызов, который подтверждает обе лавочные опции, но должен прерваться по таймауту
        yaLavkaDSConfigurer.configureCheckDeliveryOptionsRequestWithDelay(1000, 0, 1);
        Parameters parameters = yaLavkaHelper.buildParameters(false,
                lavkaOption(1),
                normalOption(1),
                lavkaOption(2)
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            List<Long> deliveryOptionsDsIds = YaLavkaHelper.getDeliveryOptions(multiCart).stream()
                    .map(Delivery::getDeliveryServiceId)
                    .collect(Collectors.toList());

            // Проверяем, что в корзине нет ни обной опции доставки Яндекс Лавкой
            assertEquals(List.of(MOCK_DELIVERY_SERVICE_ID), deliveryOptionsDsIds);
        });
        YaLavkaHelper.assertHasNoErrors(multiOrder);
        yaLavkaDSConfigurer.assertHasCheckRequests();
    }

    private List<OrderHistoryEvent> getOrderEvents(long orderId) {
        OrderFilter orderFilter = new OrderFilter();
        orderFilter.setRgb(new Color[]{BLUE});
        return orderHistoryEventsClient.getOrderHistoryEvents(
                        0, 100, null, false, null,
                        orderFilter, ClientRole.SYSTEM, null, null, Set.of(OptionalOrderPart.CHANGE_REQUEST)
                ).getContent().stream()
                .filter(event -> event.getOrderAfter().getId() == orderId)
                .sorted(Comparator.comparingLong(OrderHistoryEvent::getId))
                .collect(Collectors.toList());
    }
}

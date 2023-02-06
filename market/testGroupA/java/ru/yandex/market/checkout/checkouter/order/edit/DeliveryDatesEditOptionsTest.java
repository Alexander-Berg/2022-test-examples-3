package ru.yandex.market.checkout.checkouter.order.edit;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryLastMileEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.MarketReportSearchService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.service.combinator.CombinatorDeliveryType;
import ru.yandex.market.checkout.checkouter.service.combinator.CombinatorUtils;
import ru.yandex.market.checkout.checkouter.service.combinator.DeliverySubtype;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.DateDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.DeliveryOptionDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.IntervalDto;
import ru.yandex.market.checkout.checkouter.service.combinator.postponedelivery.PostponeDeliveryRequest;
import ru.yandex.market.checkout.checkouter.service.combinator.postponedelivery.PostponeDeliveryRequestFactory;
import ru.yandex.market.checkout.checkouter.service.combinator.postponedelivery.PostponeDeliveryResponse;
import ru.yandex.market.checkout.checkouter.service.combinator.redeliverycourieroptions.RedeliveryCourierOptionsResponse;
import ru.yandex.market.checkout.checkouter.service.combinator.redeliverypickuppointoption.RedeliveryPickupPointOptionResponse;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.delivery.RouteCleanerTaskV2;
import ru.yandex.market.checkout.checkouter.trace.OrderEditOptionsContextHolder;
import ru.yandex.market.checkout.checkouter.util.DeliveryUtil;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YaLavkaHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;

public class DeliveryDatesEditOptionsTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer combinatorMock;
    @Autowired
    private YaLavkaHelper yaLavkaHelper;
    @Autowired
    private RouteCleanerTaskV2 routeCleanerTaskV2;


    @Value("${market.checkouter.deliveryDatesEditOptions.countTotal:3}")
    private Integer countTotal;
    @Value("${market.checkouter.deliveryDatesEditOptions.daysMax:3}")
    private Integer daysMax;

    @BeforeEach
    void setUp() throws Exception {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    private static PostponeDeliveryResponse getPostponeDeliveryCombinatorResponse(
            Order order,
            PersonalDataService personalDataService) {
        var request = PostponeDeliveryRequestFactory.buildRequest(order,
                personalDataService::getPersGps);
        var response = new PostponeDeliveryResponse();
        response.setDestination(request.getDestination());
        var interval = request.getInterval();
        interval.getFrom().setHour(interval.getFrom().getHour() + 2);
        interval.getTo().setHour(interval.getTo().getHour() + 2);
        var options = new ArrayList<DeliveryOptionDto>();
        options.add(createOptionFromRequest(request, order));
        var sameDayOption = createOptionFromRequest(request, order);
        sameDayOption.getInterval().getFrom().setHour(sameDayOption.getInterval().getFrom().getHour() + 1);
        sameDayOption.getInterval().getTo().setHour(sameDayOption.getInterval().getTo().getHour() + 1);
        options.add(sameDayOption);
        var option = createOptionFromRequest(request, order);
        option.setDeliveryType(CombinatorDeliveryType.PICKUP);
        option.setDeliverySubtype(DeliverySubtype.ORDINARY);
        options.add(option);
        option = createOptionFromRequest(request, order);
        option.setDeliveryType(CombinatorDeliveryType.POST);
        option.setDeliverySubtype(DeliverySubtype.ORDINARY);
        options.add(option);
        response.setOptions(options);
        return response;
    }

    private static RedeliveryCourierOptionsResponse getRedeliveryCourierOptionsCombinatorResponse(
            Order order,
            PersonalDataService personalDataService) {
        var postponeDeliveryResponse =
                getPostponeDeliveryCombinatorResponse(order, personalDataService);
        var response = new RedeliveryCourierOptionsResponse();
        response.setDestination(postponeDeliveryResponse.getDestination());
        response.setOptions(postponeDeliveryResponse.getOptions());
        return response;
    }

    private static RedeliveryPickupPointOptionResponse getRedeliveryPickupPointOptionCombinatorResponse(
            Order order,
            Long outletId,
            PersonalDataService personalDataService) {
        var request = PostponeDeliveryRequestFactory.buildRequest(order,
                personalDataService::getPersGps);
        var option = createOptionFromRequest(request, order);
        option.setDeliveryType(CombinatorDeliveryType.PICKUP);
        option.setDeliverySubtype(DeliverySubtype.ORDINARY);
        var response = new RedeliveryPickupPointOptionResponse(
                request.getDestination(),
                List.of(option));
        response.getDestination().setLogisticPointId(outletId.toString());
        return response;
    }

    private static DeliveryOptionDto createOptionFromRequest(PostponeDeliveryRequest request, Order order) {
        var option = new DeliveryOptionDto();
        option.setInterval(new IntervalDto(request.getInterval()));
        option.setDateFrom(new DateDto(request.getDateFrom()));
        option.setDateTo(new DateDto(request.getDateTo()));
        option.setDeliveryServiceId(request.getDeliveryServiceId());
        option.setDeliveryType(request.getDeliveryType());
        option.setDeliverySubtype(request.getDeliverySubtype());
        option.setPaymentMethods(Set.of(CombinatorUtils.toCombinatorPaymentMethod(order.getPaymentMethod())));
        return option;
    }

    @Test
    public void notDeliveryEditedOrderTest() {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        var order = orderCreateHelper.createOrder(parameters);

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                OrderProvider.SHOP_ID, List.of(Color.WHITE), orderEditOptionsRequest);
        assertNotNull(orderEditOptionsResponse);
        var deliveryDatesEditOptions = orderEditOptionsResponse.getDeliveryDatesEditOptions();
        assertNotNull(deliveryDatesEditOptions);
        assertEquals(countTotal, deliveryDatesEditOptions.getCountTotal());
        assertEquals(countTotal, deliveryDatesEditOptions.getCountRemain());
        assertEquals(daysMax, deliveryDatesEditOptions.getDaysMax());
    }

    @Test
    public void someTimesDeliveryDatesEditedOrderTest() {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        var order = orderCreateHelper.createOrder(parameters);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(LocalDate.now(getClock()).plusDays(3))
                .toDate(LocalDate.now(getClock()).plusDays(4))
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES_BY_DS)
                .build());

        client.editOrder(
                order.getId(),
                ClientRole.SHOP,
                OrderProvider.SHOP_ID,
                singletonList(WHITE),
                orderEditRequest
        );

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                OrderProvider.SHOP_ID, List.of(Color.WHITE), orderEditOptionsRequest);
        assertNotNull(orderEditOptionsResponse);
        var deliveryDatesEditOptions = orderEditOptionsResponse.getDeliveryDatesEditOptions();
        assertNotNull(deliveryDatesEditOptions);
        assertEquals(countTotal, deliveryDatesEditOptions.getCountTotal());
        assertEquals(countTotal - 1, deliveryDatesEditOptions.getCountRemain());
        assertEquals(daysMax, deliveryDatesEditOptions.getDaysMax());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void getOrderEditOptions_withToggle_shouldUseCombinatorFlow(boolean emptyTypes) {
        // Assign
        OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder holder =
                new OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder();
        checkouterProperties.setEnableDeliveryDatesEditOptionsCombinatorAlways(true);
        checkouterProperties.setEnableEmptyEditOptionsCombinatorAlways(true);

        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        if (!emptyTypes) {
            orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));
        }

        reportMock.resetAll();

        // Act
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                OrderProvider.SHOP_ID, List.of(Color.BLUE), orderEditOptionsRequest);

        // Assert
        assertNotNull(orderEditOptionsResponse);

        List<ServeEvent> events = reportMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getQueryParams().get("place")
                        .containsValue("actual_delivery"))
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());
        var actualDeliveryCallQueryParams = actualDeliveryCalls.get(0).getRequest().getQueryParams();

        var combinatorParam = actualDeliveryCallQueryParams.get(MarketReportSearchService.COMBINATOR);
        assertTrue(combinatorParam.isPresent());
        assertEquals("1", combinatorParam.values().get(0));

        var forceDeliveryIdParam = actualDeliveryCallQueryParams.get(MarketReportSearchService.FORCE_DELIVERY_ID);
        assertTrue(forceDeliveryIdParam.isPresent());
        assertEquals(order.getDelivery().getDeliveryServiceId().toString(), forceDeliveryIdParam.values().get(0));

        var gpsParam = actualDeliveryCallQueryParams.get(MarketReportSearchService.GPS_PARAM);
        assertTrue(gpsParam.isPresent());
        var expectedGpsParam = String.format("lat:%s;lon:%s",
                parameters.getGeocoderParameters().getLatitude(),
                parameters.getGeocoderParameters().getLongitude());
        assertEquals(expectedGpsParam, gpsParam.values().get(0));

        if (!emptyTypes) {
            assertThat(holder.getAttributes())
                    .containsEntry("requestedTypes", ChangeRequestType.DELIVERY_DATES.toString());
        } else {
            assertThat(holder.getAttributes())
                    .doesNotContainKey("requestedTypes");
        }
        assertThat(holder.getAttributes())
                .containsEntry("actualDeliveryWithCombinator", true)
                .containsEntry("actualDeliveryForceDeliveryId", order.getDelivery().getDeliveryServiceId())
                .containsEntry("anyDeliveryOption", orderEditOptionsResponse.getDeliveryOptions() != null
                        && !orderEditOptionsResponse.getDeliveryOptions().isEmpty())
                .containsEntry("isCurrentDeliveryOptionActual",
                        orderEditOptionsResponse.getCurrentDeliveryOptionActual());
    }

    @Test
    public void getOrderEditOptions_withoutToggle_shouldNotUseCombinatorFlow() {
        // Assign
        OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder holder =
                new OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder();
        holder.clear();
        checkouterProperties.setEnableDeliveryDatesEditOptionsCombinatorAlways(false);

        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));

        reportMock.resetAll();

        // Act
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                OrderProvider.SHOP_ID, List.of(Color.BLUE), orderEditOptionsRequest);

        // Assert
        assertNotNull(orderEditOptionsResponse);

        List<ServeEvent> events = reportMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getQueryParams().get("place")
                        .containsValue("actual_delivery"))
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());
        var actualDeliveryCallQueryParams = actualDeliveryCalls.get(0).getRequest().getQueryParams();

        var combinatorParam = actualDeliveryCallQueryParams.get(MarketReportSearchService.COMBINATOR);
        assertTrue(combinatorParam.isPresent());
        assertEquals("0", combinatorParam.values().get(0));

        var forceDeliveryIdParam = actualDeliveryCallQueryParams.get(MarketReportSearchService.FORCE_DELIVERY_ID);
        assertNull(forceDeliveryIdParam);

        var gpsParam = actualDeliveryCallQueryParams.get(MarketReportSearchService.GPS_PARAM);
        assertNull(gpsParam);

        assertThat(holder.getAttributes())
                .containsEntry("requestedTypes", ChangeRequestType.DELIVERY_DATES.toString())
                .containsEntry("actualDeliveryWithCombinator", false)
                .doesNotContainKey("actualDeliveryForceDeliveryId")
                .containsEntry("anyDeliveryOption", orderEditOptionsResponse.getDeliveryOptions() != null
                        && !orderEditOptionsResponse.getDeliveryOptions().isEmpty())
                .containsEntry("isCurrentDeliveryOptionActual",
                        orderEditOptionsResponse.getCurrentDeliveryOptionActual());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void getOrderEditOptions_withToggle_shouldGoToCombinator(boolean emptyTypes)
            throws JsonProcessingException {
        // Assign
        OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder holder =
                new OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder();
        checkouterProperties.setEnableDeliveryDatesEditOptionsFromCombinator(true);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                emptySet()));
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));
        parameters.setMinifyOutlets(true);
        DeliveryRouteProvider.cleanActualDelivery(parameters.getReportParameters().getActualDelivery());

        Order order = orderCreateHelper.createOrder(parameters);

        var combinatorResponse =
                getPostponeDeliveryCombinatorResponse(order, personalDataService);
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/postpone-delivery"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        if (!emptyTypes) {
            orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));
        }

        combinatorMock.resetRequests();

        // Act
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                OrderProvider.SHOP_ID, List.of(Color.BLUE), orderEditOptionsRequest);

        // Assert
        assertNotNull(orderEditOptionsResponse);
        assertEquals(1, orderEditOptionsResponse.getDeliveryOptions().size());

        List<ServeEvent> events = combinatorMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getUrl().equals("/postpone-delivery"))
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());

        if (!emptyTypes) {
            assertThat(holder.getAttributes())
                    .containsEntry("requestedTypes", ChangeRequestType.DELIVERY_DATES.toString());
        } else {
            assertThat(holder.getAttributes())
                    .doesNotContainKey("requestedTypes");
        }
        assertThat(holder.getAttributes())
                .containsEntry("deliveryOptionsFromCombinator", true)
                .containsEntry("anyDeliveryOption", orderEditOptionsResponse.getDeliveryOptions() != null
                        && !orderEditOptionsResponse.getDeliveryOptions().isEmpty())
                .containsEntry("isCurrentDeliveryOptionActual",
                        orderEditOptionsResponse.getCurrentDeliveryOptionActual());
    }

    @Test
    public void getOrderEditOptions_afterRouteWasCleaned_shouldGoToCombinator()
            throws JsonProcessingException {
        // Assign
        OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder holder =
                new OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder();
        checkouterProperties.setEnableDeliveryDatesEditOptionsFromCombinator(true);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                emptySet()));
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));
        parameters.setMinifyOutlets(true);
        DeliveryRouteProvider.cleanActualDelivery(parameters.getReportParameters().getActualDelivery());

        Order order = orderCreateHelper.createOrder(parameters);

        // delivery route will be cleaned but routeId should stay
        assertTrue(DeliveryUtil.hasRoute(order));
        setFixedTime(getClock().instant().plus(4, ChronoUnit.DAYS));
        routeCleanerTaskV2.run(TaskRunType.ONCE);
        order = orderService.getOrder(order.getId());
        assertFalse(DeliveryUtil.hasRoute(order));

        var combinatorResponse =
                getPostponeDeliveryCombinatorResponse(order, personalDataService);
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/postpone-delivery"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));

        combinatorMock.resetRequests();

        // Act
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                OrderProvider.SHOP_ID, List.of(Color.BLUE), orderEditOptionsRequest);

        // Assert
        assertNotNull(orderEditOptionsResponse);
        assertEquals(1, orderEditOptionsResponse.getDeliveryOptions().size());

        List<ServeEvent> events = combinatorMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getUrl().equals("/postpone-delivery"))
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());

        assertThat(holder.getAttributes())
                .containsEntry("requestedTypes", ChangeRequestType.DELIVERY_DATES.toString());
        assertThat(holder.getAttributes())
                .containsEntry("deliveryOptionsFromCombinator", true)
                .containsEntry("anyDeliveryOption", orderEditOptionsResponse.getDeliveryOptions() != null
                        && !orderEditOptionsResponse.getDeliveryOptions().isEmpty())
                .containsEntry("isCurrentDeliveryOptionActual",
                        orderEditOptionsResponse.getCurrentDeliveryOptionActual());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void getOrderEditOptions_forDeferredCourier_shouldGoToCombinator(
            boolean deferredCourierOnly)
            throws JsonProcessingException {
        // Assign
        OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder holder =
                new OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder();
        checkouterProperties.setEnableDeliveryDatesEditOptionsFromCombinator(true);
        checkouterProperties.setEnablePostponeDeliveryForDeferredCourierOnly(deferredCourierOnly);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                emptySet()));

        Parameters parameters = yaLavkaHelper.buildParametersForDeferredCourier(1);

        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(OrderTypeUtils.isDeferredCourierDelivery(order));

        var combinatorResponse =
                getPostponeDeliveryCombinatorResponse(order, personalDataService);
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/postpone-delivery"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));

        combinatorMock.resetRequests();

        // Act
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                OrderProvider.SHOP_ID, List.of(Color.BLUE), orderEditOptionsRequest);

        // Assert
        assertNotNull(orderEditOptionsResponse);
        assertEquals(1, orderEditOptionsResponse.getDeliveryOptions().size());

        List<ServeEvent> events = combinatorMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getUrl().equals("/postpone-delivery"))
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());

        assertThat(holder.getAttributes())
                .containsEntry("requestedTypes", ChangeRequestType.DELIVERY_DATES.toString());

        assertThat(holder.getAttributes())
                .containsEntry("deliveryOptionsFromCombinator", true)
                .containsEntry("anyDeliveryOption", orderEditOptionsResponse.getDeliveryOptions() != null
                        && !orderEditOptionsResponse.getDeliveryOptions().isEmpty())
                .containsEntry("isCurrentDeliveryOptionActual",
                        orderEditOptionsResponse.getCurrentDeliveryOptionActual());
    }

    @Test
    public void getOrderEditOptions_withoutToggle_shouldNotGoToCombinatorFlow() throws JsonProcessingException {
        // Assign
        OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder holder =
                new OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder();
        holder.clear();

        checkouterProperties.setEnableDeliveryDatesEditOptionsFromCombinator(false);

        var parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);

        var combinatorResponse = getPostponeDeliveryCombinatorResponse(order, personalDataService);
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/postpone-delivery"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));

        combinatorMock.resetRequests();

        // Act
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                OrderProvider.SHOP_ID, List.of(Color.BLUE), orderEditOptionsRequest);

        // Assert
        assertNotNull(orderEditOptionsResponse);

        List<ServeEvent> events = combinatorMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getUrl().equals("/postpone-delivery"))
                .collect(Collectors.toList());

        assertEquals(0, actualDeliveryCalls.size());

        assertThat(holder.getAttributes())
                .containsEntry("requestedTypes", ChangeRequestType.DELIVERY_DATES.toString())
                .doesNotContainKey("deliveryOptionsFromCombinator")
                .containsEntry("anyDeliveryOption", orderEditOptionsResponse.getDeliveryOptions() != null
                        && !orderEditOptionsResponse.getDeliveryOptions().isEmpty())
                .containsEntry("isCurrentDeliveryOptionActual",
                        orderEditOptionsResponse.getCurrentDeliveryOptionActual());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void getOrderEditOptions_withEmptyOptionsFromCombinator_shouldReturn200(boolean emptyTypes)
            throws JsonProcessingException {
        // Assign
        OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder holder =
                new OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder();
        checkouterProperties.setEnableDeliveryDatesEditOptionsFromCombinator(true);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                emptySet()));
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));
        parameters.setMinifyOutlets(true);
        DeliveryRouteProvider.cleanActualDelivery(parameters.getReportParameters().getActualDelivery());

        Order order = orderCreateHelper.createOrder(parameters);

        var combinatorResponse = new PostponeDeliveryResponse();
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/postpone-delivery"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        if (!emptyTypes) {
            orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));
        }

        combinatorMock.resetRequests();

        // Act
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                OrderProvider.SHOP_ID, List.of(Color.BLUE), orderEditOptionsRequest);

        // Assert
        assertNotNull(orderEditOptionsResponse);
        assertEquals(0, orderEditOptionsResponse.getDeliveryOptions().size());

        List<ServeEvent> events = combinatorMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getUrl().equals("/postpone-delivery"))
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());

        if (!emptyTypes) {
            assertThat(holder.getAttributes())
                    .containsEntry("requestedTypes", ChangeRequestType.DELIVERY_DATES.toString());
        } else {
            assertThat(holder.getAttributes())
                    .doesNotContainKey("requestedTypes");
        }
        assertThat(holder.getAttributes())
                .containsEntry("deliveryOptionsFromCombinator", true)
                .containsEntry("anyDeliveryOption", orderEditOptionsResponse.getDeliveryOptions() != null
                        && !orderEditOptionsResponse.getDeliveryOptions().isEmpty())
                .containsEntry("isCurrentDeliveryOptionActual",
                        orderEditOptionsResponse.getCurrentDeliveryOptionActual());
    }

    @Test
    public void getOrderEditOptions_deliveryLastMile()
            throws JsonProcessingException {
        // Assign
        OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder holder =
                new OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder();
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                emptySet()));
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));
        parameters.setMinifyOutlets(true);
        DeliveryRouteProvider.cleanActualDelivery(parameters.getReportParameters().getActualDelivery());

        Order order = orderCreateHelper.createOrder(parameters);

        var combinatorResponse =
                getRedeliveryCourierOptionsCombinatorResponse(order, personalDataService);
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/redelivery-courier-options"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_LAST_MILE));
        var deliveryLastMile = new DeliveryLastMileEditOptionsRequest();
        deliveryLastMile.setRegionId(order.getRegionId());
        var newAddress = new AddressImpl();
        newAddress.setGps("37.90647953857668,55.70417927289191");
        newAddress.setPreciseRegionId(120539L);
        deliveryLastMile.setAddress(newAddress);
        orderEditOptionsRequest.setDeliveryLastMile(deliveryLastMile);

        combinatorMock.resetRequests();

        // Act
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                BuyerProvider.UID, List.of(Color.BLUE), orderEditOptionsRequest);

        // Assert
        assertNotNull(orderEditOptionsResponse);
        assertEquals(1, orderEditOptionsResponse.getDeliveryOptions().size());

        List<ServeEvent> events = combinatorMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getUrl().equals("/redelivery-courier-options"))
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());

        assertThat(holder.getAttributes())
                .containsEntry("requestedTypes", ChangeRequestType.DELIVERY_LAST_MILE.toString());

        assertThat(holder.getAttributes())
                .containsEntry("anyDeliveryOption", orderEditOptionsResponse.getDeliveryOptions() != null
                        && !orderEditOptionsResponse.getDeliveryOptions().isEmpty())
                .containsEntry("isCurrentDeliveryOptionActual",
                        orderEditOptionsResponse.getCurrentDeliveryOptionActual());
    }

    @Test
    public void getOrderEditOptions_deliveryLastMile_fromDeliveryToPickup()
            throws JsonProcessingException {
        // Assign
        OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder holder =
                new OrderEditOptionsContextHolder.OrderEditOptionsContextAttributesHolder();
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                emptySet()));
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));
        parameters.setMinifyOutlets(true);
        DeliveryRouteProvider.cleanActualDelivery(parameters.getReportParameters().getActualDelivery());
        final long outletId = 12312303L;
        final String combinatorPickupOptionUrl = "/redelivery_pickup_point_option";

        Order order = orderCreateHelper.createOrder(parameters);

        var combinatorResponse =
                getRedeliveryPickupPointOptionCombinatorResponse(order, outletId, personalDataService);
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo(combinatorPickupOptionUrl))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

        var orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_LAST_MILE));
        var deliveryLastMile = new DeliveryLastMileEditOptionsRequest();
        deliveryLastMile.setOutletId(outletId);
        deliveryLastMile.setDeliveryType(DeliveryType.PICKUP);
        orderEditOptionsRequest.setDeliveryLastMile(deliveryLastMile);

        combinatorMock.resetRequests();

        // Act
        var orderEditOptionsResponse = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                BuyerProvider.UID, List.of(Color.BLUE), orderEditOptionsRequest);

        // Assert
        assertNotNull(orderEditOptionsResponse);
        assertEquals(1, orderEditOptionsResponse.getDeliveryOptions().size());

        List<ServeEvent> events = combinatorMock.getAllServeEvents();
        List<ServeEvent> actualDeliveryCalls = events.stream()
                .filter(se -> se.getRequest().getUrl().equals(combinatorPickupOptionUrl))
                .collect(Collectors.toList());

        assertEquals(1, actualDeliveryCalls.size());

        assertThat(holder.getAttributes())
                .containsEntry("requestedTypes", ChangeRequestType.DELIVERY_LAST_MILE.toString());
        assertThat(holder.getAttributes())
                .containsEntry("requestedDeliveryType", DeliveryType.PICKUP);

        assertThat(holder.getAttributes())
                .containsEntry("anyDeliveryOption", orderEditOptionsResponse.getDeliveryOptions() != null
                        && !orderEditOptionsResponse.getDeliveryOptions().isEmpty())
                .containsEntry("isCurrentDeliveryOptionActual",
                        orderEditOptionsResponse.getCurrentDeliveryOptionActual());
    }

    @Test
    public void lastDeliveryDatesLoggingTest() throws JsonProcessingException {
        Logger logger = (Logger) LoggerFactory.getLogger(Loggers.KEY_VALUE_LOG);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Level oldLevel = logger.getLevel();

        try {
            checkouterProperties.setEnableDeliveryDatesEditOptionsFromCombinator(true);
            checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                    emptySet()));

            Parameters parameters = yaLavkaHelper.buildParametersForDeferredCourier(1);

            Order order = orderCreateHelper.createOrder(parameters);
            assertTrue(OrderTypeUtils.isDeferredCourierDelivery(order));

            var combinatorResponse =
                    getPostponeDeliveryCombinatorResponse(order, personalDataService);
            ObjectMapper mapper = new ObjectMapper();
            combinatorMock.stubFor(
                    post(urlPathEqualTo("/postpone-delivery"))
                            .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

            var orderEditOptionsRequest = new OrderEditOptionsRequest();
            orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_DATES));

            combinatorMock.resetRequests();

            listAppender.start();
            logger.addAppender(listAppender);
            logger.setLevel(Level.INFO);
            // Act
            client.getOrderEditOptions(order.getId(), ClientRole.SHOP,
                    OrderProvider.SHOP_ID, List.of(Color.BLUE), orderEditOptionsRequest);

            List<ILoggingEvent> logsList = listAppender.list;
            assertEquals(1, logsList.size());
            Assertions.assertTrue(logsList.get(0).toString()
                    .contains("\"dates\":{\"toDate\":"));
            logger.detachAppender(listAppender);

        } finally {
            logger.detachAppender(listAppender);
            logger.setLevel(oldLevel);
        }
    }
}

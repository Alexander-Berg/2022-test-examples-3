package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.checkout.common.util.StreamUtils;
import ru.yandex.market.checkout.helpers.TariffDataHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ParcelPatchRequestProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.TariffDataProvider;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.MarketReportSearchService.SHOW_MULTI_SERVICE_INTERVALS_PARAM;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_INTAKE_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class DeliveryOptionChangeRequestTest extends AbstractWebTestBase {

    public static final Duration PACKAGING_TIME = Duration.of(20, ChronoUnit.HOURS);
    private static final Long MOCK_TARIFF_ID = 6789L;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private TariffDataHelper tariffDataHelper;

    private CheckouterOrderHistoryEventsApi orderHistoryEventsApi;
    private Order order;

    private static ActualDeliveryOption nextDayDeliveryOption() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        option.setShipmentDay(0);
        option.setDayFrom(1);
        option.setDayTo(1);
        option.setDeliveryServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID);
        option.setPackagingTime(Duration.ofMinutes(2));
        option.setTimeIntervals(Lists.newArrayList(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0))));
        option.setPaymentMethods(Sets.newHashSet("YANDEX", "CASH_ON_DELIVERY"));
        return option;
    }

    private static ActualDeliveryOption anotherDeliveryServiceDeliveryOption() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        option.setShipmentDay(3);
        option.setDayFrom(4);
        option.setDayTo(5);
        option.setDeliveryServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID);
        option.setTimeIntervals(Lists.newArrayList(
                new DeliveryTimeInterval(LocalTime.of(10, 00), LocalTime.of(14, 00)),
                new DeliveryTimeInterval(LocalTime.of(14, 00), LocalTime.of(18, 00))
        ));
        option.setPaymentMethods(Sets.newHashSet("YANDEX"));
        option.setTariffId(MOCK_TARIFF_ID);
        option.setPackagingTime(PACKAGING_TIME);
        return option;
    }

    @BeforeEach
    public void setUp() {
        orderHistoryEventsApi = client.orderHistoryEvents();

        Parameters orderCreationParameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .buildParameters();
        order = orderCreateHelper.createOrder(orderCreationParameters);
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void notFilteredByDeliveryServiceIdForDeliveryServiceProblemReason() {
        Parameters orderCreationParameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(orderCreationParameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now().minusDays(1))
                        .reason(HistoryEventReason.DELIVERY_SERVICE_PROBLEM)
                        .build());

        resetWireMocks();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(anotherDeliveryServiceDeliveryOption())
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, null);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );

        assertThat(orderEditOptions.getDeliveryOptions(), hasSize(1));
        DeliveryOption deliveryOption = orderEditOptions.getDeliveryOptions().iterator().next();
        assertEquals(MOCK_INTAKE_DELIVERY_SERVICE_ID, deliveryOption.getDeliveryServiceId());
    }

    @Test
    public void shouldPassShowMultiServiceIntervalsForDeliveryServiceProblemRequest() {
        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now().minusDays(1))
                        .reason(HistoryEventReason.DELIVERY_SERVICE_PROBLEM)
                        .build());

        resetWireMocks();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(anotherDeliveryServiceDeliveryOption())
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, null);

        client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );

        List<LoggedRequest> loggedRequests = reportMock.findAll(
                getRequestedFor(anyUrl())
                        .withQueryParam("place", equalTo("actual_delivery")));
        loggedRequests.forEach(r -> {
            log.info(r.toString());
            Assertions.assertEquals(Collections.singletonList("1"),
                    r.getQueryParams().get(SHOW_MULTI_SERVICE_INTERVALS_PARAM).values());
        });
    }

    @Test
    public void shouldCreateAndApplyDeliveryOptionChangeRequest() throws Exception {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        // переводим статус parcel-а в ERROR
        ParcelPatchRequest parcelPatchRequest = ParcelPatchRequestProvider.getStatusUpdateRequest(ParcelStatus.ERROR);
        client.updateParcel(
                order.getId(),
                order.getDelivery().getParcels().iterator().next().getId(),
                parcelPatchRequest,
                ClientRole.SYSTEM,
                null);

        // заполняем tariffData
        tariffDataHelper.putTariffData(order.getId(), TariffDataProvider.getTariffData());

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .reason(HistoryEventReason.DELIVERY_SERVICE_PROBLEM)
                        .build());

        resetWireMocks();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(anotherDeliveryServiceDeliveryOption())
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, null);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );
        assertThat(orderEditOptions.getDeliveryOptions(), hasSize(1));

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        TimeInterval chosenInterval = chosenOption.getTimeIntervalOptions().stream().findFirst().get();
        assertNotEquals(order.getDelivery().getParcels().get(0).getShipmentDate(), chosenOption.getShipmentDate());
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .timeInterval(chosenInterval)
                .shipmentDate(chosenOption.getShipmentDate())
                .deliveryServiceId(chosenOption.getDeliveryServiceId())
                .reason(HistoryEventReason.DELIVERY_SERVICE_PROBLEM)
                .build());

        List<ChangeRequest> changeRequests = client.editOrder(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                singletonList(BLUE),
                orderEditRequest
        );
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        ChangeRequest changeRequest = changeRequests.get(0);
        assertEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus());
        assertEquals(ChangeRequestType.DELIVERY_OPTION, changeRequest.getType());
        assertTrue(changeRequest.getPayload() instanceof DeliveryOptionChangeRequestPayload);
        DeliveryOptionChangeRequestPayload changeRequestPayload =
                (DeliveryOptionChangeRequestPayload) changeRequest.getPayload();
        assertEquals(MOCK_INTAKE_DELIVERY_SERVICE_ID, changeRequestPayload.getDeliveryServiceId());
        assertEquals(HistoryEventReason.DELIVERY_SERVICE_PROBLEM, changeRequestPayload.getReason());

        Order orderAfter = orderService.getOrder(order.getId());

        assertEquals(MOCK_INTAKE_DELIVERY_SERVICE_ID, orderAfter.getDelivery().getDeliveryServiceId());
        assertThat(orderAfter.getDelivery().getDeliveryDates(),
                allOf(
                        hasProperty("fromDate", is(Date.valueOf(chosenOption.getFromDate()))),
                        hasProperty("toDate", is(Date.valueOf(chosenOption.getToDate()))),
                        hasProperty("fromTime", is(chosenInterval.getFromTime())),
                        hasProperty("toTime", is(chosenInterval.getToTime()))
                ));
        assertEquals(MOCK_TARIFF_ID, orderAfter.getDelivery().getTariffId());
        assertNull(orderAfter.getDelivery().getTariffData());

        Parcel parcel = orderAfter.getDelivery().getParcels().iterator().next();
        assertNotNull(parcel.getParcelItems());
        assertFalse(parcel.getParcelItems().isEmpty());
        assertEquals(ParcelStatus.NEW, parcel.getStatus());
        assertEquals(chosenOption.getShipmentDate(), parcel.getShipmentDate());
        assertEquals(chosenOption.getShipmentDate().atStartOfDay(ZoneId.systemDefault()).plus(PACKAGING_TIME),
                parcel.getPackagingTime().atZone(ZoneId.systemDefault()));
        assertEquals(chosenOption.getShipmentDate().atStartOfDay(ZoneId.systemDefault()).toLocalDate(),
                parcel.getShipmentDate());

        OrderHistoryEvents events = orderHistoryEventsApi.getOrderHistoryEvents(
                0,
                20,
                null,
                false,
                null,
                OrderFilter.builder().setRgb(Color.BLUE).build(),
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));

        Optional<OrderHistoryEvent> crCreatedEventOptional = events.getContent().stream()
                .filter(orderHistoryEvent ->
                        orderHistoryEvent.getType() == HistoryEventType.ORDER_CHANGE_REQUEST_CREATED)
                .findFirst();
        assertTrue(crCreatedEventOptional.isPresent());

        OrderHistoryEvent crCreatedEvent = crCreatedEventOptional.get();
        Optional<ChangeRequest> changeRequestOptional =
                StreamUtils.stream(crCreatedEvent.getOrderAfter().getChangeRequests())
                        .filter(cr -> cr.getType() == ChangeRequestType.DELIVERY_OPTION)
                        .findFirst();
        assertTrue(changeRequestOptional.isPresent());
        assertEquals(ChangeRequestStatus.APPLIED, changeRequestOptional.get().getStatus());
        assertEquals(MOCK_INTAKE_DELIVERY_SERVICE_ID,
                crCreatedEvent.getOrderAfter().getDelivery().getDeliveryServiceId());
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void shouldNotChangeOrderWithEqualDateOption() throws Exception {
        setFixedTime(getClock().instant());
        Parameters orderCreationParameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(orderCreationParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        // переводим статус parcel-а в ERROR
        ParcelPatchRequest parcelPatchRequest = ParcelPatchRequestProvider.getStatusUpdateRequest(ParcelStatus.ERROR);
        client.updateParcel(
                order.getId(),
                order.getDelivery().getParcels().iterator().next().getId(),
                parcelPatchRequest,
                ClientRole.SYSTEM,
                null);

        // заполняем tariffData
        tariffDataHelper.putTariffData(order.getId(), TariffDataProvider.getTariffData());

        setFixedTime(getClock().instant().minus(1L, ChronoUnit.DAYS));

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .reason(HistoryEventReason.DELIVERY_SERVICE_PROBLEM)
                        .build());

        resetWireMocks();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(nextDayDeliveryOption())
                .addDelivery(anotherDeliveryServiceDeliveryOption())
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, null);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        TimeInterval chosenInterval = chosenOption.getTimeIntervalOptions().stream().findFirst().get();

        assertThat(chosenOption.getToDate(), is(LocalDate.now(getClock()).plusDays(1)));
        assertThat(chosenOption.getFromDate(), is(LocalDate.now(getClock()).plusDays(1)));

        assertThat(chosenInterval.getFromTime(), is(LocalTime.of(10, 0)));
        assertThat(chosenInterval.getToTime(), is(LocalTime.of(18, 00)));

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .timeInterval(chosenInterval)
                .shipmentDate(chosenOption.getShipmentDate())
                .deliveryServiceId(chosenOption.getDeliveryServiceId())
                .reason(HistoryEventReason.DELIVERY_SERVICE_PROBLEM)
                .build());

        List<ChangeRequest> changeRequests = client.editOrder(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                singletonList(BLUE),
                orderEditRequest
        );

        assertThat(changeRequests, hasSize(1));
        ChangeRequest changeRequest = changeRequests.get(0);

        DeliveryOptionChangeRequestPayload changeRequestPayload =
                (DeliveryOptionChangeRequestPayload) changeRequest.getPayload();
        assertEquals(HistoryEventReason.DELIVERY_SERVICE_PROBLEM, changeRequestPayload.getReason());

        Order orderAfter = orderService.getOrder(order.getId());

        assertEquals(MOCK_INTAKE_DELIVERY_SERVICE_ID, orderAfter.getDelivery().getDeliveryServiceId());
        assertThat(orderAfter.getDelivery().getDeliveryDates(), is(order.getDelivery().getDeliveryDates()));
    }

    private void mockReportPlaceActualDelivery(
            Order order,
            ActualDelivery actualDelivery,
            @Nullable Long deliveryServiceId
    ) {
        ReportGeneratorParameters generatorParameters = new ReportGeneratorParameters(
                order,
                actualDelivery
        );
        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("offers-list", "-_40VqaS9BpXO1qaTtweBA:1;w:1.000;d:10x10x10;p:250;wh:300501;ff:1;ffWh:300501");
        if (deliveryServiceId != null) {
            extraParams.put("deliveryServiceId", Long.toString(deliveryServiceId));
        }
        generatorParameters.setExtraActualDeliveryParams(extraParams);
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, generatorParameters);
    }
}

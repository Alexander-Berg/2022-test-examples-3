package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.validation.order.status.graph.OrderStatusGraph;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemsRemovalChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsStrategyDto;
import ru.yandex.market.checkout.checkouter.order.item.MissingItemsStrategyType;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.util.DateUtils;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.ChangeRequestStatusHelper;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YaLavkaHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.util.report.ShipmentDayAndDateOption;
import ru.yandex.market.checkout.util.yalavka.YaLavkaDeliveryServiceConfigurer;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.PickupOption;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.Color.RED;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.APPLE_PAY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;
import static ru.yandex.market.checkout.helpers.YaLavkaHelper.lavkaOption;
import static ru.yandex.market.checkout.helpers.YaLavkaHelper.normalOption;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_INTAKE_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class OrderEditTest extends AbstractWebTestBase {

    protected ShipmentDayAndDateOption shipmentDayAndDateOption = ShipmentDayAndDateOption.ONLY_DAY;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private ChangeRequestStatusHelper changeRequestStatusHelper;
    @Autowired
    private ChangeRequestService changeRequestService;
    @Autowired
    private OrderStatusGraph orderStatusGraph;
    @Autowired
    private YaLavkaHelper yaLavkaHelper;
    @Autowired
    private YaLavkaDeliveryServiceConfigurer yaLavkaDSConfigurer;

    private static PickupOption validPostOption() {
        PickupOption option = new PickupOption();
        option.setShipmentDay(0);
        option.setDayFrom(1);
        option.setDayTo(2);
        option.setDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID);
        option.setPaymentMethods(Sets.newHashSet("YANDEX", "CASH_ON_DELIVERY"));
        option.setOutletIds(Collections.singletonList(DeliveryProvider.POST_OUTLET_ID));
        return option;
    }

    private static PickupOption invalidPostOption() {
        PickupOption option = new PickupOption();
        option.setShipmentDay(1);
        option.setDayFrom(3);
        option.setDayTo(3);
        option.setDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID);
        option.setPaymentMethods(Sets.newHashSet("YANDEX", "CASH_ON_DELIVERY"));
        option.setOutletIds(Lists.newArrayList(1L, 2L));
        return option;
    }

    private static ActualDeliveryOption validPostpaidOnlyDeliveryOption() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        option.setDayFrom(3);
        option.setDayTo(3);
        option.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        option.setTimeIntervals(Lists.newArrayList(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0)),
                new DeliveryTimeInterval(LocalTime.of(14, 0), LocalTime.of(18, 0))
        ));
        option.setShipmentDay(1);
        option.setPaymentMethods(Sets.newHashSet("CASH_ON_DELIVERY", "CARD_ON_DELIVERY"));
        return option;
    }

    @BeforeEach
    public void before() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    @AfterEach
    public void cleanup() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(false,
                Collections.<String>emptySet()));
    }

    @Test
    public void orderEditOptionsForDeliveryOrderFromUnpaid() throws Exception {
        setFixedTime(getClock().instant());
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withPartnerInterface(true)
                .build();

        assertEquals(LocalDate.now().plusDays(1), order.getDelivery().getParcels().get(0).getShipmentDate());
        assertNull(order.getDelivery().getParcels().get(0).getDelayedShipmentDate());
        assertEquals(OrderStatus.UNPAID, order.getStatus());

        setFixedTime(getClock().instant().plus(3, ChronoUnit.DAYS));

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                        .build());

        resetWireMocks();
        ActualDeliveryOption actualDeliveryOption = validDeliveryOption();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(anotherValidDeliveryOption())
                .addDelivery(actualDeliveryOption)
                .addDelivery(invalidDeliveryOption())
                .addDelivery(anotherInvalidDeliveryOption())
                .addPickup(MOCK_DELIVERY_SERVICE_ID)
                .addPost(1)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditOptionsRequest);

        assertThat(orderEditOptions, hasProperty("deliveryOptions",
                contains(
                        allOf(
                                hasProperty("deliveryServiceId", is(MOCK_DELIVERY_SERVICE_ID)),
                                hasProperty("fromDate", is(LocalDate.now(getClock()).plusDays(1))),
                                hasProperty("toDate", is(LocalDate.now(getClock()).plusDays(2))),
                                hasProperty("timeIntervalOptions", contains(
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(10, 0))),
                                                hasProperty("toTime", is(LocalTime.of(18, 0)))
                                        )
                                ))
                        ),
                        allOf(
                                hasProperty("deliveryServiceId", is(MOCK_DELIVERY_SERVICE_ID)),
                                hasProperty("fromDate", is(LocalDate.now(getClock()).plusDays(3))),
                                hasProperty("toDate", is(LocalDate.now(getClock()).plusDays(3))),
                                hasProperty("timeIntervalOptions", contains(
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(10, 0))),
                                                hasProperty("toTime", is(LocalTime.of(14, 0)))
                                        ),
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(14, 0))),
                                                hasProperty("toTime", is(LocalTime.of(18, 0)))
                                        )
                                ))
                        )
                )));

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        TimeInterval chosenInterval = chosenOption.getTimeIntervalOptions().iterator().next();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .timeInterval(chosenInterval)
                .reason(HistoryEventReason.SHIPPING_DELAYED)
                .build());

        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        assertThat(changeRequests.get(0), hasProperty("status", is(ChangeRequestStatus.APPLIED)));

        order = orderService.getOrder(order.getId());
        assertThat(order.getDelivery().getDeliveryDates(),
                allOf(
                        hasProperty("fromDate", is(Date.valueOf(chosenOption.getFromDate()))),
                        hasProperty("toDate", is(Date.valueOf(chosenOption.getToDate()))),
                        hasProperty("fromTime", is(chosenInterval.getFromTime())),
                        hasProperty("toTime", is(chosenInterval.getToTime()))
                ));
        assertEquals(LocalDate.now(getClock()).minusDays(2), order.getDelivery().getParcels().get(0)
                .getShipmentDate());
        assertEquals(LocalDate.now(getClock()).minusDays(1), order.getDelivery().getParcels().get(0)
                .getDelayedShipmentDate());

        assertThat(
                eventsGetHelper.getOrderHistoryEvents(order.getId()).getItems(),
                hasItem(hasProperty("reason", equalTo(HistoryEventReason.SHIPPING_DELAYED))));

        orderEditRequest.getDeliveryEditRequest().setTimeInterval(null);
        try {
            client.editOrder(order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE),
                    orderEditRequest);
            Assertions.fail("Exception should be thrown");
        } catch (ErrorCodeException ignored) {

        }
    }

    @Test
    public void orderEditOptionsForDeliveryOrderFromProcessing() throws Exception {
        orderEditOptionsForDeliveryOrder(OrderStatus.PROCESSING);
    }

    @Test
    public void orderEditOptionsForDeliveryOrderFromDelivery() throws Exception {
        orderEditOptionsForDeliveryOrder(OrderStatus.DELIVERY);
    }

    // TODO: Добавлено в рамках https://st.yandex-team.ru/MARKETCHECKOUT-16557. Выпилить, когда перестанет быть нужным
    @Test
    public void testCombinatorFlagsShouldNotPassToActualDelivery() {
        createAndEditCombinatorOrder(OrderStatus.PROCESSING);

        List<ServeEvent> actualDeliveryCalls = reportMock.getServeEvents().getRequests().stream()
                .filter(r -> r.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery"))
                .collect(Collectors.toList());
        assertFalse(actualDeliveryCalls.isEmpty());
        var flagMissed = actualDeliveryCalls.stream()
                .allMatch(r -> r.getRequest().getQueryParams().get("combinator").values().equals(singletonList("0")));
        assertTrue(flagMissed);
    }

    private void createAndEditCombinatorOrder(OrderStatus orderStatus) {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                Collections.<String>emptySet()));
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                .withCombinator(true)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, orderStatus);

        setFixedTime(getClock().instant().plus(3, ChronoUnit.DAYS));

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                        .build()
        );

        resetWireMocks();
        ActualDeliveryOption actualDeliveryOption = validDeliveryOption();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(anotherValidDeliveryOption())
                .addDelivery(actualDeliveryOption)
                .addDelivery(invalidDeliveryOption())
                .addDelivery(anotherInvalidDeliveryOption())
                .addPickup(MOCK_DELIVERY_SERVICE_ID)
                .addPost(1)
                .build();
        // до процессинга не фильтруем комбинаторный заказ по СД
        boolean afterProcessing = orderStatusGraph.compareStatus(orderStatus).isAfterOrEqual(OrderStatus.PROCESSING);
        mockReportPlaceActualDelivery(order, actualDelivery, afterProcessing ? MOCK_DELIVERY_SERVICE_ID : null);

        client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );
    }

    private void orderEditOptionsForDeliveryOrder(OrderStatus orderStatus) throws Exception {
        setFixedTime(getClock().instant());
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        assertEquals(LocalDate.now().plusDays(1), order.getDelivery().getParcels().get(0).getShipmentDate());
        assertNull(order.getDelivery().getParcels().get(0).getDelayedShipmentDate());
        orderStatusHelper.proceedOrderToStatus(order, orderStatus);

        setFixedTime(getClock().instant().plus(3, ChronoUnit.DAYS));

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                        .build());

        resetWireMocks();
        ActualDeliveryOption actualDeliveryOption = validDeliveryOption();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(anotherValidDeliveryOption())
                .addDelivery(actualDeliveryOption)
                .addDelivery(invalidDeliveryOption())
                .addDelivery(anotherInvalidDeliveryOption())
                .addPickup(MOCK_DELIVERY_SERVICE_ID)
                .addPost(1)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE),
                orderEditOptionsRequest);

        assertThat(orderEditOptions.getCurrentDeliveryOptionActual(), is(false));
        assertThat(orderEditOptions, hasProperty("deliveryOptions",
                contains(
                        allOf(
                                hasProperty("fromDate", is(LocalDate.now(getClock()).plusDays(1))),
                                hasProperty("toDate", is(LocalDate.now(getClock()).plusDays(2))),
                                hasProperty("timeIntervalOptions", contains(
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(10, 0))),
                                                hasProperty("toTime", is(LocalTime.of(18, 0)))
                                        )
                                ))
                        ),
                        allOf(
                                hasProperty("fromDate", is(LocalDate.now(getClock()).plusDays(3))),
                                hasProperty("toDate", is(LocalDate.now(getClock()).plusDays(3))),
                                hasProperty("timeIntervalOptions", contains(
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(10, 0))),
                                                hasProperty("toTime", is(LocalTime.of(14, 0)))
                                        ),
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(14, 0))),
                                                hasProperty("toTime", is(LocalTime.of(18, 0)))
                                        )
                                ))
                        )
                )));

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        TimeInterval chosenInterval = chosenOption.getTimeIntervalOptions().iterator().next();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .timeInterval(chosenInterval)
                .reason(HistoryEventReason.SHIPPING_DELAYED)
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
        assertThat(changeRequests.get(0), hasProperty("status", is(ChangeRequestStatus.NEW)));

        order = orderService.getOrder(order.getId());
        assertThat(order.getDelivery().getDeliveryDates(),
                not(allOf(
                        hasProperty("fromDate", is(Date.valueOf(chosenOption.getFromDate()))),
                        hasProperty("toDate", is(Date.valueOf(chosenOption.getToDate()))),
                        hasProperty("fromTime", is(chosenInterval.getFromTime())),
                        hasProperty("toTime", is(chosenInterval.getToTime()))
                )));
        assertEquals(LocalDate.now(getClock()).minusDays(2), order.getDelivery().getParcels().get(0).getShipmentDate());


        orderEditRequest.getDeliveryEditRequest().setTimeInterval(null);
        try {
            client.editOrder(order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE),
                    orderEditRequest);
            Assertions.fail("Exception should be thrown");
        } catch (ErrorCodeException ignored) {

        }

        changeRequestStatusHelper.proceedToStatus(order, changeRequests.get(0), ChangeRequestStatus.APPLIED);

        order = client.getOrder(order.getId(), ClientRole.SYSTEM, 0L, Sets.newHashSet(OptionalOrderPart
                .CHANGE_REQUEST));
        assertThat(order.getDelivery().getDeliveryDates(),
                allOf(
                        hasProperty("fromDate", is(Date.valueOf(chosenOption.getFromDate()))),
                        hasProperty("toDate", is(Date.valueOf(chosenOption.getToDate()))),
                        hasProperty("fromTime", is(chosenInterval.getFromTime())),
                        hasProperty("toTime", is(chosenInterval.getToTime()))
                ));

        assertEquals(LocalDate.now(getClock()).minusDays(2), order.getDelivery().getParcels().get(0)
                .getShipmentDate());
        assertEquals(LocalDate.now(getClock()).minusDays(1), order.getDelivery().getParcels().get(0)
                .getDelayedShipmentDate());
        assertThat(order.getChangeRequests(), contains(hasProperty("status",
                is(ChangeRequestStatus.APPLIED))));

        assertThat(
                eventsGetHelper.getOrderHistoryEvents(order.getId()).getItems(),
                hasItem(hasProperty("reason", equalTo(HistoryEventReason.SHIPPING_DELAYED))));
    }

    @Test
    public void orderEditOptionsNotFilteredForApplePay() throws Exception {
        Parameters orderCreationParameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .buildParameters();
        orderCreationParameters.setPaymentMethod(APPLE_PAY);
        Order orderWithApplePay = orderCreateHelper.createOrder(orderCreationParameters);

        orderStatusHelper.proceedOrderToStatus(orderWithApplePay, OrderStatus.PROCESSING);

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now().minusDays(1))
                        .build());

        resetWireMocks();
        ActualDeliveryOption validOption = validDeliveryOption();
        validOption.setDayFrom(3);
        validOption.setDayTo(3);
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(validOption)
                .addDelivery(validPostpaidOnlyDeliveryOption())
                .build();
        mockReportPlaceActualDelivery(orderWithApplePay, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                orderWithApplePay.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );

        assertThat(orderEditOptions, hasProperty("deliveryOptions",
                contains(
                        allOf(
                                hasProperty("fromDate", is(LocalDate.now(getClock()).plusDays(3))),
                                hasProperty("toDate", is(LocalDate.now(getClock()).plusDays(3))),
                                hasProperty("timeIntervalOptions", contains(
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(10, 0))),
                                                hasProperty("toTime", is(LocalTime.of(18, 0)))
                                        )
                                ))
                        )
                )));

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        TimeInterval chosenInterval = chosenOption.getTimeIntervalOptions().iterator().next();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .timeInterval(chosenInterval)
                .reason(HistoryEventReason.SHIPPING_DELAYED)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(
                orderWithApplePay.getId(),
                ClientRole.SYSTEM,
                null,
                singletonList(BLUE),
                orderEditRequest
        );
        assertNotNull(changeRequests);

        changeRequestStatusHelper.proceedToStatus(orderWithApplePay, changeRequests.get(0), ChangeRequestStatus
                .APPLIED);

        orderWithApplePay = orderService.getOrder(orderWithApplePay.getId());
        assertThat(orderWithApplePay.getDelivery().getDeliveryDates(),
                allOf(
                        hasProperty("fromDate", is(DeliveryDates.daysOffsetToDate(getClock(), 3))),
                        hasProperty("toDate", is(DeliveryDates.daysOffsetToDate(getClock(), 3))),
                        hasProperty("fromTime", is(LocalTime.of(10, 0))),
                        hasProperty("toTime", is(LocalTime.of(18, 0)))
                ));
        assertThat(
                eventsGetHelper.getOrderHistoryEvents(orderWithApplePay.getId()).getItems(),
                hasItem(hasProperty("reason", equalTo(HistoryEventReason.SHIPPING_DELAYED))));

        orderEditRequest.getDeliveryEditRequest().setTimeInterval(null);
        try {
            client.editOrder(
                    orderWithApplePay.getId(),
                    ClientRole.SYSTEM,
                    null,
                    Collections.singletonList(BLUE),
                    orderEditRequest
            );
            Assertions.fail("Exception should be thrown");
        } catch (ErrorCodeException ignored) {
        }
    }

    @Test
    public void orderEditOptionsFilteredOptionFromPast() {
        Parameters orderCreationParameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .buildParameters();
        orderCreationParameters.setPaymentMethod(APPLE_PAY);
        Order order = orderCreateHelper.createOrder(orderCreationParameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now().minusDays(1))
                        .build());

        resetWireMocks();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(deliveryOptionFromPast())
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );

        assertThat(orderEditOptions.getDeliveryOptions(), hasSize(0));
    }

    @Test
    void shouldReturnOptionsForPreorder() {
        Order order = OrderProvider.getBlueOrder(o -> {
            o.getItems().forEach(oi -> oi.setPreorder(true));
        });

        Parameters orderCreationParameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withOrder(order)
                .buildParameters();
        orderCreationParameters.setPaymentMethod(APPLE_PAY);
        orderCreationParameters.getReportParameters().getActualDelivery().getResults().get(0)
                .getDelivery().forEach(o -> o.setTimeIntervals(null));
        order = orderCreateHelper.createOrder(orderCreationParameters);

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now().minusDays(1))
                        .build());

        resetWireMocks();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(validDeliveryOption())
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );

        assertThat(orderEditOptions.getDeliveryOptions(), hasSize(greaterThan(0)));
    }

    @Test
    public void orderEditOptionsFilteredOptionBeforeDateChosenAtCheckoutOrder() {
        Parameters orderCreationParameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .buildParameters();
        orderCreationParameters.setPaymentMethod(APPLE_PAY);
        orderCreationParameters.getReportParameters().getActualDelivery().getResults().get(0)
                .getDelivery().forEach(o -> o.setTimeIntervals(null));
        Order order = orderCreateHelper.createOrder(orderCreationParameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now().minusDays(1))
                        .build());

        resetWireMocks();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(validDeliveryOption())
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );

        assertThat(orderEditOptions.getDeliveryOptions(), hasSize(0));
    }

    @Test
    public void shouldReturnEditOptionsForSystemRole() {
        setFixedTime(getClock().instant());
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID)
                .build();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now().minusDays(1))
                        .build());

        resetWireMocks();
        ActualDeliveryOption validDeliveryOption = validDeliveryOption();
        validDeliveryOption.setDayTo(3);
        validDeliveryOption.setDayFrom(3);
        validDeliveryOption.setDeliveryServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID);
        ActualDeliveryOption anotherValidDeliveryOption = anotherValidDeliveryOption();
        anotherValidDeliveryOption.setDeliveryServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID);
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(validDeliveryOption)
                .addDelivery(anotherValidDeliveryOption)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_INTAKE_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                Collections.singletonList(BLUE),
                orderEditOptionsRequest
        );

        assertThat(orderEditOptions.getDeliveryOptions(), hasSize(2));
    }

    private void mockReportPlaceActualDelivery(
            Order order,
            ActualDelivery actualDelivery,
            @Nullable Long deliveryServiceId
    ) {
        mockReportPlaceActualDelivery(order, actualDelivery, deliveryServiceId, -1);
    }

    private void mockReportPlaceActualDelivery(
            Order order,
            ActualDelivery actualDelivery,
            @Nullable Long deliveryServiceId,
            int shipmentDay
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
        if (order.getStatus() != OrderStatus.UNPAID && order.getStatus() != OrderStatus.PENDING) {
            extraParams.put("allow-disabled-lms-relations", "1");
            extraParams.put("inlet-shipment-day", String.valueOf(shipmentDay));
        }
        generatorParameters.setExtraActualDeliveryParams(extraParams);
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, generatorParameters);
    }

    private ActualDeliveryOption nextDayDeliveryOption() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        setShipmentDayAndDate(option, 0);
        option.setDayFrom(1);
        option.setDayTo(1);
        option.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        option.setPackagingTime(Duration.ofMinutes(2));
        option.setTimeIntervals(Lists.newArrayList(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0))));
        option.setPaymentMethods(Sets.newHashSet("YANDEX", "CASH_ON_DELIVERY"));
        return option;
    }

    private ActualDeliveryOption validDeliveryOption() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        setShipmentDayAndDate(option, 0);
        option.setDayFrom(1);
        option.setDayTo(2);
        option.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        option.setPackagingTime(Duration.ofMinutes(2));
        option.setTimeIntervals(Lists.newArrayList(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0))));
        option.setPaymentMethods(Sets.newHashSet("YANDEX", "CASH_ON_DELIVERY"));
        return option;
    }

    private ActualDeliveryOption anotherValidDeliveryOption() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        setShipmentDayAndDate(option, 1);
        option.setDayFrom(3);
        option.setDayTo(3);
        option.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        option.setTimeIntervals(Lists.newArrayList(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0)),
                new DeliveryTimeInterval(LocalTime.of(14, 0), LocalTime.of(18, 0))
        ));
        option.setPaymentMethods(Sets.newHashSet("YANDEX"));
        return option;
    }

    private ActualDeliveryOption invalidDeliveryOption() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        setShipmentDayAndDate(option, 1);
        option.setDayFrom(4);
        option.setDayTo(4);
        option.setDeliveryServiceId(123456789L);
        option.setTimeIntervals(Lists.newArrayList(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0))));
        option.setPaymentMethods(Sets.newHashSet("YANDEX", "CASH_ON_DELIVERY"));
        return option;
    }

    private ActualDeliveryOption deliveryOptionFromPast() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        setShipmentDayAndDate(option, -3);
        option.setDayFrom(-2);
        option.setDayTo(-1);
        option.setDeliveryServiceId(123456789L);
        option.setTimeIntervals(Lists.newArrayList(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0))));
        option.setPaymentMethods(Sets.newHashSet("YANDEX", "CASH_ON_DELIVERY"));
        return option;
    }

    private ActualDeliveryOption anotherInvalidDeliveryOption() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        setShipmentDayAndDate(option, 1);
        option.setDayFrom(4);
        option.setDayTo(4);
        option.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        option.setTimeIntervals(Lists.newArrayList(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0))));
        option.setPaymentMethods(Sets.newHashSet("CASH_ON_DELIVERY"));
        return option;
    }

    private void setShipmentDayAndDate(ActualDeliveryOption option, int shipmentDay) {
        shipmentDayAndDateOption.setupDeliveryOption(option, getClock(), shipmentDay);
    }

    @Test
    public void orderEditOptionsForPickupOrder() throws Exception {
        setFixedTime(getClock().instant());
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        setFixedTime(getClock().instant().plus(3, ChronoUnit.DAYS));

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                        .build());

        resetWireMocks();
        PickupOption pickupOption = new PickupOption();
        pickupOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        pickupOption.setShipmentDay(3);
        pickupOption.setDayFrom(5);
        pickupOption.setDayTo(6);
        pickupOption.setOutletIds(Collections.singletonList(order.getDelivery().getOutletId()));
        pickupOption.setPaymentMethods(Collections.singleton(YANDEX.toString()));

        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(MOCK_DELIVERY_SERVICE_ID)
                .addPickup(MOCK_DELIVERY_SERVICE_ID, 0, Lists.newArrayList(-1L))
                .addPickup(123456789L)
                .addPickup(pickupOption)
                .addPost(1)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditOptionsRequest);

        assertThat(orderEditOptions, hasProperty("deliveryOptions",
                contains(allOf(
                        hasProperty("timeIntervalOptions", nullValue()),
                        hasProperty("fromDate", is(LocalDate.now(getClock()).plusDays(5))),
                        hasProperty("toDate", is(LocalDate.now(getClock()).plusDays(6)))
                ))));

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .reason(HistoryEventReason.SHIPPING_DELAYED)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        assertThat(changeRequests.get(0), hasProperty("status", is(ChangeRequestStatus.NEW)));

        order = orderService.getOrder(order.getId());
        assertThat(order.getDelivery().getDeliveryDates(),
                not(allOf(
                        hasProperty("fromDate", is(DeliveryDates.daysOffsetToDate(getClock(), 5))),
                        hasProperty("toDate", is(DeliveryDates.daysOffsetToDate(getClock(), 6))),
                        hasProperty("fromTime", nullValue()),
                        hasProperty("toTime", nullValue())
                )));

        changeRequestStatusHelper.proceedToStatus(order, changeRequests.get(0), ChangeRequestStatus.APPLIED);

        order = client.getOrder(order.getId(), ClientRole.SYSTEM, 0L, Sets.newHashSet(OptionalOrderPart
                .CHANGE_REQUEST));
        assertThat(order.getDelivery().getDeliveryDates(),
                allOf(
                        hasProperty("fromDate", is(Date.valueOf(chosenOption.getFromDate()))),
                        hasProperty("toDate", is(Date.valueOf(chosenOption.getToDate()))),
                        hasProperty("fromTime", is(nullValue())),
                        hasProperty("toTime", is(nullValue()))
                ));

        assertThat(order.getChangeRequests(),
                contains(hasProperty("status", is(ChangeRequestStatus.APPLIED))));

        assertThat(
                eventsGetHelper.getOrderHistoryEvents(order.getId()).getItems(),
                hasItem(hasProperty("reason", equalTo(HistoryEventReason.SHIPPING_DELAYED))));
    }

    @Test
    public void orderEditOptionsForPostOrder() throws Exception {
        setFixedTime(getClock().instant());
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.POST)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        setFixedTime(getClock().instant().plus(3, ChronoUnit.DAYS));

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                        .build());

        resetWireMocks();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(MOCK_DELIVERY_SERVICE_ID)
                .addPickup(MOCK_DELIVERY_SERVICE_ID)
                .addPost(validPostOption())
                .addPost(invalidPostOption())
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, RUSPOST_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditOptionsRequest);

        assertThat(orderEditOptions, hasProperty("deliveryOptions",
                contains(
                        allOf(
                                hasProperty("fromDate", is(LocalDate.now(getClock()).plusDays(1))),
                                hasProperty("toDate", is(LocalDate.now(getClock()).plusDays(2))),
                                hasProperty("timeIntervalOptions", nullValue())
                        )
                )));

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .reason(HistoryEventReason.SHIPPING_DELAYED)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        assertThat(changeRequests.get(0), hasProperty("status", is(ChangeRequestStatus.NEW)));

        order = orderService.getOrder(order.getId());
        assertThat(order.getDelivery().getDeliveryDates(),
                not(allOf(
                        hasProperty("fromDate", is(DeliveryDates.daysOffsetToDate(getClock(), 1))),
                        hasProperty("toDate", is(DeliveryDates.daysOffsetToDate(getClock(), 2))),
                        hasProperty("fromTime", nullValue()),
                        hasProperty("toTime", nullValue())
                )));

        changeRequestStatusHelper.proceedToStatus(order, changeRequests.get(0), ChangeRequestStatus.APPLIED);

        order = client.getOrder(order.getId(), ClientRole.SYSTEM, 0L, Sets.newHashSet(OptionalOrderPart
                .CHANGE_REQUEST));
        assertThat(order.getDelivery().getDeliveryDates(),
                allOf(
                        hasProperty("fromDate", is(Date.valueOf(chosenOption.getFromDate()))),
                        hasProperty("toDate", is(Date.valueOf(chosenOption.getToDate()))),
                        hasProperty("fromTime", is(nullValue())),
                        hasProperty("toTime", is(nullValue()))
                ));

        assertThat(order.getChangeRequests(),
                contains(hasProperty("status", is(ChangeRequestStatus.APPLIED))));

        assertThat(
                eventsGetHelper.getOrderHistoryEvents(order.getId()).getItems(),
                hasItem(hasProperty("reason", equalTo(HistoryEventReason.SHIPPING_DELAYED))));
    }

    @Test
    public void testColorAccess() {
        Assertions.assertThrows(OrderNotFoundException.class, () -> {
            Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                    .withDeliveryType(DeliveryType.DELIVERY)
                    .withColor(BLUE)
                    .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                    .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                    .build();

            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

            OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
            orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                    DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                            .shipmentDate(LocalDate.now().minusDays(1))
                            .build());

            client.getOrderEditOptions(
                    order.getId(), ClientRole.SYSTEM, null,
                    Collections.singletonList(RED), orderEditOptionsRequest);
        });
    }

    @Test
    public void shouldntMatchOptionsOnSpecificReasons() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        resetWireMocks();

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(5))
                .toDate(LocalDate.now(getClock()).plusDays(5))
                .reason(HistoryEventReason.DELIVERY_SERVICE_DELAYED)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        assertThat(changeRequests.get(0), hasProperty("status", is(ChangeRequestStatus.APPLIED)));

        orderEditRequest.getDeliveryEditRequest().setReason(HistoryEventReason.USER_MOVED_DELIVERY_DATES_BY_DS);
        List<ChangeRequest> otherChangeRequest = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(otherChangeRequest);

        orderEditRequest.getDeliveryEditRequest().setReason(HistoryEventReason.ROUTE_RECALCULATION);
        List<ChangeRequest> routeRecalculationChangeRequest = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(routeRecalculationChangeRequest);

        DeliveryDates dates = orderService.getOrder(order.getId()).getDelivery().getDeliveryDates();
        orderEditRequest.setDeliveryEditRequest(
                DeliveryEditRequest.newDeliveryEditRequest()
                        .fromDate(DateUtils.dateToLocalDate(dates.getFromDate(), TestableClock.systemDefaultZone()))
                        .toDate(DateUtils.dateToLocalDate(dates.getToDate(), TestableClock.systemDefaultZone()))
                        .timeInterval(new TimeInterval(LocalTime.of(13, 0), LocalTime.of(14, 0)))
                        .reason(HistoryEventReason.DELIVERY_TIME_INTERVAL_CLARIFIED)
                        .build());
        List<ChangeRequest> timeIntervalClarifiedChangeRequest = client.editOrder(order.getId(), ClientRole.SYSTEM,
                null, singletonList(BLUE), orderEditRequest);
        assertNotNull(timeIntervalClarifiedChangeRequest);

        orderEditRequest.getDeliveryEditRequest().setReason(HistoryEventReason.SHIPPING_DELAYED);
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder().build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        try {
            client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                    Collections.singletonList(BLUE), orderEditRequest);
            Assertions.fail(new InvalidRequestException("No matched delivery edit option"));
        } catch (ErrorCodeException ignored) {

        }
    }

    @Test
    public void shouldntMatchOptionsOnSpecificReasonsByDS() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        resetWireMocks();

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(5))
                .toDate(LocalDate.now(getClock()).plusDays(5))
                .timeInterval(new TimeInterval(LocalTime.of(0, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES_BY_DS)
                .build());

        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        assertThat(changeRequests.get(0), hasProperty("status", is(ChangeRequestStatus.APPLIED)));
    }

    @Test
    public void shouldEditInUnpaidStatus() {
        setFixedTime(getClock().instant());
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.UNPAID);

        setFixedTime(getClock().instant().plus(3, ChronoUnit.DAYS));

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                        .build());

        resetWireMocks();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(MOCK_DELIVERY_SERVICE_ID)
                .addDelivery(validDeliveryOption())
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.USER, BuyerProvider.UID, Collections.singletonList(BLUE),
                orderEditOptionsRequest);

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .timeInterval(chosenOption.getTimeIntervalOptions().iterator().next())
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        assertThat(changeRequests.get(0), hasProperty("status", is(ChangeRequestStatus.APPLIED)));
    }

    @Test
    public void shouldSavePackagingTimeOnCR() {
        setFixedTime(getClock().instant());
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withPackagingTime(Duration.ofMinutes(2))
                .build();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.UNPAID);

        setFixedTime(getClock().instant().plus(3, ChronoUnit.DAYS));
        ActualDeliveryOption validDeliveryOption = validDeliveryOption();
        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(shipmentDayAndDateOption.useDate()
                                ? validDeliveryOption.getShipmentDate()
                                : LocalDate.now(getClock()).plusDays(validDeliveryOption.getShipmentDay()))
                        .build());

        resetWireMocks();


        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(MOCK_DELIVERY_SERVICE_ID)
                .addDelivery(validDeliveryOption)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID, 0);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.USER, BuyerProvider.UID, Collections.singletonList(BLUE),
                orderEditOptionsRequest);

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .shipmentDate(chosenOption.getShipmentDate())
                .deliveryOption(chosenOption)
                .timeInterval(chosenOption.getTimeIntervalOptions().iterator().next())
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        ChangeRequest changeRequest = changeRequests.get(0);
        assertThat(changeRequest, hasProperty("status", is(ChangeRequestStatus.APPLIED)));

        Instant expectedPackagingTime = chosenOption.getShipmentDate().atStartOfDay(ZoneId.systemDefault())
                .plus(validDeliveryOption.getPackagingTime())
                .toInstant();
        assertThat(changeRequest.getPayload(), hasProperty("packagingTime", is(expectedPackagingTime)));
    }

    @Test
    public void shouldReturnDeliveryDatesChangeRequest() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        resetWireMocks();

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(5))
                .toDate(LocalDate.now(getClock()).plusDays(5))
                .reason(HistoryEventReason.DELIVERY_SERVICE_DELAYED)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);

        assertThat(changeRequests, hasSize(1));

        ChangeRequest changeRequest = changeRequests.iterator().next();
        assertNotNull(changeRequest);

        assertEquals(ChangeRequestType.DELIVERY_DATES, changeRequest.getType());
        assertEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus());
    }

    @Test
    public void shouldUseCurrentToDateDeliveryOption() {
        setFixedTime(getClock().instant());
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        setFixedTime(getClock().instant().minus(1L, ChronoUnit.DAYS));

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now(getClock()))
                        .build());

        resetWireMocks();
        ActualDeliveryOption actualDeliveryOption = nextDayDeliveryOption();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(anotherValidDeliveryOption())
                .addDelivery(actualDeliveryOption)
                .addPickup(MOCK_DELIVERY_SERVICE_ID)
                .addPost(1)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditOptionsRequest);

        assertThat(orderEditOptions.getDeliveryOptions().size(), is(2));
        assertThat(orderEditOptions, hasProperty("deliveryOptions",
                contains(
                        allOf(
                                hasProperty("fromDate", is(LocalDate.now(getClock()).plusDays(1))),
                                hasProperty("toDate", is(LocalDate.now(getClock()).plusDays(1))),
                                hasProperty("timeIntervalOptions", contains(
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(10, 0))),
                                                hasProperty("toTime", is(LocalTime.of(18, 0)))
                                        )
                                ))
                        ),
                        allOf(
                                hasProperty("fromDate", is(LocalDate.now(getClock()).plusDays(3))),
                                hasProperty("toDate", is(LocalDate.now(getClock()).plusDays(3))),
                                hasProperty("timeIntervalOptions", contains(
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(10, 0))),
                                                hasProperty("toTime", is(LocalTime.of(14, 0)))
                                        ),
                                        allOf(
                                                hasProperty("fromTime", is(LocalTime.of(14, 0))),
                                                hasProperty("toTime", is(LocalTime.of(18, 0)))
                                        )
                                ))
                        )
                )
        ));
    }

    @Test
    public void shouldReturnChangeRequestsSorted() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        resetWireMocks();

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(5))
                .toDate(LocalDate.now(getClock()).plusDays(5))
                .reason(HistoryEventReason.DELIVERY_SERVICE_DELAYED)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);

        assertThat(changeRequests, hasSize(1));

        ChangeRequest changeRequest = changeRequests.iterator().next();
        assertNotNull(changeRequest);

        assertEquals(ChangeRequestType.DELIVERY_DATES, changeRequest.getType());
        assertEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus());

        orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(7))
                .toDate(LocalDate.now(getClock()).plusDays(7))
                .reason(HistoryEventReason.DELIVERY_SERVICE_DELAYED)
                .build());

        changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE),
                orderEditRequest);
        assertNotNull(changeRequests);

        orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(6))
                .toDate(LocalDate.now(getClock()).plusDays(6))
                .reason(HistoryEventReason.DELIVERY_SERVICE_DELAYED)
                .build());

        changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE),
                orderEditRequest);
        assertNotNull(changeRequests);

        changeRequests = client.getOrder(
                order.getId(),
                ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid(),
                false,
                null,
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST)).getChangeRequests();

        assertThat(changeRequests, hasSize(3));

        List<Instant> createdAtList =
                changeRequests.stream().map(ChangeRequest::getCreatedAt).collect(Collectors.toList());
        ArrayList<Instant> copyList = new ArrayList<>(createdAtList);
        Collections.sort(copyList);
        Collections.reverse(copyList);
        assertEquals(copyList, createdAtList);
    }

    @Test
    public void shouldReturnEmptyChangeRequests() {
        setFixedTime(getClock().instant());
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()))
                .toDate(LocalDate.now(getClock()))
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.SHIPPING_DELAYED)
                .build());

        resetWireMocks();

        setFixedTime(getClock().instant().minus(1L, ChronoUnit.DAYS));
        ActualDeliveryOption actualDeliveryOption = nextDayDeliveryOption();
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(anotherValidDeliveryOption())
                .addDelivery(actualDeliveryOption)
                .addPickup(MOCK_DELIVERY_SERVICE_ID)
                .addPost(1)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);

        assertThat(changeRequests.size(), is(1));
        assertEquals(ChangeRequestType.DELIVERY_DATES, changeRequests.get(0).getType());
        assertEquals(ChangeRequestStatus.NEW, changeRequests.get(0).getStatus());
        Order changedOrder = orderService.getOrder(order.getId());
        assertThat(order.getDelivery().getDeliveryDates(), is(changedOrder.getDelivery().getDeliveryDates()));

        ChangeRequestPatchRequest patchRequest = new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null);
        client.updateChangeRequestStatus(order.getId(), changeRequests.get(0).getId(), ClientRole.SYSTEM, null,
                patchRequest);


        Order currentOrder = orderService.getOrder(order.getId());
        currentOrder.setLastHistoryId(null);
        changedOrder.setLastHistoryId(null);
        compareTwoObjects(changedOrder, currentOrder);

        changeRequests =
                changeRequestService.getChangeRequestsById(Collections.singletonList(changeRequests.get(0).getId()));
        assertThat(changeRequests.size(), is(1));
        assertEquals(ChangeRequestStatus.APPLIED, changeRequests.get(0).getStatus());
    }

    private void compareTwoObjects(Object left, Object right) {
        var gson = new Gson();
        var leftAsMap = (LinkedTreeMap<String, Object>) gson.fromJson(left.toString(), Object.class);
        var rightAsMap = (LinkedTreeMap<String, Object>) gson.fromJson(right.toString(), Object.class);

        leftAsMap.forEach((key, value) -> {
            if (rightAsMap.containsKey(key)) {
                Assertions.assertEquals(value, rightAsMap.get(key), "For key " + key);
            } else {
                Assertions.fail("Expected key " + key + " is absent");
            }
        });

        rightAsMap.forEach((key, value) -> {
            if (leftAsMap.containsKey(key)) {
                Assertions.assertEquals(value, leftAsMap.get(key), "For key " + key);
            } else {
                Assertions.fail("Key " + key + " shall to be  absent");
            }
        });
    }

    @Test
    public void shouldChangeTimeIntervalToNull() {
        LocalTime fromTime = LocalTime.of(14, 0);
        LocalTime toTime = LocalTime.of(18, 0);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
        Iterables.getOnlyElement(
                actualDeliveryResult.getDelivery()
        ).setTimeIntervals(
                singletonList(new DeliveryTimeInterval(fromTime, toTime))
        );
        Order order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        resetWireMocks();

        assertNotNull(order.getDelivery().getDeliveryDates().getFromTime());
        assertNotNull(order.getDelivery().getDeliveryDates().getToTime());

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(5))
                .toDate(LocalDate.now(getClock()).plusDays(5))
                .reason(HistoryEventReason.DELIVERY_SERVICE_DELAYED)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);

        order = orderService.getOrder(order.getId());

        assertNull(order.getDelivery().getDeliveryDates().getFromTime());
        assertNull(order.getDelivery().getDeliveryDates().getToTime());
    }

    @Test
    public void checkForcedDateSent() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withPartnerInterface(true)
                .buildParameters();
//        parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        // на PROCESSING+STARTED шлем дату отгрузки
        checkForcedShipmentDate(order.getId(), order.getDelivery().getParcels().get(0).getShipmentDate());

        order = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PROCESSING, OrderSubstatus.PACKAGING);
        // на PROCESSING+PACKAGING шлем дату отгрузки
        checkForcedShipmentDate(order.getId(), order.getDelivery().getParcels().get(0).getShipmentDate());

        order = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PROCESSING,
                OrderSubstatus.READY_TO_SHIP);
        // на PROCESSING+READY_TO_SHIP шлем дату отгрузки
        checkForcedShipmentDate(order.getId(), order.getDelivery().getParcels().get(0).getShipmentDate());

        order = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PROCESSING, OrderSubstatus.SHIPPED);
        // на PROCESSING+SHIPPED шлем дату отгрузки
        checkForcedShipmentDate(order.getId(), order.getDelivery().getParcels().get(0).getShipmentDate());

        order = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.DELIVERY);
        // на DELIVERY шлем дату отгрузки
        checkForcedShipmentDate(order.getId(), order.getDelivery().getParcels().get(0).getShipmentDate());

        // при переносе даты отгрузки шлем задержанную дату отгрузки
        setFixedTime(getClock().instant().plus(3, ChronoUnit.DAYS));

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setChangeRequestTypes(Set.of(ChangeRequestType.DELIVERY_OPTION));
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                        .build());

        resetWireMocks();
        PickupOption pickupOption = new PickupOption();
        pickupOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        pickupOption.setShipmentDay(3);
        pickupOption.setDayFrom(5);
        pickupOption.setDayTo(6);
        pickupOption.setOutletIds(Collections.singletonList(order.getDelivery().getOutletId()));
        pickupOption.setPaymentMethods(Collections.singleton(YANDEX.toString()));
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addPickup(pickupOption)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditOptionsRequest);

        DeliveryOption chosenOption = orderEditOptions.getDeliveryOptions().iterator().next();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .prerequest(orderEditOptionsRequest.getDeliveryEditOptionsRequest())
                .deliveryOption(chosenOption)
                .reason(HistoryEventReason.SHIPPING_DELAYED)
                .build());
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);

        changeRequestStatusHelper.proceedToStatus(order, changeRequests.get(0), ChangeRequestStatus.APPLIED);

        order = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertNotNull(order.getDelivery().getParcels().get(0).getDelayedShipmentDate());
        checkForcedShipmentDate(order.getId(), order.getDelivery().getParcels().get(0).getDelayedShipmentDate());
    }

    private void checkForcedShipmentDate(long orderId, LocalDate forcedDate) {
        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .build());

        reportMock.resetRequests();
        client.getOrderEditOptions(
                orderId, ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditOptionsRequest);

        List<LoggedRequest> actualDeliveryRequests = getActualDeliveryRequests(forcedDate);
        assertThat(actualDeliveryRequests, hasSize(1));
    }

    private List<LoggedRequest> getActualDeliveryRequests(LocalDate forcedDate) {
        return reportMock.findAll(
                getRequestedFor(anyUrl())
                        .withQueryParam(
                                "place",
                                WireMock.equalTo("actual_delivery")
                        )
                        .withQueryParam(
                                "inlet-shipment-day",
                                forcedDate == null ?
                                        WireMock.absent()
                                        : WireMock.equalTo(Integer.toString(Period.between(LocalDate.now(getClock()),
                                        forcedDate).getDays()))
                        )

        );
    }

    @Test
    public void returnDeliveryActualFlag() {
        freezeTime();
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withPartnerInterface(true)
                .build();
        jumpToFuture(3, ChronoUnit.DAYS);

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now(getClock()).minusDays(1))
                        .build());

        resetWireMocks();
        Instant now = getClock().instant();
        Instant dateFrom = order.getDelivery().getDeliveryDates().getFromDate().toInstant();
        Instant dateTo = order.getDelivery().getDeliveryDates().getToDate().toInstant();
        ActualDeliveryOption actualDeliveryOption = validDeliveryOption();
        ActualDeliveryOption currentDeliveryOption = validDeliveryOption();
        currentDeliveryOption.setDayFrom((int) ChronoUnit.DAYS.between(now, dateFrom));
        currentDeliveryOption.setDayTo((int) ChronoUnit.DAYS.between(now, dateTo));
        currentDeliveryOption.setDeliveryServiceId(order.getDelivery().getDeliveryServiceId());
        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(actualDeliveryOption)
                .addDelivery(currentDeliveryOption)
                .addPickup(MOCK_DELIVERY_SERVICE_ID)
                .addPost(1)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, MOCK_DELIVERY_SERVICE_ID);

        OrderEditOptions orderEditOptions = client.getOrderEditOptions(
                order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditOptionsRequest);
        assertThat(orderEditOptions.getCurrentDeliveryOptionActual(), is(true));
    }

    @Test
    public void getMissingStrategyShouldRemoveItems() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withOrder(OrderProvider.getBlueOrder(it -> {
                    it.getItems().stream().findFirst().get().setCount(100);
                    it.setPaymentType(PaymentType.POSTPAID);
                }))
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        resetWireMocks();

        OrderItem item = order.getItems().stream().findFirst().orElseThrow(IllegalStateException::new);
        Long supplierId = item.getSupplierId();
        String sku = item.getShopSku();

        MissingItemsStrategyDto strategy = client.getMissingStrategy(
                order.getId(),
                new MissingItemsNotification(
                        true,
                        List.of(new ItemInfo(supplierId, sku, 99)),
                        HistoryEventReason.ITEMS_NOT_FOUND
                )
        );

        assertThat(strategy.getType(), is(MissingItemsStrategyType.REMOVE_ITEMS));
        assertThat(strategy.getPayload(), notNullValue());
    }

    @Test
    public void getMissingStrategyShouldCancelOrder() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withPartnerInterface(true)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        resetWireMocks();

        OrderItem item = order.getItems().stream().findFirst().orElseThrow(IllegalStateException::new);
        Long supplierId = item.getSupplierId();
        String sku = item.getShopSku();

        MissingItemsStrategyDto strategy = client.getMissingStrategy(
                order.getId(),
                new MissingItemsNotification(
                        false,
                        List.of(new ItemInfo(supplierId, sku, 0)),
                        HistoryEventReason.ITEMS_NOT_FOUND
                )
        );

        assertThat(strategy.getType(), is(MissingItemsStrategyType.CANCEL_ORDER));
        assertThat(strategy.getPayload(), nullValue());
    }

    @Test
    public void getMissingStrategyShouldNoChangeOrder() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withPartnerInterface(true)
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        resetWireMocks();

        OrderItem item = order.getItems().stream().findFirst().orElseThrow(IllegalStateException::new);
        Long supplierId = item.getSupplierId();
        String sku = item.getShopSku();

        MissingItemsStrategyDto strategy = client.getMissingStrategy(
                order.getId(),
                new MissingItemsNotification(
                        true,
                        List.of(new ItemInfo(supplierId, sku, 1)),
                        HistoryEventReason.ITEMS_NOT_FOUND
                )
        );

        assertThat(strategy.getType(), is(MissingItemsStrategyType.NOTHING_CHANGED));
        assertThat(strategy.getPayload(), nullValue());
    }

    @Test
    public void withYaLavkaDeliveryShouldChangeTimeIntervalToNull() {
        yaLavkaDSConfigurer.configureOrderReservationRequest(HttpStatus.OK);
        var parameters = yaLavkaHelper.buildParameters(true,
                normalOption(0),
                lavkaOption(0)
        );

        var order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(1))
                .toDate(LocalDate.now(getClock()).plusDays(1))
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.SHIPPING_DELAYED)
                .build());

        resetWireMocks();

        var nextDayOption = nextDayDeliveryOption();
        nextDayOption.setDeliveryServiceId(YaLavkaHelper.LAVKA_DELIVERY_SERVICE_ID);
        var actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(nextDayOption)
                .build();
        mockReportPlaceActualDelivery(order, actualDelivery, YaLavkaHelper.LAVKA_DELIVERY_SERVICE_ID);

        var changeRequests = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);

        var patchRequest = new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null);
        client.updateChangeRequestStatus(order.getId(), changeRequests.get(0).getId(), ClientRole.SYSTEM, null,
                patchRequest);

        var changedOrder = orderService.getOrder(order.getId());

        assertNull(changedOrder.getDelivery().getDeliveryDates().getFromTime());
        assertNull(changedOrder.getDelivery().getDeliveryDates().getToTime());
    }

    @Test
    public void missingStrategyShouldRemoveItemsWithCAncellationRequestTest() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withOrder(OrderProvider.getBlueOrder(it -> {
                    it.getItems().stream().findFirst().get().setCount(100);
                    it.setPaymentType(PaymentType.POSTPAID);
                }))
                .build();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        resetWireMocks();

        OrderItem item = order.getItems().stream().findFirst().orElseThrow(IllegalStateException::new);
        Long supplierId = item.getSupplierId();
        String sku = item.getShopSku();

        OrderEditRequest cancelRequest = new OrderEditRequest();
        cancelRequest.setCancellationRequest(
                CancellationRequest.builder().substatus(OrderSubstatus.SHOP_FAILED).build());

        List<ChangeRequest> cancelChangeRequest = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), cancelRequest);

        assertThat(cancelChangeRequest.size(), greaterThan(0));

        OrderEditRequest itemEditRequest = new OrderEditRequest();
        itemEditRequest.setMissingItemsNotification(new MissingItemsNotification(
                true,
                List.of(new ItemInfo(supplierId, sku, 99)),
                HistoryEventReason.ITEMS_NOT_FOUND,
                true
        ));

        List<ChangeRequest> itemChangeRequest = client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), itemEditRequest);

        assertThat(itemChangeRequest, hasSize(1));
        assertThat(itemChangeRequest.get(0).getPayload(), instanceOf(ItemsRemovalChangeRequestPayload.class));

        assertThat(itemChangeRequest.get(0).getStatus(), equalTo(ChangeRequestStatus.NEW));
        assertThat(((ItemsRemovalChangeRequestPayload) itemChangeRequest.get(0).getPayload()).getUpdatedItems()
                .iterator().next().getCount(), equalTo(99));
    }
}

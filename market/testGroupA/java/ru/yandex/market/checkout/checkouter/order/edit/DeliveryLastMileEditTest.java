package ru.yandex.market.checkout.checkouter.order.edit;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryUtils;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryLastMileEditRequest;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryLastMileChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.EditPossibilityWrapper;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.service.combinator.CombinatorUtils;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.CommonRequestBuilder;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.DateDto;
import ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.DeliveryOptionDto;
import ru.yandex.market.checkout.checkouter.service.combinator.redeliverycourieroptions.RedeliveryCourierOptionsRequestFactory;
import ru.yandex.market.checkout.checkouter.service.combinator.redeliverycourieroptions.RedeliveryCourierOptionsResponse;
import ru.yandex.market.checkout.checkouter.service.combinator.redeliverypickuppointoption.RedeliveryPickupPointOptionResponse;
import ru.yandex.market.checkout.checkouter.service.combinator.redeliveryroute.RedeliveryRouteResponse;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.trace.OrderEditContextHolder;
import ru.yandex.market.checkout.checkouter.util.DeliveryUtil;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.SKIP_REDELIVERY_ROUTE_ON_ADDRESS_CHANGE;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.service.combinator.CombinatorUtils.getDeliverySubtype;
import static ru.yandex.market.checkout.checkouter.service.combinator.CombinatorUtils.toCombinatorDeliveryType;
import static ru.yandex.market.checkout.checkouter.service.combinator.commonrequest.CommonRequestBuilder.buildInterval;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.ActualDeliveryBuilder.DEFAULT_INTAKE_SHIPMENT_DAYS;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.ANOTHER_MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class DeliveryLastMileEditTest extends AbstractWebTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long OUTLET_ID_FOR_ANOTHER_MOCK_DELIVERY_SERVICE_ID = 741260;
    private static final long REGION_ID_FOR_ANOTHER_MOCK_DELIVERY_SERVICE_ID = 2;

    @Autowired
    private WireMockServer combinatorMock;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private PersonalDataService personalDataService;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void editDeliveryLastMileTest(boolean skipDeliveryRoute) throws Exception {
        var holder = new OrderEditContextHolder.OrderEditContextAttributesHolder();
        if (skipDeliveryRoute) {
            checkouterFeatureWriter.writeValue(SKIP_REDELIVERY_ROUTE_ON_ADDRESS_CHANGE, true);
        }

        Order order = createDeliveryOrder();

        // can edit delivery_dates BEFORE last mile edit request
        var possibilities = getEditPossibilities(order);
        assertTrue(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));

        final var postponeDays = 1;
        final var postponeHours = 2;

        final var newRoute = "{\"this\":\"is\",\"route\":112}";
        mockCombinatorForEditDeliveryLastMile(order, postponeDays, postponeHours, newRoute);

        var deliveryBefore = order.getDelivery();
        var deliveryDatesBefore = deliveryBefore.getDeliveryDates();

        var fromDate = convertToLocalDate(deliveryDatesBefore
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryDatesBefore
                .getToDate())
                .plusDays(postponeDays);
        var fromTime = deliveryDatesBefore.getFromTime().plusHours(postponeHours);
        var toTime = deliveryDatesBefore.getToTime().plusHours(postponeHours);

        final var deliveryDatesExpected = new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate),
                fromTime, toTime);

        var addressUpdate = createAddressUpdate();
        var expectedBuyerAddress = DeliveryUtil.cloneAddress(order.getDelivery().getBuyerAddress());
        updateAddress(expectedBuyerAddress);
        AddressImpl expectedShopAddress = DeliveryUtil.cloneAddress(expectedBuyerAddress);
        expectedShopAddress.setType(AddressType.SHOP);

        var orderEditRequest = createEditRequestForDeliveryLastMile(order,
                fromDate, toDate, fromTime, toTime, addressUpdate);

        // edit date and address
        var changeRequests = client.editOrder(
                order.getId(),
                RequestClientInfo.builder(ClientRole.USER).withClientId(BuyerProvider.UID).build(),
                singletonList(BLUE), orderEditRequest, null
        );

        // assert KV logs
        assertThat(holder.getAttributes()).containsEntry("isLastMileChange", true);
        assertThat(holder.getAttributes()).containsEntry("editRequestTypes", "DELIVERY_LAST_MILE_EDIT");
        assertThat(holder.getAttributes()).containsEntry("deliveryType", DeliveryType.DELIVERY);
        holder = new OrderEditContextHolder.OrderEditContextAttributesHolder();

        // cannot edit delivery_dates or last mile DURING last mile edit request
        possibilities = getEditPossibilities(order);
        assertFalse(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertLastMileEditPossibilities(possibilities, false);

        // apply change request
        var patchRequest = new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null);
        client.updateChangeRequestStatus(order.getId(), changeRequests.get(0).getId(), ClientRole.SYSTEM, null,
                patchRequest);

        assertEquals(ChangeRequestStatus.APPLIED, patchRequest.getStatus());

        // assert KV logs
        assertThat(holder.getAttributes()).containsEntry("isLastMileChange", true);
        assertThat(holder.getAttributes()).containsEntry("changeRequestStatus", ChangeRequestStatus.APPLIED);
        assertThat(holder.getAttributes()).hasEntrySatisfying("changeRequestProcessingDuration", d -> {
            assertTrue((Long) d > 0L);
        });

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();
        var changeRequest = changeRequests.get(0);
        var payload = (DeliveryLastMileChangeRequestPayload) changeRequest.getPayload();

        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.getId(), Integer.MAX_VALUE);
        var event = orderHistoryEvents.getItems().stream()
                .filter(e -> HistoryEventType.ORDER_CHANGE_REQUEST_CREATED == e.getType())
                .findFirst()
                .orElseGet(null);
        assertNotNull(event);
        var changeRequestsFromEvent = event.getOrderAfter().getChangeRequests();

        // assert order changes
        assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        assertEquals(deliveryDatesExpected, deliveryAfter.getDeliveryDates());
        assertEquals(expectedBuyerAddress, deliveryAfter.getBuyerAddress());
        assertEquals(expectedShopAddress, deliveryAfter.getShopAddress());
        assertEquals(expectedBuyerAddress.getNotes(), order.getNotes());

        // assert change requests
        final var expectedRoute = skipDeliveryRoute ? null : newRoute;
        assertChangeRequestsForEditDeliveryLastMile(changeRequests, order, payload, expectedBuyerAddress,
                orderEditRequest.getDeliveryLastMileEditRequest().getRegionId(), expectedRoute, null, null);
        assertChangeRequestsForEditDeliveryLastMile(changeRequestsFromEvent, order, payload, expectedBuyerAddress,
                orderEditRequest.getDeliveryLastMileEditRequest().getRegionId(), expectedRoute, null, null);

        // route should be cleaned from order viewModels
        var getOrderResult = getOrder(order);
        getOrderResult.andExpect(jsonPath("$.changeRequests[0].payload.route").doesNotExist());

        // can edit delivery_dates AFTER last mile edit request
        possibilities = getEditPossibilities(order);
        assertTrue(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));

        // cannot edit last mile for the 2nd time
        possibilities = getEditPossibilities(order);
        assertLastMileEditPossibilities(possibilities, false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void editDeliveryLastMileTest_fromDeliveryToPickup(boolean skipDeliveryRoute) throws Exception {
        var holder = new OrderEditContextHolder.OrderEditContextAttributesHolder();
        if (skipDeliveryRoute) {
            checkouterFeatureWriter.writeValue(SKIP_REDELIVERY_ROUTE_ON_ADDRESS_CHANGE, true);
        }

        final Order expectedPickupOrder = createPickupOrder();
        final var expectedDeliveryServiceId = expectedPickupOrder.getDelivery().getDeliveryServiceId();
        Order order = createDeliveryOrder();
        expectedPickupOrder.getDelivery().setParcels(order.getDelivery().getParcels());
        expectedPickupOrder.getDelivery().setBalanceOrderId(order.getDelivery().getBalanceOrderId());

        // can edit delivery_dates BEFORE last mile edit request
        var possibilities = getEditPossibilities(order);
        assertTrue(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));

        final var postponeDays = 1;
        final var postponeHours = 2;

        var deliveryBefore = order.getDelivery();
        var deliveryDatesBefore = deliveryBefore.getDeliveryDates();

        var fromDate = convertToLocalDate(deliveryDatesBefore
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryDatesBefore
                .getToDate())
                .plusDays(postponeDays);
        var fromTime = deliveryDatesBefore.getFromTime().plusHours(postponeHours);
        var toTime = deliveryDatesBefore.getToTime().plusHours(postponeHours);

        final var deliveryDatesExpected = new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate),
                fromTime, toTime);
        expectedPickupOrder.getDelivery().setDeliveryDates(deliveryDatesExpected);

        final var newRoute = "{\"this\":\"is\",\"route\":112}";
        final long outletId = OUTLET_ID_FOR_ANOTHER_MOCK_DELIVERY_SERVICE_ID;
        final long newRegionId = REGION_ID_FOR_ANOTHER_MOCK_DELIVERY_SERVICE_ID;
        final long newRegionIdFromCombinator = newRegionId + 1;
        mockCombinatorForEditDeliveryLastMileToPickup(expectedPickupOrder, 0, 0, newRoute, outletId,
                newRegionIdFromCombinator);

        var orderEditRequest = createEditRequestForDeliveryLastMileToPickup(expectedPickupOrder,
                fromDate, toDate, fromTime, toTime, outletId, newRegionId);

        // edit date and address
        var changeRequests = client.editOrder(
                order.getId(),
                RequestClientInfo.builder(ClientRole.USER).withClientId(BuyerProvider.UID).build(),
                singletonList(BLUE), orderEditRequest, null
        );

        // assert KV logs
        assertThat(holder.getAttributes()).containsEntry("isLastMileChange", true);
        assertThat(holder.getAttributes()).containsEntry("editRequestTypes", "DELIVERY_LAST_MILE_EDIT");
        assertThat(holder.getAttributes()).containsEntry("deliveryType", DeliveryType.DELIVERY);
        assertThat(holder.getAttributes()).containsEntry("newDeliveryType", DeliveryType.PICKUP);
        holder = new OrderEditContextHolder.OrderEditContextAttributesHolder();

        // cannot edit delivery_dates or last mile DURING last mile edit request
        possibilities = getEditPossibilities(order);
        assertFalse(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertLastMileEditPossibilities(possibilities, false);

        // apply change request
        var patchRequest = new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null);
        client.updateChangeRequestStatus(order.getId(), changeRequests.get(0).getId(), ClientRole.SYSTEM, null,
                patchRequest);

        assertEquals(ChangeRequestStatus.APPLIED, patchRequest.getStatus());

        // assert KV logs
        assertThat(holder.getAttributes()).containsEntry("isLastMileChange", true);
        assertThat(holder.getAttributes()).containsEntry("changeRequestStatus", ChangeRequestStatus.APPLIED);
        assertThat(holder.getAttributes()).hasEntrySatisfying("changeRequestProcessingDuration", d -> {
            assertTrue((Long) d > 0L);
        });

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();
        var changeRequest = changeRequests.get(0);
        var payload = (DeliveryLastMileChangeRequestPayload) changeRequest.getPayload();

        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.getId(), Integer.MAX_VALUE);
        var event = orderHistoryEvents.getItems().stream()
                .filter(e -> HistoryEventType.ORDER_CHANGE_REQUEST_CREATED == e.getType())
                .findFirst()
                .orElseGet(null);
        assertNotNull(event);
        var changeRequestsFromEvent = event.getOrderAfter().getChangeRequests();

        // assert order changes
        assertEquals(expectedPickupOrder.getDelivery(), deliveryAfter);
        assertNull(order.getNotes());

        // assert change requests
        assertChangeRequestsForEditDeliveryLastMile(changeRequests, order, payload, payload.getAddress(),
                orderEditRequest.getDeliveryLastMileEditRequest().getRegionId(), newRoute,
                orderEditRequest.getDeliveryLastMileEditRequest().getOutletId(), expectedDeliveryServiceId);
        assertChangeRequestsForEditDeliveryLastMile(changeRequestsFromEvent, order, payload, payload.getAddress(),
                orderEditRequest.getDeliveryLastMileEditRequest().getRegionId(), newRoute,
                orderEditRequest.getDeliveryLastMileEditRequest().getOutletId(), expectedDeliveryServiceId);

        // route should be cleaned from order viewModels
        var getOrderResult = getOrder(order);
        getOrderResult.andExpect(jsonPath("$.changeRequests[0].payload.route").doesNotExist());

        // cannot edit delivery_dates AFTER last mile was changed to PICKUP
        possibilities = getEditPossibilities(order);
        assertFalse(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));

        // cannot edit last mile for the 2nd time
        possibilities = getEditPossibilities(order);
        assertLastMileEditPossibilities(possibilities, false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void editDeliveryLastMileTest_fromPickupToDelivery(boolean skipDeliveryRoute) throws Exception {
        var holder = new OrderEditContextHolder.OrderEditContextAttributesHolder();
        if (skipDeliveryRoute) {
            checkouterFeatureWriter.writeValue(SKIP_REDELIVERY_ROUTE_ON_ADDRESS_CHANGE, true);
        }
        var exp = Experiments.of(Experiments.CHANGE_LAST_MILE_FROM_PICKUP,
                Experiments.CHANGE_LAST_MILE_FROM_PICKUP_VALUE);

        Order order = createPickupOrder();
        final Order expectedDeliveryOrder = createDeliveryOrder();
        final var expectedDeliveryServiceId = expectedDeliveryOrder.getDelivery().getDeliveryServiceId();
        expectedDeliveryOrder.getDelivery().setParcels(order.getDelivery().getParcels());
        expectedDeliveryOrder.getDelivery().setBalanceOrderId(order.getDelivery().getBalanceOrderId());

        // cannot edit delivery_dates BEFORE last mile edit from PICKUP to DELIVERY
        var possibilities = getEditPossibilities(order, exp);
        assertFalse(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));

        final var postponeDays = 2;
        final var postponeHours = 3;

        var deliveryDatesBefore = expectedDeliveryOrder.getDelivery().getDeliveryDates();
        var fromDate = convertToLocalDate(deliveryDatesBefore
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryDatesBefore
                .getToDate())
                .plusDays(postponeDays);
        var fromTime = deliveryDatesBefore.getFromTime().plusHours(postponeHours);
        var toTime = deliveryDatesBefore.getToTime().plusHours(postponeHours);

        final var deliveryDatesExpected = new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate),
                fromTime, toTime);
        expectedDeliveryOrder.getDelivery().setDeliveryDates(deliveryDatesExpected);

        final var newRoute = "{\"this\":\"is\",\"route\":112}";
        mockCombinatorForEditDeliveryLastMile(expectedDeliveryOrder, 0, 0, newRoute);

        var addressUpdate = createAddressUpdate();
        var expectedBuyerAddress = DeliveryUtil.cloneAddress(addressUpdate);
        DeliveryUtils.specifyRecipient(expectedBuyerAddress, order.getDelivery());
        AddressImpl expectedShopAddress = DeliveryUtil.cloneAddress(expectedBuyerAddress);
        expectedShopAddress.setType(AddressType.SHOP);

        var orderEditRequest = createEditRequestForDeliveryLastMile(expectedDeliveryOrder,
                fromDate, toDate, fromTime, toTime, addressUpdate);

        // edit date and address
        var changeRequests = client.editOrder(
                order.getId(),
                RequestClientInfo.builder(ClientRole.USER).withClientId(BuyerProvider.UID).build(),
                singletonList(BLUE), orderEditRequest, exp.toExperimentString()
        );

        // assert KV logs
        assertThat(holder.getAttributes()).containsEntry("isLastMileChange", true);
        assertThat(holder.getAttributes()).containsEntry("editRequestTypes", "DELIVERY_LAST_MILE_EDIT");
        assertThat(holder.getAttributes()).containsEntry("deliveryType", DeliveryType.PICKUP);
        assertThat(holder.getAttributes()).containsEntry("newDeliveryType", DeliveryType.DELIVERY);
        holder = new OrderEditContextHolder.OrderEditContextAttributesHolder();

        // cannot edit delivery_dates or last mile DURING last mile edit request
        possibilities = getEditPossibilities(order, exp);
        assertFalse(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertLastMileEditPossibilities(possibilities, false);

        // apply change request
        var patchRequest = new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null);
        client.updateChangeRequestStatus(order.getId(), changeRequests.get(0).getId(), ClientRole.SYSTEM, null,
                patchRequest);

        assertEquals(ChangeRequestStatus.APPLIED, patchRequest.getStatus());

        // assert KV logs
        assertThat(holder.getAttributes()).containsEntry("isLastMileChange", true);
        assertThat(holder.getAttributes()).containsEntry("changeRequestStatus", ChangeRequestStatus.APPLIED);
        assertThat(holder.getAttributes()).hasEntrySatisfying("changeRequestProcessingDuration", d -> {
            assertTrue((Long) d > 0L);
        });

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();
        var changeRequest = changeRequests.get(0);
        var payload = (DeliveryLastMileChangeRequestPayload) changeRequest.getPayload();

        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.getId(), Integer.MAX_VALUE);
        var event = orderHistoryEvents.getItems().stream()
                .filter(e -> HistoryEventType.ORDER_CHANGE_REQUEST_CREATED == e.getType())
                .findFirst()
                .orElseGet(null);
        assertNotNull(event);
        var changeRequestsFromEvent = event.getOrderAfter().getChangeRequests();

        // assert order changes
        assertEquals(expectedDeliveryServiceId, deliveryAfter.getDeliveryServiceId());
        assertEquals(deliveryDatesExpected, deliveryAfter.getDeliveryDates());
        assertEquals(expectedBuyerAddress, deliveryAfter.getBuyerAddress());
        assertEquals(expectedShopAddress, deliveryAfter.getShopAddress());
        assertEquals(expectedBuyerAddress.getNotes(), order.getNotes());

        // assert change requests
        assertChangeRequestsForEditDeliveryLastMile(changeRequests, order, payload, expectedBuyerAddress,
                orderEditRequest.getDeliveryLastMileEditRequest().getRegionId(), newRoute, null,
                expectedDeliveryServiceId);
        assertChangeRequestsForEditDeliveryLastMile(changeRequestsFromEvent, order, payload, expectedBuyerAddress,
                orderEditRequest.getDeliveryLastMileEditRequest().getRegionId(), newRoute, null,
                expectedDeliveryServiceId);

        // route should be cleaned from order viewModels
        var getOrderResult = getOrder(order);
        getOrderResult.andExpect(jsonPath("$.changeRequests[0].payload.route").doesNotExist());

        // can edit delivery_dates AFTER last mile edit request
        possibilities = getEditPossibilities(order, exp);
        assertTrue(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));

        // cannot edit last mile for the 2nd time
        possibilities = getEditPossibilities(order, exp);
        assertLastMileEditPossibilities(possibilities, false);
    }

    @Test
    public void editDeliveryLastMile_withoutAddress_shouldReturn400() throws JsonProcessingException {
        editDeliveryLastMile_shouldReturn400(request ->
            request.setAddress(null)
        );
    }

    @Test
    public void editDeliveryLastMile_withoutRegionId_shouldReturn400() throws JsonProcessingException {
        editDeliveryLastMile_shouldReturn400(request ->
            request.setRegionId(null)
        );
    }

    @Test
    public void editDeliveryLastMile_withoutDeliveryType_shouldReturn400() throws JsonProcessingException {
        editDeliveryLastMile_shouldReturn400(request ->
            request.setDeliveryType(null)
        );
    }

    @Test
    public void editDeliveryLastMile_withoutCity_shouldReturn400() throws JsonProcessingException {
        editDeliveryLastMile_shouldReturn400(request ->
            ((AddressImpl) request.getAddress()).setCity(null)
        );
    }

    @Test
    public void editDeliveryLastMile_withoutTimeInterval_shouldReturn400() throws JsonProcessingException {
        editDeliveryLastMile_shouldReturn400(
                request -> request.setTimeInterval(null)
        );
    }

    private void editDeliveryLastMile_shouldReturn400(Consumer<DeliveryLastMileEditRequest> requestMutator)
            throws JsonProcessingException {
        Order order = createDeliveryOrder();

        final var postponeDays = 1;
        final var postponeHours = 2;

        final var newRoute = "{\"this\":\"is\",\"route\":112}";
        mockCombinatorForEditDeliveryLastMile(order, postponeDays, postponeHours, newRoute);

        var deliveryBefore = order.getDelivery();
        var deliveryDatesBefore = deliveryBefore.getDeliveryDates();

        var fromDate = convertToLocalDate(deliveryDatesBefore
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryDatesBefore
                .getToDate())
                .plusDays(postponeDays);
        var fromTime = deliveryDatesBefore.getFromTime().plusHours(postponeHours);
        var toTime = deliveryDatesBefore.getToTime().plusHours(postponeHours);

        var addressUpdate = createAddressUpdate();
        var expectedBuyerAddress = DeliveryUtil.cloneAddress(order.getDelivery().getBuyerAddress());
        updateAddress(expectedBuyerAddress);
        AddressImpl expectedShopAddress = DeliveryUtil.cloneAddress(expectedBuyerAddress);
        expectedShopAddress.setType(AddressType.SHOP);

        var orderEditRequest = createEditRequestForDeliveryLastMile(order,
                fromDate, toDate, fromTime, toTime, addressUpdate);
        requestMutator.accept(orderEditRequest.getDeliveryLastMileEditRequest());

        // edit date and address
        var errorCodeException = assertThrows(ErrorCodeException.class, () -> {
            client.editOrder(
                    order.getId(),
                    RequestClientInfo.builder(ClientRole.USER).withClientId(BuyerProvider.UID).build(),
                    singletonList(BLUE), orderEditRequest, null
            );
        });
        assertEquals(400, errorCodeException.getStatusCode());
    }

    @Test
    public void editDeliveryLastMileToPickup_withoutOutletId_shouldReturn400() throws JsonProcessingException {
        editDeliveryLastMileToPickup_shouldReturn400(request ->
            request.setOutletId(null)
        );
    }

    @Test
    public void editDeliveryLastMileToPickup_withDifferentOutletId_shouldReturn400() throws JsonProcessingException {
        editDeliveryLastMileToPickup_shouldReturn400(request ->
            request.setOutletId(request.getOutletId() + 1)
        );
    }

    @Test
    public void editDeliveryLastMileToPickup_withoutTimeInterval_shouldReturn400() throws JsonProcessingException {
        editDeliveryLastMileToPickup_shouldReturn400(
                request -> request.setTimeInterval(null)
        );
    }

    private void editDeliveryLastMileToPickup_shouldReturn400(Consumer<DeliveryLastMileEditRequest> requestMutator)
            throws JsonProcessingException {
        var exp = Experiments.of(Experiments.CHANGE_LAST_MILE_FROM_PICKUP,
                Experiments.CHANGE_LAST_MILE_FROM_PICKUP_VALUE);
        var holder = new OrderEditContextHolder.OrderEditContextAttributesHolder();

        final Order expectedPickupOrder = createPickupOrder();
        Order order = createDeliveryOrder();
        expectedPickupOrder.getDelivery().setParcels(order.getDelivery().getParcels());
        expectedPickupOrder.getDelivery().setBalanceOrderId(order.getDelivery().getBalanceOrderId());

        // can edit delivery_dates BEFORE last mile edit request
        var possibilities = getEditPossibilities(order, exp);
        assertTrue(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));

        final var postponeDays = 1;
        final var postponeHours = 2;

        var deliveryBefore = order.getDelivery();
        var deliveryDatesBefore = deliveryBefore.getDeliveryDates();

        var fromDate = convertToLocalDate(deliveryDatesBefore
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryDatesBefore
                .getToDate())
                .plusDays(postponeDays);
        var fromTime = deliveryDatesBefore.getFromTime().plusHours(postponeHours);
        var toTime = deliveryDatesBefore.getToTime().plusHours(postponeHours);

        final var deliveryDatesExpected = new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate),
                fromTime, toTime);
        expectedPickupOrder.getDelivery().setDeliveryDates(deliveryDatesExpected);

        final var newRoute = "{\"this\":\"is\",\"route\":112}";
        final long outletId = OUTLET_ID_FOR_ANOTHER_MOCK_DELIVERY_SERVICE_ID;
        final long newRegionId = deliveryBefore.getRegionId() + 1;
        mockCombinatorForEditDeliveryLastMileToPickup(expectedPickupOrder, 0, 0, newRoute, outletId, newRegionId);

        var orderEditRequest = createEditRequestForDeliveryLastMileToPickup(expectedPickupOrder,
                fromDate, toDate, fromTime, toTime, outletId, newRegionId);

        requestMutator.accept(orderEditRequest.getDeliveryLastMileEditRequest());

        // edit date and address
        var errorCodeException = assertThrows(ErrorCodeException.class, () -> {
            client.editOrder(
                    order.getId(),
                    RequestClientInfo.builder(ClientRole.USER).withClientId(BuyerProvider.UID).build(),
                    singletonList(BLUE), orderEditRequest, null
            );
        });
        assertEquals(400, errorCodeException.getStatusCode());
    }

    @Test
    public void editDeliveryLastMile_forPickupOrder_shouldReturn400() throws JsonProcessingException {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CHANGE_LAST_MILE, true);
        var exp = Experiments.of(Experiments.CHANGE_LAST_MILE_FROM_PICKUP,
                Experiments.CHANGE_LAST_MILE_FROM_PICKUP_VALUE);

        final Order order = createPickupOrder();
        assertEquals(DeliveryType.PICKUP, order.getDelivery().getType());

        var deliveryBefore = order.getDelivery();
        deliveryBefore.getDeliveryDates().setFromTime(LocalTime.of(9, 0));
        deliveryBefore.getDeliveryDates().setToTime(LocalTime.of(18, 0));

        final var postponeDays = 1;
        final var postponeHours = 2;

        var deliveryDatesBefore = deliveryBefore.getDeliveryDates();

        var fromDate = convertToLocalDate(deliveryDatesBefore
                .getFromDate())
                .plusDays(postponeDays);
        var toDate = convertToLocalDate(deliveryDatesBefore
                .getToDate())
                .plusDays(postponeDays);
        var fromTime = deliveryDatesBefore.getFromTime().plusHours(postponeHours);
        var toTime = deliveryDatesBefore.getToTime().plusHours(postponeHours);

        var orderEditRequest = createEditRequestForDeliveryLastMileToPickup(order,
                fromDate, toDate, fromTime, toTime, 1L, deliveryBefore.getRegionId() + 1);

        // edit date and address
        var errorCodeException = assertThrows(ErrorCodeException.class, () -> {
            client.editOrder(
                    order.getId(),
                    RequestClientInfo.builder(ClientRole.USER).withClientId(BuyerProvider.UID).build(),
                    singletonList(BLUE), orderEditRequest, exp.toExperimentString()
            );
        });
        assertEquals(400, errorCodeException.getStatusCode());
        assertEquals("Change deliveryType from PICKUP to PICKUP is not supported.", errorCodeException.getMessage());
    }

    @Test
    public void editDeliveryLastMile_withSameDateTime() throws Exception {
        Order order = createDeliveryOrder();

        // can edit delivery_dates BEFORE last mile edit request
        var possibilities = getEditPossibilities(order);
        assertTrue(possibilities.isPossible(ChangeRequestType.DELIVERY_DATES));


        final var newRoute = "{\"this\":\"is\",\"route\":112}";
        mockCombinatorForEditDeliveryLastMile(order, 0, 0, newRoute);

        var deliveryBefore = order.getDelivery();
        var deliveryDatesBefore = deliveryBefore.getDeliveryDates();

        var fromDate = convertToLocalDate(deliveryDatesBefore.getFromDate());
        var toDate = convertToLocalDate(deliveryDatesBefore.getToDate());
        var fromTime = deliveryDatesBefore.getFromTime();
        var toTime = deliveryDatesBefore.getToTime();

        var addressUpdate = createAddressUpdate();
        var expectedBuyerAddress = DeliveryUtil.cloneAddress(order.getDelivery().getBuyerAddress());
        updateAddress(expectedBuyerAddress);
        AddressImpl expectedShopAddress = DeliveryUtil.cloneAddress(expectedBuyerAddress);
        expectedShopAddress.setType(AddressType.SHOP);

        var orderEditRequest = createEditRequestForDeliveryLastMile(order,
                fromDate, toDate, fromTime, toTime, addressUpdate);

        // edit date and address
        var changeRequests = client.editOrder(
                order.getId(),
                RequestClientInfo.builder(ClientRole.USER).withClientId(BuyerProvider.UID).build(),
                singletonList(BLUE), orderEditRequest, null
        );

        // apply change request
        var patchRequest = new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null);
        client.updateChangeRequestStatus(order.getId(), changeRequests.get(0).getId(), ClientRole.SYSTEM, null,
                patchRequest);

        assertEquals(ChangeRequestStatus.APPLIED, patchRequest.getStatus());

        order = orderService.getOrder(order.getId());

        var deliveryAfter = order.getDelivery();
        var changeRequest = changeRequests.get(0);
        var payload = (DeliveryLastMileChangeRequestPayload) changeRequest.getPayload();

        // assert order changes
        assertEquals(deliveryAfter.getDeliveryServiceId(), deliveryBefore.getDeliveryServiceId());
        assertEquals(deliveryDatesBefore, deliveryAfter.getDeliveryDates());
        assertEquals(expectedBuyerAddress, deliveryAfter.getBuyerAddress());
        assertEquals(expectedShopAddress, deliveryAfter.getShopAddress());
        assertEquals(expectedBuyerAddress.getNotes(), order.getNotes());

        // assert change requests
        assertChangeRequestsForEditDeliveryLastMile(changeRequests, order, payload, expectedBuyerAddress,
                orderEditRequest.getDeliveryLastMileEditRequest().getRegionId(), newRoute, null, null);
    }

    @Test
    public void editDeliveryLastMile_withEmptyNotes_shouldClearOrderNotes() throws Exception {
        final String notesBefore = "Изначальный комментарий";
        Order order = createDeliveryOrder(notesBefore);

        assertEquals(notesBefore, order.getNotes());

        final var newRoute = "{\"this\":\"is\",\"route\":112}";
        mockCombinatorForEditDeliveryLastMile(order, 0, 0, newRoute);

        var deliveryBefore = order.getDelivery();
        var deliveryDatesBefore = deliveryBefore.getDeliveryDates();

        var fromDate = convertToLocalDate(deliveryDatesBefore.getFromDate());
        var toDate = convertToLocalDate(deliveryDatesBefore.getToDate());
        var fromTime = deliveryDatesBefore.getFromTime();
        var toTime = deliveryDatesBefore.getToTime();
        var addressUpdate = createAddressUpdate();
        addressUpdate.setNotes(null);

        var orderEditRequest = createEditRequestForDeliveryLastMile(order,
                fromDate, toDate, fromTime, toTime, addressUpdate);

        // edit date and address
        var changeRequests = client.editOrder(
                order.getId(),
                RequestClientInfo.builder(ClientRole.USER).withClientId(BuyerProvider.UID).build(),
                singletonList(BLUE), orderEditRequest, null
        );

        // apply change request
        var patchRequest = new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null);
        client.updateChangeRequestStatus(order.getId(), changeRequests.get(0).getId(), ClientRole.SYSTEM, null,
                patchRequest);

        assertEquals(ChangeRequestStatus.APPLIED, patchRequest.getStatus());

        order = orderService.getOrder(order.getId());
        assertNull(order.getNotes());
    }

    private EditPossibilityWrapper getEditPossibilities(Order order) {
        return getEditPossibilities(order, Experiments.empty());
    }

    private EditPossibilityWrapper getEditPossibilities(Order order, Experiments experiments) {
        return EditPossibilityWrapper.build(
                client.getOrderEditPossibilities(Set.of(order.getId()), ClientRole.USER, BuyerProvider.UID,
                                List.of(BLUE), experiments.toExperimentString())
                        .get(0)
                        .getEditPossibilities());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void assertChangeRequestsForEditDeliveryLastMile(List<ChangeRequest> changeRequests,
                                                             Order order,
                                                             DeliveryLastMileChangeRequestPayload payload,
                                                             Address newAddress,
                                                             Long regionId,
                                                             String newRoute,
                                                             Long outletId,
                                                             Long deliveryServiceId
    ) throws JsonProcessingException {
        assertEquals(1, changeRequests.size());
        var changeRequest = changeRequests.get(0);
        assertEquals(ChangeRequestType.DELIVERY_LAST_MILE, changeRequest.getType());
        assertEquals(order.getDelivery().getType(), payload.getDeliveryType());
        assertEquals(newAddress, payload.getAddress());
        assertEquals(regionId, payload.getRegionId());
        if (newRoute == null) {
            assertTrue(payload.getRoute() == null || payload.getRoute().isNull());
        } else {
            assertEquals(newRoute, OBJECT_MAPPER.writeValueAsString(payload.getRoute()));
        }
        assertEquals(outletId, payload.getOutletId());
        assertEquals(deliveryServiceId, payload.getDeliveryServiceId());
    }

    private Order createDeliveryOrder() {
        return createDeliveryOrder(null);
    }

    private Order createDeliveryOrder(String notes) {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                emptySet()));
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CHANGE_LAST_MILE, true);

        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().setNotes(notes);
        parameters.getReportParameters().getActualDelivery().getResults().get(0).getDelivery().get(0)
                .setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));
        parameters.setMinifyOutlets(true);
        DeliveryRouteProvider.cleanActualDelivery(parameters.getReportParameters().getActualDelivery());

        Order order = orderCreateHelper.createOrder(parameters);
        return orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
    }

    private Order createPickupOrder() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(ANOTHER_MOCK_DELIVERY_SERVICE_ID)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(ANOTHER_MOCK_DELIVERY_SERVICE_ID, DEFAULT_INTAKE_SHIPMENT_DAYS,
                                        Collections.singletonList(OUTLET_ID_FOR_ANOTHER_MOCK_DELIVERY_SERVICE_ID))
                                .build()
                )
                .build();
        return orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
    }

    private void mockCombinatorForEditDeliveryLastMile(Order order,
                                                       int postponeDays, int postponeHours,
                                                       String newRoute)
            throws JsonProcessingException {
        var combinatorResponse =
                getRedeliveryCourierOptionsCombinatorResponse(order, postponeDays, postponeHours, personalDataService);
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/redelivery-courier-options"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

        var redeliveryRouteResponse = new RedeliveryRouteResponse();
        redeliveryRouteResponse.setRouteString(newRoute);
        combinatorMock.stubFor(
                post(urlPathEqualTo("/redelivery-route"))
                        .willReturn(okJson(mapper.writeValueAsString(redeliveryRouteResponse))));
    }

    private void mockCombinatorForEditDeliveryLastMileToPickup(Order order,
                                                               int postponeDays, int postponeHours,
                                                               String newRoute, long outletId, long newRegionId)
            throws JsonProcessingException {
        var combinatorResponse =
                getRedeliveryPickupPointOptionCombinatorResponse(order, postponeDays, postponeHours,
                        personalDataService);
        combinatorResponse.getDestination().setLogisticPointId(Long.toString(outletId));
        combinatorResponse.getDestination().setRegionId(newRegionId);
        ObjectMapper mapper = new ObjectMapper();
        combinatorMock.stubFor(
                post(urlPathEqualTo("/redelivery_pickup_point_option"))
                        .willReturn(okJson(mapper.writeValueAsString(combinatorResponse))));

        var redeliveryRouteResponse = new RedeliveryRouteResponse();
        redeliveryRouteResponse.setRouteString(newRoute);
        combinatorMock.stubFor(
                post(urlPathEqualTo("/redelivery-route"))
                        .willReturn(okJson(mapper.writeValueAsString(redeliveryRouteResponse))));
    }

    private AddressImpl createAddressUpdate() {
        AddressImpl newAddress = new AddressImpl();
        updateAddress(newAddress);
        return newAddress;
    }

    private void updateAddress(AddressImpl address) {
        address.setCountry("Россия");
        address.setCity("Москва");
        address.setStreet("Новая тестовая");
        address.setHouse("91");
        address.setApartment("111");
        address.setEntrance("4");
        address.setFloor("25");
        address.setEntryPhone("к111");
        address.setDistrict(null);
        address.setSubway("Университет");
        address.setBlock("2");
        address.setNotes("Чтобы открыть шлагбаум - звоните на телефон");
        address.setGps("37.597863,55.763234");
        address.setPostcode("123104");
        address.setPreciseRegionId(120538L);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private OrderEditRequest createEditRequestForDeliveryLastMile(Order order, LocalDate fromDate, LocalDate toDate,
                                                                  LocalTime fromTime, LocalTime toTime,
                                                                  Address newAddress, DeliveryType deliveryType,
                                                                  Long outletId, Long regionId) {
        var deliveryLastMileRequest = new DeliveryLastMileEditRequest();
        deliveryLastMileRequest.setFromDate(fromDate);
        deliveryLastMileRequest.setToDate(toDate);
        deliveryLastMileRequest.setTimeInterval(new TimeInterval(fromTime, toTime));
        deliveryLastMileRequest.setReason(HistoryEventReason.USER_CHANGED_LAST_MILE);
        deliveryLastMileRequest.setRegionId(regionId);
        deliveryLastMileRequest.setDeliveryType(deliveryType);
        deliveryLastMileRequest.setOutletId(outletId);
        deliveryLastMileRequest.setAddress(newAddress);

        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryLastMileEditRequest(deliveryLastMileRequest);
        return orderEditRequest;
    }

    private OrderEditRequest createEditRequestForDeliveryLastMile(Order order, LocalDate fromDate, LocalDate toDate,
                                                                  LocalTime fromTime, LocalTime toTime,
                                                                  Address newAddress) {
        return createEditRequestForDeliveryLastMile(order, fromDate, toDate, fromTime, toTime, newAddress,
                DeliveryType.DELIVERY, null, order.getDelivery().getRegionId());
    }

    private OrderEditRequest createEditRequestForDeliveryLastMileToPickup(
            Order order, LocalDate fromDate, LocalDate toDate, LocalTime fromTime, LocalTime toTime, Long outletId,
            long regionId) {
        return createEditRequestForDeliveryLastMile(order, fromDate, toDate, fromTime, toTime, null,
                DeliveryType.PICKUP, outletId, regionId);
    }

    private static LocalDate convertToLocalDate(Date date) {
        return LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static RedeliveryCourierOptionsResponse getRedeliveryCourierOptionsCombinatorResponse(
            Order order, int postponeDays, int postponeHours, PersonalDataService personalDataService) {
        var delivery = order.getDelivery();
        var request = RedeliveryCourierOptionsRequestFactory
                .buildRequest(order, delivery, personalDataService::getPersGps);
        var response = new RedeliveryCourierOptionsResponse();
        response.setDestination(request.getDestination());
        var options = new ArrayList<DeliveryOptionDto>();
        options.add(createOption(order, postponeDays, postponeHours));
        response.setOptions(options);
        return response;
    }

    private static DeliveryOptionDto createOption(Order order, int postponeDays, int postponeHours) {
        var option = new DeliveryOptionDto();
        var delivery = order.getDelivery();
        var deliveryDates = delivery.getDeliveryDates();
        option.setInterval(buildInterval(
                Optional.ofNullable(deliveryDates.getFromTime()).orElse(LocalTime.of(9, 0))
                        .plusHours(postponeHours),
                Optional.ofNullable(deliveryDates.getToTime()).orElse(LocalTime.of(18, 0))
                        .plusHours(postponeHours)));

        var fromDate = convertToLocalDate(deliveryDates.getFromDate())
                .plusDays(postponeDays);
        var fromDateDto = buildDate(fromDate);
        option.setDateFrom(fromDateDto);
        var toDate = convertToLocalDate(deliveryDates.getToDate())
                .plusDays(postponeDays);
        var toDateDto = buildDate(toDate);
        option.setDateTo(toDateDto);
        option.setDeliveryServiceId(delivery.getDeliveryServiceId());
        option.setDeliveryType(toCombinatorDeliveryType(delivery.getType()));
        option.setDeliverySubtype(getDeliverySubtype(order));
        option.setPaymentMethods(Set.of(CombinatorUtils.toCombinatorPaymentMethod(order.getPaymentMethod())));
        return option;
    }

    private static RedeliveryPickupPointOptionResponse getRedeliveryPickupPointOptionCombinatorResponse(
            Order expectedOrder, int postponeDays, int postponeHours, PersonalDataService personalDataService) {
        var delivery = expectedOrder.getDelivery();
        var destination = CommonRequestBuilder.buildDestination(delivery, personalDataService::getPersGps);
        var option = createOption(expectedOrder, postponeDays, postponeHours);
        return new RedeliveryPickupPointOptionResponse(destination, List.of(option));
    }

    private ResultActions getOrder(Order order) throws Exception {
        Long userId = order.getBuyer().getUid();
        var builder = get("/orders/{orderId}", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.USER.name())
                .param(CheckouterClientParams.CLIENT_ID, userId.toString())
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.CHANGE_REQUEST.name());
        return mockMvc.perform(builder);
    }

    @Nullable
    public static DateDto buildDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return new DateDto(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private void assertLastMileEditPossibilities(EditPossibilityWrapper possibilities, boolean isPossible) {
        assertEquals(possibilities.isPossible(ChangeRequestType.DELIVERY_LAST_MILE), isPossible);
        assertEquals(possibilities.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_COURIER), isPossible);
        assertEquals(possibilities.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP), isPossible);
    }
}

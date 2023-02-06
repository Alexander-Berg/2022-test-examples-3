package ru.yandex.market.checkout.checkouter.returns;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.checkouter.pay.validation.ReturnStatusValidator;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.request.ShopReturnsFilters;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.BuyerReturnViewModelCollection;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.ClientHelper;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT;
import static ru.yandex.market.checkout.checkouter.returns.ReturnFeature.SHOP_HOTLINE;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.DECISION_MADE;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.REFUNDED;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.STARTED_BY_USER;
import static ru.yandex.market.checkout.helpers.ReturnHelper.addDeliveryItemToRequest;
import static ru.yandex.market.checkout.helpers.ReturnHelper.copyWithRandomizeItemComments;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

/**
 * Тут будут тесты нового процесса возвратов через ЛК покупателя.
 */
public class ReturnClientTest extends AbstractReturnTestBase {

    @Autowired
    private ReturnHelper returnHelper;

    private Order order;

    @BeforeEach
    public void createOrder() {
        Parameters params = defaultBlueOrderParameters();
        params.getOrder().getItems().forEach(item -> {
            item.setCount(10);
            item.setQuantity(BigDecimal.TEN);
        });
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        reportMock.resetRequests();
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_SINGLE_STEP_RETURN_CREATION, true);
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_OPTIONS)
    @DisplayName("Проверка ручки /orders/{orderId}/return/options")
    public void checkReturnOptions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String ret = mockMvc.perform(
                        post("/orders/{orderId}/returns/options", order.getId())
                                .param("clientRole", "SYSTEM")
                                .param("uid", "3331")
                                .content(mapper.writeValueAsString(
                                        order.getItems().stream().map(this::toReturnItem).collect(toList())
                                )).contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.deliveryOptions").isArray())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_OPTIONS)
    @DisplayName("Проверка ручки /orders/{orderId}/return/options для доставки без айтемов")
    public void checkReturnOptionsWithoutItems() throws Exception {
        String ret = mockMvc.perform(
                        post("/orders/{orderId}/return/options", order.getId())
                                .param("clientRole", "SYSTEM")
                                .param("uid", "3331")
                                .content("[{\"isDeliveryService\":true,\"count\":1}]")
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().is4xxClientError())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_OPTIONS)
    @DisplayName("Проверка ручки /orders/{orderId}/return/options")
    public void checkReturnOptionsCombinatorFlagsPassed() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String ret = mockMvc.perform(
                        post("/orders/{orderId}/returns/options", order.getId())
                                .param("clientRole", "SYSTEM")
                                .param("uid", "3331")
                                .content(mapper.writeValueAsString(
                                        order.getItems().stream().map(this::toReturnItem).collect(toList())
                                )).contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.deliveryOptions").isArray())
                .andReturn().getResponse().getContentAsString();

        List<ServeEvent> actualDeliveryEvents = reportMock.getServeEvents().getRequests().stream()
                .filter(req -> req.getRequest().getQueryParams().get("place").isSingleValued()
                        && req.getRequest().getQueryParams().get("place").firstValue().equals("actual_delivery"))
                .collect(Collectors.toList());

        assertFalse(actualDeliveryEvents.isEmpty());
        assertTrue(actualDeliveryEvents.size() == 1);

        LoggedRequest actualDeliveryRequest = actualDeliveryEvents.get(0).getRequest();

        assertTrue(actualDeliveryRequest.getQueryParams().get("combinator").values().size() == 1);

        String combinatorParam = actualDeliveryRequest.getQueryParams().get("combinator").firstValue();
        assertThat(combinatorParam, equalTo("1"));

    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Проверка ручки /orders/{orderId}/return/create")
    public void checkReturnCreate() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);

        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        delivery.setRegionId(123L);
        delivery.setOwTicketId("12345OW");
        addCompensation(returnOptions);
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        addBankDetails(returnOptions);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, returnOptions);

        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);

        assertThat(returnResp.getItems(), hasSize(order.getItems().size()));
        assertThat(
                returnResp.getItems().stream().map(ReturnItem::getItemId).collect(Collectors.toList()),
                containsInAnyOrder(order.getItems().stream().map(OrderItem::getId).toArray())
        );

        assertThat(returnResp.getDelivery().getDeliveryServiceId(), equalTo(delivery.getDeliveryServiceId()));
        assertThat(returnResp.getDelivery().getType(), equalTo(delivery.getType()));
        assertThat(returnResp.getDelivery().getRegionId(), equalTo(123L));
        assertThat(returnResp.getDelivery().getOwTicketId(), equalTo("12345OW"));
        assertThat(returnResp.getStatus(), equalTo(ReturnStatus.STARTED_BY_USER));
        assertThat(returnResp.getComment(), equalTo("Comment"));
        assertThat(returnResp.getFeatures(), equalTo(Set.of()));
        compensationProcessed(returnResp, true);
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Проверка ручки /orders/{orderId}/return/create")
    public void checkReturnCreateFromUser() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);

        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        addBankDetails(returnOptions);

        client.returns().initReturn(order.getId(), ClientRole.USER, order.getBuyer().getUid(), returnOptions);
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Проверка ручки /orders/{orderId}/return/create")
    public void checkReturnCreateWithDifferentDeliveryServiceIdFromUser() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);

        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        Long deliveryServiceId = 9999L;
        assertNotEquals(deliveryServiceId, delivery.getDeliveryServiceId());
        delivery.setDeliveryServiceId(deliveryServiceId);
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        addBankDetails(returnOptions);

        client.returns().initReturn(order.getId(), ClientRole.USER, order.getBuyer().getUid(), returnOptions);
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Проверка ручки /orders/{orderId}/return/create. " +
            "Если не указан regionId в возврате, то берем из заказа")
    public void checkReturnCreateWithEmptyReturnId() {
        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
        ReturnOptionsResponse returnOptions = getReturnOptions(order);
        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        delivery.setRegionId(null);
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        addBankDetails(returnOptions);

        Return returnResp = client.returns()
                .initReturn(order.getId(), ClientRole.USER, order.getBuyer().getUid(), returnOptions);

        returnResp = client.returns()
                .getReturn(clientInfo, ReturnRequest.builder(returnResp.getId()).build());
        assertThat(returnResp.getDelivery().getRegionId(), equalTo(213L));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Проверка ручки /orders/{orderId}/return/create без валидации банковских данных и доставки")
    public void checkReturnCreateWithSkipValidation() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);

        returnOptions.setDelivery(null);
        returnOptions.setBankDetails(null);
        returnOptions.setComment("Comment");
        returnOptions.getItems().get(0).setSupplierCompensation(BigDecimal.valueOf(100L));
        returnOptions.setUserCompensationSum(BigDecimal.valueOf(100L));
        Return ret = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, 3331L, returnOptions);
        assertThat(ret.getDelivery(), nullValue());
        assertThat(ret.getBankDetails(), nullValue());
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Продолжение возврата с новыми BankDetails")
    public void checkReturnResumeWithNewBankDetails() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);

        Long oldContractId = returnResp.getCompensationContractId();
        Return newRequest = ReturnHelper.copy(returnResp);
        newRequest.setBankDetails(NEW_BANK_DETAILS);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, newRequest);
        returnResp = client.returns().getReturn(order.getId(), returnResp.getId(), false, ClientRole.SYSTEM, 3331L);

        compensationProcessed(returnResp);
    }

    @Test
    @DisplayName("Подтверждение возврата работает если указать только returnId")
    public void checkReturnResumeWithSpecifiedReturnIdOnly() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        Return newRequest = new Return();
        newRequest.setId(returnResp.getId());

        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, newRequest);

        returnResp = client.returns().getReturn(
                RequestClientInfo.builder(ClientRole.SYSTEM).build(),
                ReturnRequest.builder(returnResp.getId()).build());
        assertThat(returnResp.getStatus(), equalTo(ReturnStatus.REFUND_IN_PROGRESS));
    }

    @Test
    @DisplayName("Проверка метода в клиенте на получение возвратов покупателя")
    public void checkGetBuyerReturns() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);

        BuyerReturnViewModelCollection buyerReturns = client.returns()
                .getBuyerReturns(order.getBuyer().getUid(), null, null);

        assertThat(buyerReturns.getValues(), not(empty()));
    }

    @Test
    @DisplayName("Проставляем службу доставки с бекендов, не верим фронту")
    public void deliveryServiceIdSetFromCombinatorNotFront() {
        long deliveryServiceFromCombinator = 1861L;
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        returnHelper.mockActualDelivery(order, deliveryServiceFromCombinator);
        assertNotEquals(request.getDelivery().getDeliveryServiceId(), deliveryServiceFromCombinator);
        var createdReturn = client.returns().initReturn(order.getId(), ClientRole.USER,
                order.getBuyer().getUid(), request);

        assertThat(createdReturn.getDelivery().getDeliveryServiceId(), equalTo(deliveryServiceFromCombinator));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Продолжение возврата с новым ReturnDelivery")
    public void checkReturnResumeWithNewReturnDelivery() {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);
        ReturnDelivery oldDelivery = request.getDelivery();

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        assertReturnDelivery(returnResp, oldDelivery);

        Return newRequest = ReturnHelper.copy(returnResp);
        ReturnDelivery newDelivery = ReturnDelivery.newReturnDelivery(DeliveryType.DELIVERY, 12345L);
        newDelivery.setRegionId(777L);
        newRequest.setDelivery(newDelivery);
        returnHelper.mockActualDelivery(returnResp, order, newDelivery.getDeliveryServiceId());
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, newRequest);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);

        assertReturnDelivery(returnResp, newDelivery);
        assertThat(returnResp.getDelivery().getRegionId(), equalTo(777L));
    }

    private void assertReturnDelivery(Return returnResp, ReturnDelivery delivery) {
        assertThat(returnResp.getDelivery(), not(nullValue()));
        assertThat(returnResp.getDelivery().getDeliveryServiceId(), equalTo(delivery.getDeliveryServiceId()));
        assertThat(returnResp.getDelivery().getType(), equalTo(delivery.getType()));
        assertThat(returnResp.getDelivery().getOutletId(), equalTo(delivery.getOutletId()));
        if (delivery.getOutletId() != null) {
            assertThat(returnResp.getDelivery().getOutlet(), CoreMatchers.notNullValue());
        }
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Продолжение возврата с новым составом")
    public void checkReturnResumeWithItemsChanged() {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        assertItemsEquals(returnResp, returnItems);

        Return newRequest = ReturnHelper.copy(returnResp);
        newRequest.getItems().forEach(i -> {
            i.setCount(1);
            i.setQuantity(BigDecimal.ONE);
        });
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, newRequest);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);

        assertItemsEquals(returnResp, newRequest.getItems());
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Проверка генерации эвентов при создании возврата")
    public void checkReturnEventsGenerated() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_SINGLE_STEP_RETURN_CREATION,
                true);
        Return request = new Return();
        request.setItems(order.getItems().stream().map(this::toReturnItem).collect(toList()));
        setDefaultDelivery(order, request, DeliveryType.PICKUP);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        List<OrderHistoryEvent> returnCreateEvents = eventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ORDER_RETURN_CREATED);
        assertThat(returnCreateEvents, hasSize(1));
        assertThat(returnCreateEvents.get(0).getReturnId(), notNullValue());
        assertThat(eventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.ORDER_RETURN_CREATED), hasSize(1));
        assertThat(eventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.ORDER_RETURN_DELIVERY_UPDATED),
                hasSize(0));
        assertThat(eventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.ORDER_RETURN_STATUS_UPDATED),
                empty());

        ReturnOptionsResponse returnOptions = getReturnOptions(order);
        ReturnDelivery returnDelivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        client.returns().addReturnDelivery(
                order.getId(), returnResp.getId(), returnDelivery, ClientRole.SYSTEM, 0
        );
        List<OrderHistoryEvent> returnDeliveryUpdatedEvents = eventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ORDER_RETURN_DELIVERY_UPDATED);
        assertThat(returnDeliveryUpdatedEvents, hasSize(1));
        assertThat(returnDeliveryUpdatedEvents.get(0).getReturnId(), notNullValue());

        Return newRequest = ReturnHelper.copy(returnResp);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, newRequest);

        assertThat(eventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.ORDER_RETURN_STATUS_UPDATED),
                hasSize(1));
        assertThat(eventsTestHelper.getEventsOfType(order.getId(),
                        HistoryEventType.ORDER_RETURN_DELIVERY_CANCEL_REQUESTED),
                hasSize(1));
    }

    private void assertItemsEquals(Return returnResp, List<ReturnItem> returnItems) {
        assertThat(returnResp.getItems(), hasSize(returnItems.size()));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Продолжение возврата без изменений")
    public void checkReturnResumeWithNoChanges() {
        Return request = new Return();
        BigDecimal compensation = BigDecimal.valueOf(100);
        request.setUserCompensationSum(compensation);
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(toList());
        returnItems.get(0).setSupplierCompensation(compensation);
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);
        ReturnDelivery delivery = request.getDelivery();
        addBankDetails(request);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        assertItemsEquals(returnResp, returnItems);
        assertThat(returnResp.getFullName(), equalTo(getFullName(request.getBankDetails())));

        Return newRequest = ReturnHelper.copy(returnResp);
        client.returns().resumeReturn(order.getId(), returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID,
                newRequest);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        // Ничего не поменялось, возврат прошел
        assertReturnDelivery(returnResp, delivery);
        assertItemsEquals(returnResp, returnItems);
        assertThat(returnResp.getUserCompensationSum(), equalTo(compensation));
        assertThat(returnResp.getStatus(), equalTo(ReturnStatus.REFUND_IN_PROGRESS));
        assertThat(returnResp.getFullName(), equalTo(getFullName(request.getBankDetails())));
        assertThat(eventsTestHelper.getEventsOfType(order.getId(),
                        HistoryEventType.ORDER_RETURN_DELIVERY_CANCEL_REQUESTED),
                hasSize(1));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Продолжение возврата с изменением компенсации")
    public void checkReturnResumeWithCompensationChanged() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        assertItemsEquals(returnResp, request.getItems());

        Return newRequest = ReturnHelper.copy(returnResp);
        BigDecimal newCompensation = BigDecimal.valueOf(50);
        newRequest.setUserCompensationSum(newCompensation);
        client.returns().resumeReturn(order.getId(), returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID,
                newRequest);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);

        assertThat(returnResp.getUserCompensationSum(), equalTo(newCompensation));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.PUT_TRACK_CODE_TO_RETURN)
    @DisplayName("Добавление трек-кода в возврат")
    public void testSetTrackCode() {
        String trackCode = "testCode";
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        Return returnResp = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        request = ReturnHelper.copy(returnResp);
        returnResp = client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, request);

        client.returns().setReturnTrackCode(order.getId(),
                returnResp.getId(), ClientRole.SYSTEM, order.getBuyer().getUid(),
                trackCode);
        Return ret = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        assertThat(ret.getDelivery().getTrack(), not(nullValue()));
        assertThat(ret.getDelivery().getTrack().getTrackCode(), equalTo(trackCode));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Создание возврата с различным reasonType, но одинаковым itemId")
    public void createReturnWithDuplicateItemIdsAndDifferentReasonTypes() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        ReturnItem duplicateItem = toReturnItem(order.getItem(request.getItems().get(0).getItemId()));
        decreaseCountAndQuantity(request.getItems().get(0), 1);
        duplicateItem.setReasonType(ReturnReasonType.DO_NOT_FIT);
        duplicateItem.setCount(1);
        duplicateItem.setQuantity(BigDecimal.ONE);
        duplicateItem.setDefective(true);
        duplicateItem.setSupplierCompensation(BigDecimal.valueOf(100L));
        request.getItems().add(duplicateItem);
        addDeliveryItemToRequest(request);
        Return resp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        assertThat(resp.getItems(), not(empty()));
        assertThat(resp.getItems().stream()
                        .filter(i -> Objects.equals(i.getItemId(), duplicateItem.getItemId()))
                        .count(),
                equalTo(2L));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Создание оффлайн возврата")
    public void createOfflineReturn() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        request.setPayOffline(true);
        addDeliveryItemToRequest(request);
        Return response = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        assertThat(response.getPayOffline(), equalTo(true));
        request = ReturnHelper.copy(response);
        response = client.returns().resumeReturn(order.getId(),
                response.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, request);
        assertThat(response.getPayOffline(), equalTo(true));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Продолжение возврата без доставки и с флагом skipValidation = true")
    public void checkReturnResumeWithSkipValidation() {
        Return request = new Return();
        BigDecimal compensation = BigDecimal.valueOf(100);
        request.setUserCompensationSum(compensation);
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(toList());
        returnItems.get(0).setSupplierCompensation(compensation);
        request.setItems(returnItems);
        request.setDelivery(returnHelper.getDefaultReturnDelivery());
        addBankDetails(request);
        returnHelper.mockActualDelivery(request, order, request.getDelivery().getDeliveryServiceId());

        Return returnResp = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, 3331L, request);
        assertItemsEquals(returnResp, returnItems);

        Return newRequest = ReturnHelper.copy(returnResp);
        client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, newRequest, true);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        // Ничего не поменялось, возврат прошел
        compensationProcessed(returnResp);
        assertItemsEquals(returnResp, returnItems);
        assertThat(returnResp.getUserCompensationSum(), equalTo(compensation));
        assertThat(returnResp.getStatus(), equalTo(ReturnStatus.REFUND_IN_PROGRESS));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_ADD_DELIVERY)
    @DisplayName("Добавление доставки в возврат отдельной ручкой")
    public void checkReturnAddDelivery() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);
        log.info("Return options: {}", returnOptions);

        returnOptions.setDelivery(null);
        returnOptions.setBankDetails(null);
        returnOptions.setComment("Comment");
        Return ret = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, 3331L, returnOptions);
        assertThat(ret.getDelivery(), nullValue());
        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        delivery.setRegionId(777L);
        delivery.setOwTicketId("12345OW");

        ret = client.returns().addReturnDelivery(order.getId(), ret.getId(), delivery, ClientRole.SYSTEM, 3331L);

        assertThat(ret.getDelivery(), not(nullValue()));
        assertThat(ret.getDelivery().getOutletId(), CoreMatchers.is(delivery.getOutletId()));
        assertThat(ret.getDelivery().getRegionId(), is(777L));
        assertThat(ret.getDelivery().getOwTicketId(), is("12345OW"));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_ADD_DELIVERY)
    @DisplayName("Изменение доставки в возврат отдельной ручкой")
    public void checkReturnUpdateDelivery() {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);
        ReturnDelivery oldDelivery = request.getDelivery();

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        assertReturnDelivery(returnResp, oldDelivery);
        ReturnDelivery newDelivery = ReturnDelivery.newReturnDelivery(
                DeliveryType.DELIVERY,
                BlueParametersProvider.DELIVERY_SERVICE_ID
        );
        returnHelper.mockActualDelivery(returnResp, order, newDelivery.getDeliveryServiceId());
        client.returns().addReturnDelivery(order.getId(),
                returnResp.getId(), newDelivery, ClientRole.REFEREE, ClientHelper.REFEREE_UID);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);

        assertEquals(oldDelivery.getId(), newDelivery.getId());
        assertReturnDelivery(returnResp, newDelivery);
    }

    @Test
    @Epic(Epics.RETURN)
    @DisplayName("Изменение статуса доставки в возврате отдельной ручкой")
    public void checkReturnDeliveryStatusUpdate() {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        assertNotEquals(SENDER_SENT, returnResp.getDelivery().getStatus());
        ReturnDelivery deliveryUpdated = client.returns()
                .changeReturnDeliveryStatus(order.getId(),
                        returnResp.getId(),
                        SENDER_SENT,
                        null,
                        ClientRole.SYSTEM,
                        null).getDelivery();
        assertEquals(SENDER_SENT, deliveryUpdated.getStatus());
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Добавление и обновление ФИО в возврате")
    public void checkAddAndRefreshFullName() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);

        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        returnOptions.setBankDetails(FULL_NAME_BANK_DETAILS);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, returnOptions);

        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);

        assertThat(returnResp.getFullName(), equalTo(getFullName(FULL_NAME_BANK_DETAILS)));

        Return newRequest = ReturnHelper.copy(returnResp);
        newRequest.setBankDetails(NEW_FULL_NAME_BANK_DETAILS);
        returnResp = client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, 3331L, newRequest);
        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        assertThat(returnResp.getFullName(), equalTo(getFullName(NEW_FULL_NAME_BANK_DETAILS)));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Добавление и обновление ФИО в возврате с компенсацией")
    public void checkInitWithCompensationAndIncompleteBankDetails() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);
        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        returnOptions.setBankDetails(FULL_NAME_BANK_DETAILS);
        addCompensation(returnOptions);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, returnOptions);

        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);
        assertThat(returnResp.getFullName(), equalTo(getFullName(FULL_NAME_BANK_DETAILS)));

        Return newRequest = ReturnHelper.copy(returnResp);
        newRequest.setBankDetails(DEFAULT_BANK_DETAILS);
        returnResp = client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, 3331L, newRequest);
        assertThat(returnResp.getFullName(), equalTo(getFullName(DEFAULT_BANK_DETAILS)));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Создание возврата с указанием подпричины")
    public void createReturnWithSubreason() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);

        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        returnOptions.setBankDetails(FULL_NAME_BANK_DETAILS);
        returnOptions.getItems().forEach(item -> item.setSubreasonType(ReturnSubreason.DAMAGED));

        Return newReturn = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, returnOptions);

        newReturn = client.returns().getReturn(order.getId(),
                newReturn.getId(), false, ClientRole.SYSTEM, 3331L);

        assertThat(
                newReturn.getItems().stream().map(ReturnItem::getSubreasonType).collect(toSet()),
                containsInAnyOrder(ReturnSubreason.DAMAGED));

        Return newRequest = ReturnHelper.copy(newReturn);
        newReturn = client.returns().resumeReturn(order.getId(),
                newReturn.getId(), ClientRole.REFEREE, 3331L, newRequest);
        newReturn = client.returns().getReturn(order.getId(),
                newReturn.getId(), false, ClientRole.SYSTEM, 3331L);
        assertThat(
                newReturn.getItems().stream().map(ReturnItem::getSubreasonType).collect(toSet()),
                containsInAnyOrder(ReturnSubreason.DAMAGED));
    }

    @Test
    @Epic(Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Создание возврата без указания подпричины")
    public void createReturnWithoutSubreason() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);

        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        returnOptions.setBankDetails(FULL_NAME_BANK_DETAILS);
        returnOptions.getItems().forEach(item -> item.setReturnReason("мне не понравилось =("));

        Return newReturn = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, returnOptions);

        newReturn = client.returns().getReturn(order.getId(),
                newReturn.getId(), false, ClientRole.SYSTEM, 3331L);

        assertThat(
                newReturn.getItems().stream().map(ReturnItem::getReasonType).collect(toSet()),
                containsInAnyOrder(ReturnReasonType.BAD_QUALITY));
        assertThat(
                newReturn.getItems().stream().map(ReturnItem::getReturnReason).collect(toSet()),
                containsInAnyOrder("мне не понравилось =("));
        assertThat(
                newReturn.getItems().stream().map(ReturnItem::getSubreasonType).filter(Objects::nonNull).count(),
                is(0L));

        Return newRequest = ReturnHelper.copy(newReturn);
        newReturn = client.returns().resumeReturn(order.getId(),
                newReturn.getId(), ClientRole.REFEREE, 3331L, newRequest);
        newReturn = client.returns().getReturn(order.getId(),
                newReturn.getId(), false, ClientRole.SYSTEM, 3331L);
        assertThat(
                newReturn.getItems().stream().map(ReturnItem::getReasonType).collect(toSet()),
                containsInAnyOrder(ReturnReasonType.BAD_QUALITY));
        assertThat(
                newReturn.getItems().stream().map(ReturnItem::getReturnReason).collect(toSet()),
                containsInAnyOrder("мне не понравилось =("));
        assertThat(
                newReturn.getItems().stream().map(ReturnItem::getSubreasonType).filter(Objects::nonNull).count(),
                is(0L));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void shouldCreateReturnWithPicturesUrls() {
        Order order = orderCreateHelper.createOrder(defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        ClientInfo clientInfo = ClientHelper.userClientFor(order);
        ReturnOptionsResponse returnOptions = getReturnOptions(order);
        returnOptions.setDelivery(convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0)));

        returnOptions.getItems().forEach(ri -> {
            try {
                ri.setPicturesUrls(List.of(new URL("http://example.org"), new URL("http://example.com")));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
        Return returnResult = client.returns()
                .initReturn(order.getId(), clientInfo.getRole(), clientInfo.getUid(), returnOptions);

        MatcherAssert.assertThat(returnResult.getItems(), everyItem(
                hasProperty("picturesUrls", hasSize(2))
        ));

        ReturnRequest returnRequest = ReturnRequest.builder(returnResult.getId(), order.getId()).build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(clientInfo.getRole(), clientInfo.getId());
        Return aReturn = client.returns()
                .getReturn(requestClientInfo, returnRequest);

        MatcherAssert.assertThat(aReturn.getItems(), everyItem(
                hasProperty("picturesUrls", hasSize(2))
        ));
    }

    @Test
    public void shouldCreateWithReturnFeatures() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);

        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        returnOptions.setDelivery(delivery);

        ClientInfo clientInfo = ClientHelper.userClientFor(order);
        returnOptions.setDelivery(convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0)));
        returnOptions.setFeatures(Set.of(SHOP_HOTLINE));


        Return returnResult = client.returns()
                .initReturn(order.getId(), clientInfo.getRole(), clientInfo.getUid(), returnOptions);

        MatcherAssert.assertThat(returnResult.getFeatures(), hasItem(equalTo(SHOP_HOTLINE)));

        ReturnRequest returnRequest = ReturnRequest.builder(returnResult.getId(), order.getId()).build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(clientInfo.getRole(), clientInfo.getId());
        Return aReturn = client.returns()
                .getReturn(requestClientInfo, returnRequest);

        MatcherAssert.assertThat(aReturn.getFeatures(), hasSize(1));
        MatcherAssert.assertThat(aReturn.getFeatures(), hasItem(equalTo(SHOP_HOTLINE)));
    }

    @Test
    @Epic(Epics.RETURN)
    @DisplayName("Изменение статуса возврата с правильной ролью")
    public void testChangeReturnStatusSuccess() throws Exception {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);

        ClientInfo clientInfo = ClientHelper.shopUserClientFor(order);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        assertNotEquals(DECISION_MADE, returnResp.getStatus());

        client.returns().updateReturnItemDecision(order.getId(), returnResp.getId(), clientInfo.getRole(),
                clientInfo.getId(), clientInfo.getUid(),
                returnResp.getItems().stream().map(item ->
                                new ReturnDecision(item.getId(), ReturnDecisionType.REFUND_MONEY, ""))
                        .collect(Collectors.toUnmodifiableList())
        );

        Return returnResult = client.returns()
                .updateReturnStatus(order.getId(), returnResp.getId(), DECISION_MADE, null, clientInfo.getRole(),
                        clientInfo.getShopId(), clientInfo.getUid());

        assertEquals(DECISION_MADE, returnResult.getStatus());
    }

    @Epic(Epics.RETURN)
    @DisplayName("Отмена возврата ролью ")
    @ParameterizedTest(name = "Отмена возврата ролью " + ParameterizedTest.ARGUMENTS_PLACEHOLDER)
    @MethodSource("userAccessRoles")
    public void testChangeReturnCancelSuccess(ClientRole role) {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);

        ClientInfo clientInfo = ClientHelper.userClientFor(order);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.USER, clientInfo.getUid(), request);
        assertNotEquals(CANCELLED, returnResp.getStatus());

        Return returnResult = client.returns().updateReturnStatus(order.getId(), returnResp.getId(), CANCELLED,
                ReturnCancelReason.USER_CHANGED_MIND, role, null, clientInfo.getUid());


        assertEquals(CANCELLED, returnResult.getStatus());
    }

    public static Stream<Arguments> userAccessRoles() {
        return ReturnStatusValidator.CLIENT_ACCESS_LEVEL_ROLES.stream().map(Arguments::of);
    }

    @Test
    @Epic(Epics.RETURN)
    @DisplayName("Отмена возврата с без причины невозможна")
    public void testChangeReturnCancelNoReasonFail() {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);

        ClientInfo clientInfo = ClientHelper.userClientFor(order);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.USER, clientInfo.getUid(), request);
        assertNotEquals(CANCELLED, returnResp.getStatus());

        try {
            client.returns().updateReturnStatus(order.getId(), returnResp.getId(), CANCELLED,
                    null, ClientRole.USER, null, 3331L);
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Can't cancel without a reason"));
        }
    }

    @Test
    @Epic(Epics.RETURN)
    @DisplayName("Отмена возвратного отправления логистикой (возврат компенсирован)")
    public void testChangeReturnDeliveryCancellation() {
        var ret = returnHelper.createReturnForOrder(order, (r, o) -> {
            r.setDelivery(returnHelper.getDefaultReturnDelivery());
            r.setStatus(REFUNDED);
            return r;
        });
        long returnId = ret.getId();

        Assertions.assertEquals(REFUNDED, ret.getStatus());

        assertDoesNotThrow(() ->
                client.returns().changeReturnDeliveryStatus(order.getId(), returnId, ReturnDeliveryStatus.CANCELLED,
                        ReturnCancelReason.ITEMS_QUANTITY_MISMATCH, ClientRole.DELIVERY_SERVICE, 1L));

    }

    @Test
    @Epic(Epics.RETURN)
    @DisplayName("Изменение статуса возврата с неправильной ролью")
    public void testChangeReturnStatusWrongRole() {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);

        try {
            client.returns().updateReturnStatus(order.getId(), returnResp.getId(), DECISION_MADE, null,
                    ClientRole.SYSTEM, 123L, 3331L);
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("no permission"));
            return;
        }

        fail("Should catch exception");
    }

    @Test
    @Epic(Epics.RETURN)
    @DisplayName("Изменение решения по возврату")
    public void testAddReturnItemDecision() {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);

        ClientInfo clientInfo = ClientHelper.shopUserClientFor(order);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);

        Return returnResult = client.returns().updateReturnItemDecision(order.getId(), returnResp.getId(),
                clientInfo.getRole(), clientInfo.getShopId(), clientInfo.getUid(),
                returnResp.getItems().stream().map(item ->
                                new ReturnDecision(item.getId(), ReturnDecisionType.REFUND_MONEY, "Random comment"))
                        .collect(Collectors.toList())
        );
        assertEquals(returnResult.getItems().size(), order.getItems().size());
    }

    @Test
    @Epic(Epics.RETURN)
    @DisplayName("Изменение решения по возврату с неправильной ролью")
    public void testAddReturnItemDecisionWrongRole() {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.PICKUP);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);

        try {
            client.returns().updateReturnItemDecision(order.getId(), returnResp.getId(), ClientRole.SYSTEM, 123L, 3331L,
                    returnResp.getItems().stream().map(item ->
                                    new ReturnDecision(item.getId(), ReturnDecisionType.REFUND_MONEY, "Random comment"))
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("no permission"));
            return;
        }

        fail("Should catch exception");
    }

    @Test
    @Epic(Epics.RETURN)
    @DisplayName("Изменение даты возврата")
    public void testChangeReturnDeliveryDate() {
        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
        request.setItems(returnItems);
        setDefaultDelivery(order, request, DeliveryType.DELIVERY);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        var initialDelivery = returnResp.getDelivery();
        assertNotNull(initialDelivery);
        assertNotNull(initialDelivery.getDates());
        assert initialDelivery.getDates().getFromTime() != null;
        assert initialDelivery.getDates().getToTime() != null;

        Assertions.assertNull(initialDelivery.getReschedulingReason());

        LocalDate fromDate = LocalDate.now().plusDays(5);
        LocalDate toDate = LocalDate.now().plusDays(5);


        ReturnDeliveryReschedulingRequest reschedulingRequest = new ReturnDeliveryReschedulingRequest(
                fromDate, toDate,
                initialDelivery.getDates().getFromTime(), initialDelivery.getDates().getToTime(),
                ReturnDeliveryReschedulingReason.USER_REQUESTED
        );

        client.returns().rescheduleDelivery(returnResp.getId(), reschedulingRequest, ClientRole.DELIVERY_SERVICE);

        Return returnAfter = client.returns().getReturn(RequestClientInfo.builder(ClientRole.SYSTEM).build(),
                ReturnRequest.builder(returnResp.getId()).build());
        assert returnAfter.getDelivery().getDates() != null;
        assertEquals(Date.from(fromDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()),
                returnAfter.getDelivery().getDates().getFromDate());
        assertEquals(Date.from(toDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()),
                returnAfter.getDelivery().getDates().getToDate());
        assertEquals(ReturnDeliveryReschedulingReason.USER_REQUESTED,
                returnAfter.getDelivery().getReschedulingReason());
        assertThat(eventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ORDER_RETURN_DELIVERY_RESCHEDULED), hasSize(1));
    }

    @Test
    @Epic(Epics.RETURN)
    @DisplayName("Проверка ручки /returns?shopId={shopId}")
    public void checkFindAllShopReturns() throws Exception {
        Parameters firstOrderParameters = defaultBlueOrderParameters();
        firstOrderParameters.setShopId(111L);
        Order firstOrder = orderCreateHelper.createOrder(firstOrderParameters);
        orderStatusHelper.proceedOrderToStatus(firstOrder, OrderStatus.DELIVERED);

        Return firstReturnRequest = new Return();
        List<ReturnItem> firstReturnItems =
                firstOrder.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
        firstReturnRequest.setItems(firstReturnItems);
        setDefaultDelivery(firstOrder, firstReturnRequest, DeliveryType.PICKUP);

        client.returns().initReturn(firstOrder.getId(), ClientRole.SYSTEM, 3331L, firstReturnRequest).getId();

        Parameters secondOrderParameters = defaultBlueOrderParameters();
        secondOrderParameters.setShopId(222L);
        Order secondOrder = orderCreateHelper.createOrder(secondOrderParameters);
        orderStatusHelper.proceedOrderToStatus(secondOrder, OrderStatus.DELIVERED);

        Return secondReturnRequest = new Return();
        List<ReturnItem> secondReturnItems =
                secondOrder.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
        secondReturnRequest.setItems(secondReturnItems);
        setDefaultDelivery(secondOrder, secondReturnRequest, DeliveryType.PICKUP);

        Long expectedReturnId = client.returns()
                .initReturn(secondOrder.getId(), ClientRole.SYSTEM, 3331L, secondReturnRequest).getId();

        assertThrows(ErrorCodeException.class, () -> {
            Return thirdReturnRequest = new Return();
            List<ReturnItem> thirdReturnItems =
                    secondOrder.getItems().stream().map(this::toReturnItem).collect(Collectors.toList());
            thirdReturnRequest.setItems(thirdReturnItems);
            setDefaultDelivery(secondOrder, thirdReturnRequest, DeliveryType.PICKUP);
            thirdReturnRequest = copyWithRandomizeItemComments(thirdReturnRequest);

            Return thirdReturn = client.returns()
                    .initReturn(secondOrder.getId(), ClientRole.SYSTEM, 3331L, thirdReturnRequest);
        });
        ClientInfo shopClientInfo = ClientHelper.shopUserClientFor(secondOrder);

        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.SYSTEM, 3331L);

        PagedReturns pagedReturns = client.returns().getShopReturns(
                new RequestClientInfo(clientInfo.getClientRole(), clientInfo.getClientId(), clientInfo.getShopId()),
                ShopReturnsFilters.builder(222L)
                        .withOrder(List.of(secondOrder.getId(), 10000L))
                        .withFromDate(Date.from(LocalDateTime.now().minusDays(5).atZone(ZoneId.systemDefault())
                                .toInstant()))
                        .withToDate(Date.from(LocalDateTime.now().plusDays(5).atZone(ZoneId.systemDefault())
                                .toInstant()))
                        .withStatuses(EnumSet.of(STARTED_BY_USER))
                        .withPage(1).withPageSize(10).build()
        );

        assertEquals(1, pagedReturns.getItems().size());
        assertEquals(expectedReturnId, new ArrayList<>(pagedReturns.getItems()).get(0).getId());
    }

    private String getFullName(BankDetails buyer) {
        return (buyer.getLastName() + " " + buyer.getFirstName() + " " + avoidNull(buyer.getMiddleName(), "")).trim();
    }

    private void decreaseCountAndQuantity(ReturnItem returnItem, int amount) {
        returnItem.setQuantity(returnItem.getQuantityIfExistsOrCount().subtract(BigDecimal.valueOf(amount)));
        returnItem.setCount(returnItem.getCount() - amount);
    }
}

package ru.yandex.market.pvz.internal.controller.return_request;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnableItemsResponse;
import ru.yandex.market.checkout.checkouter.viewmodel.ReturnableItemViewModel;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderItem;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;
import ru.yandex.market.tpl.common.util.exception.TplForbiddenException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.PARTIAL_RETURN_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.SEND_RECEIVE_TO_OW_ENABLED;
import static ru.yandex.market.pvz.core.domain.returns.model.ReturnRequest.DEFAULT_EXPIRATION_DAYS;


@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReturnRequestControllerTest extends BaseShallowTest {

    private final TestPickupPointFactory pickupPointFactory;
    private final TestReturnRequestFactory returnRequestFactory;

    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final ReturnRequestController returnRequestController;

    private final TestableClock clock;

    private PickupPoint pickupPoint;

    private ReturnRequestParams returnRequest;

    private final TestOrderFactory orderFactory;

    @MockBean
    private CheckouterClient checkouterClient;

    @MockBean
    private CheckouterReturnClient checkouterReturnClient;

    @BeforeEach
    public void setUp() {
        pickupPoint = pickupPointFactory.createPickupPoint();
        returnRequest = returnRequestFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        configurationGlobalCommandService.setValue(SEND_RECEIVE_TO_OW_ENABLED, true);
        configurationGlobalCommandService.setValue(PARTIAL_RETURN_ENABLED, true);
    }

    @Test
    @SneakyThrows
    void createReturnInCheckouter() {
        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneId.systemDefault());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder().externalId("123").build())
                .build()
        );
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);

        var item = order.getItems().get(0);
        var checkouterReturn = createReturnFromCheckauter(order, pickupPoint, item);
        var requestDate = LocalDate.ofInstant(checkouterReturn.getCreatedAt(), ZoneId.systemDefault());
        var createReturnDto = String.format(getFileContent("return_request/create_return.json"),
                item.getId(), item.getName(), item.getPrice(), item.getCount()
        );
        var expectedJson = String.format(getFileContent("return_request/create_return_response.json"),
                item.getOrder().getExternalId(), order.getRecipientName(), requestDate,
                requestDate.plusDays(DEFAULT_EXPIRATION_DAYS), item.getName(), item.getPrice(), item.getCount(),
                item.getSumPrice(), item.getCount(), item.getSumPrice()
        );

        when(checkouterReturnClient.initReturn(anyLong(), any(), anyLong(), any(), anyBoolean()))
                .thenReturn(checkouterReturn);

        mockMvc.perform(MockMvcRequestBuilders.post((
                String.format("/v1/pi/pickup-points/%s/return-requests/create/%s", pickupPoint.getPvzMarketId(),
                        order.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(createReturnDto)
        ).andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson, false));
    }

    @Test
    void getReturnById() throws Exception {
        var expectedJson = buildExpectedReturnRequestJson(returnRequest, null);
        mockMvc.perform(get(
                "/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId()
                        + "/return-requests/" + returnRequest.getReturnId()
        )).andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson, false));
    }

    @Test
    void getErrorWithWrongReturnId() throws Exception {
        mockMvc.perform(get("/v1/pi/pickup-points/1/return-requests/" + returnRequest.getReturnId() + 123))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getReturnByIdWithThrownError() {
        var pickupPointData = new PickupPointRequestData(
                pickupPoint.getId() + 1, 123L, "Катарсис", 123L, 3, 7);
        assertThatThrownBy(() -> returnRequestController.getReturnById(returnRequest.getReturnId(), pickupPointData))
                .isInstanceOf(TplForbiddenException.class)
                .hasMessageContaining("Данные по возвратам другого ПВЗ недоступны");
    }

    @Test
    void getReturnBarcode() throws Exception {
        mockMvc.perform(get(
                "/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId()
                        + "/return-requests/" + returnRequest.getReturnId() + "/barcode"
        )).andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .string("{\"barcode\":\"VOZVRAT_SF_PVZ_" + returnRequest.getReturnId() + "\"}"));
    }

    @Test
    void updateReturnItemComment() throws Exception {
        String expectedJson = buildExpectedReturnRequestJson(
                returnRequest, "Действительно без катарсиса");
        mockMvc.perform(patch(
                "/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId()
                        + "/return-requests/" + returnRequest.getReturnId() + "/update-comment")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(String.format(getFileContent("return_request/return_request_update_comment.json"),
                        returnRequest.getItems().get(0).getId()))
        ).andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson, false));
    }

    @Test
    @SneakyThrows
    void receiveReturn() {
        String expectedJson = String.format(
                getFileContent("return_request/return_request_receive.json"), returnRequest.getReturnId());
        mockMvc.perform(patch(
                "/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId()
                        + "/return-requests/" + returnRequest.getReturnId() + "/receive")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson, false));
    }

    @Test
    @SneakyThrows
    void getReturnableItems() {
        configurationGlobalCommandService.setValue(PARTIAL_RETURN_ENABLED, true);

        var testPickupPoint = pickupPointFactory.createPickupPoint();
        testPickupPoint = pickupPointFactory.updatePickupPoint(testPickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .partialReturnAllowed(true)
                        .build());
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder().pickupPoint(testPickupPoint)
                        .params(
                                TestOrderFactory.OrderParams.builder()
                                        .items(List.of(TestOrderFactory.OrderItemParams.builder().build()))
                                        .externalId("123").build())
                        .build());
        orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);

        var items = order.getItems();
        var item = items.get(0);
        var returnableItems = StreamEx.of(order.getItems()).map(this::createItem).toList();
        var response = new ReturnableItemsResponse(Collections.emptySet(), returnableItems, Collections.emptyList());

        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setNoAuth(false);
        checkouterOrder.setBuyer(new Buyer(99L));
        when(checkouterClient.getOrder(any(), any())).thenReturn(checkouterOrder);
        when(checkouterReturnClient.getReturnableItems(Long.parseLong(order.getExternalId()), ClientRole.USER, 99L))
                .thenReturn(response);

        var expectedJson = String.format(getJsonFromFile("return_response/returnable_items.json"),
                order.getRecipientName(), order.getRecipientEmail(), order.getRecipientPhone(), item.getId(),
                item.getName(), item.getPrice(), item.getCount(),
                item.getPrice().multiply(BigDecimal.valueOf(item.getCount())), true
        );
        mockMvc.perform(get(
                "/v1/pi/pickup-points/" + testPickupPoint.getPvzMarketId() + "/return-requests/returnable-items")
                .param("orderId", String.valueOf(order.getId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson, false));
    }

    private String buildExpectedReturnRequestJson(ReturnRequestParams request, String expectedComment) {
        if (expectedComment != null) {
            return String.format(
                    getFileContent("return_request/return_request_with_comment.json"), request.getReturnId(),
                    request.getOrderId(), request.getItems().get(0).getId(), expectedComment);
        }
        return String.format(getFileContent("return_request/return_request.json"),
                request.getReturnId(), request.getOrderId(), request.getItems().get(0).getId());
    }

    private String getJsonFromFile(String filePath) {
        return getFileContent(filePath);
    }

    private ReturnableItemViewModel createItem(OrderItem item) {
        var returnableItem = new ReturnableItemViewModel();
        returnableItem.setItemId(item.getId());
        returnableItem.setItemTitle(item.getName());
        returnableItem.setCount(item.getCount());
        returnableItem.setBuyerPrice(item.getPrice());
        return returnableItem;
    }

    private Return createReturnFromCheckauter(Order order, PickupPoint pickupPoint, OrderItem item) {
        var returnFromCheckauter = new Return();
        returnFromCheckauter.setId(123456L);
        returnFromCheckauter.setOrderId(Long.valueOf(order.getExternalId()));

        var returnItem = new ReturnItem();
        returnItem.setCount(item.getCount());
        returnItem.setReturnId(123456L);
        returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
        returnItem.setItemTitle(item.getName());
        returnItem.setItemId(item.getId());
        returnItem.setReturnReason("Катарсис не случился (((");
        returnFromCheckauter.setItems(List.of(returnItem));
        returnFromCheckauter.setCreationDate(Date.from(Instant.now(clock)));
        return returnFromCheckauter;
    }

}

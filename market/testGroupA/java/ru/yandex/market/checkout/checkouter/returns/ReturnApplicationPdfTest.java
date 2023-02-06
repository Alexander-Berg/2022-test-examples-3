package ru.yandex.market.checkout.checkouter.returns;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.checkout.helpers.ReturnHelper.addDeliveryItemToRequest;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class ReturnApplicationPdfTest extends AbstractReturnTestBase {

    private Order order;

    @BeforeEach
    public void createOrder() {
        MockitoAnnotations.initMocks(this);
        Parameters params = defaultBlueOrderParameters();
        params.getOrder().getItems().forEach(item -> item.setCount(10));
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("При создании возврата проставляется URL к заявлению")
    public void pdfGenerationAfterReturnInit() throws Exception {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        addDeliveryItemToRequest(request);
        Return response = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        response = client.returns().getReturn(response.getId(), false, ClientRole.SYSTEM, 1L);
        assertThat(response.getApplicationUrl(), notNullValue());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.POST_RETURN)
    @DisplayName("Ручка скачивания PDF с заявлением на возврат работает")
    public void returnApplicationPdfDownload() throws Exception {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        addDeliveryItemToRequest(request);
        Return response = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        RequestBuilder requestBuilder = get("/orders/{orderId}/returns/{returnId}/pdf", order.getId(), response
                .getId())
                .param("clientRole", ClientRole.SYSTEM.name())
                .param("uid", order.getBuyer().getUid().toString())
                .contentType(APPLICATION_JSON_UTF8);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.POST_RETURN)
    @DisplayName("Ручка скачивания PDF с заявлением на возврат (ошибка авторизации)")
    public void returnApplicationPdfDownloadWrongUser() throws Exception {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        addDeliveryItemToRequest(request);
        Return response = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        RequestBuilder requestBuilder = get("/orders/{orderId}/returns/{returnId}/pdf", order.getId(), response
                .getId())
                .param("clientRole", order.getUserClientInfo().getRole().name())
                .param("uid", "123")
                .contentType(APPLICATION_JSON_UTF8);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.POST_RETURN)
    @DisplayName("Ручка скачивания PDF с заявлением на возврат (ошибка ID заказа)")
    public void returnApplicationPdfDownloadWrongOrderId() throws Exception {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        addDeliveryItemToRequest(request);
        Return response = client.returns().initReturn(order.getId(),
                ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        RequestBuilder requestBuilder = get("/orders/{orderId}/returns/{returnId}/pdf",
                9999, response.getId())
                .param("clientRole", order.getUserClientInfo().getRole().name())
                .param("uid", "123")
                .contentType(APPLICATION_JSON_UTF8);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }
}

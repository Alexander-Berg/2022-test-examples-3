package ru.yandex.market.logistics.nesu.controller.order;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.model.enums.tags.OrderTag;
import ru.yandex.market.logistics.nesu.base.order.AbstractCreateOrderTest;
import ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraft;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание черновика заказа")
class BackOfficeCreateOrderTest extends AbstractCreateOrderTest {

    @Test
    @DisplayName("Недоступный магазин")
    void noShopAccess() throws Exception {
        createOrder(OrderDtoFactory.defaultOrderDraft(), 1L, 2L)
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Failed to find [SENDER] with ids [1]\","
                + "\"resourceType\":\"SENDER\",\"identifiers\":[1]}"));
    }

    @Override
    protected String orderDraftObjectName() {
        return "orderDraft";
    }

    @Nonnull
    @Override
    protected ResultActions createOrder(Consumer<OrderDraft> orderDraftAdjuster, Long senderId) throws Exception {
        return createOrder(orderDraftAdjuster, senderId, 1L);
    }

    @Nonnull
    @Override
    protected ResultActions createOrder(String fileName, Long senderId) throws Exception {
        return mockMvc.perform(
            post("/back-office/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    String.format("controller/order/create/request/back_office/%s.json", fileName)
                ))
                .param("userId", "1")
                .param("senderId", String.valueOf(senderId))
                .param("shopId", String.valueOf(1L))
        );
    }

    @Nonnull
    @Override
    protected ResultActions createOrder(Consumer<OrderDraft> orderDraftAdjuster, Long senderId, Long shopId)
        throws Exception {
        OrderDraft orderDraft = new OrderDraft();
        orderDraftAdjuster.accept(orderDraft);

        return mockMvc.perform(request(HttpMethod.POST, "/back-office/orders", orderDraft)
            .param("userId", "1")
            .param("senderId", String.valueOf(senderId))
            .param("shopId", String.valueOf(shopId)));
    }

    @Nonnull
    @Override
    protected OrderTag getTag() {
        return OrderTag.CREATED_VIA_DAAS_BACK_OFFICE;
    }
}

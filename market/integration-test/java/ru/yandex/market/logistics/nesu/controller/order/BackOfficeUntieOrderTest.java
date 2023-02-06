package ru.yandex.market.logistics.nesu.controller.order;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Отвязка заказа от отгрузки")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
class BackOfficeUntieOrderTest extends AbstractContextualTest {

    protected static final long ORDER_ID = 100L;

    private long shopId = 1;

    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setup() {
        when(lomClient.getOrder(ORDER_ID, Set.of()))
            .thenReturn(Optional.of(defaultOrder()));
    }

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        shopId = 2;

        untieFromShipment(ORDER_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [1]"));
    }

    @Test
    @DisplayName("Неизвестный заказ")
    void unknownOrder() throws Exception {
        untieFromShipment(ORDER_ID + 1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with ids [101]"));
    }

    @Test
    @DisplayName("Неправильный идентификатор клиента платформы")
    void wrongPlatformClientId() throws Exception {
        doReturn(Optional.of(defaultOrder().setPlatformClientId(2L)))
            .when(lomClient).getOrder(ORDER_ID, Set.of());

        untieFromShipment(ORDER_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with ids [100]"));
    }

    @Test
    @DisplayName("Неизвестный сендер")
    void unknownSender() throws Exception {
        doReturn(Optional.of(defaultOrder().setSenderId(-1L)))
            .when(lomClient).getOrder(ORDER_ID, Set.of());

        untieFromShipment(ORDER_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [-1]"));
    }

    @Test
    @DisplayName("Ошибка отвязки")
    void lomError() throws Exception {
        String lomError = "LOM error";
        int lomStatus = 451;
        doThrow(new HttpTemplateException(lomStatus, lomError))
            .when(lomClient).untieOrderFromShipment(ORDER_ID);

        untieFromShipment(ORDER_ID)
            .andExpect(status().isUnavailableForLegalReasons())
            .andExpect(content().string(lomError));
    }

    @Test
    @DisplayName("Успешная отвязка")
    void success() throws Exception {
        untieFromShipment(ORDER_ID)
            .andExpect(status().isOk());

        verify(lomClient).untieOrderFromShipment(ORDER_ID);
    }

    @Nonnull
    private OrderDto defaultOrder() {
        return new OrderDto().setId(ORDER_ID).setPlatformClientId(3L).setSenderId(1L);
    }

    @Nonnull
    private ResultActions untieFromShipment(Long orderId) throws Exception {
        return mockMvc.perform(post("/back-office/orders/" + orderId + "/untie-from-shipment")
            .param("userId", "1")
            .param("shopId", String.valueOf(shopId)));
    }

}

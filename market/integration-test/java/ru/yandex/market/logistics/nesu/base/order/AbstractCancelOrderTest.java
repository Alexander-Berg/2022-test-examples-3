package ru.yandex.market.logistics.nesu.base.order;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CancelOrderDto;
import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractCancelOrderTest extends AbstractContextualTest {

    protected static final long ORDER_ID = 100L;

    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setup() {
        when(lomClient.getOrder(ORDER_ID, Set.of()))
            .thenReturn(Optional.of(defaultOrder()));
    }

    @Test
    @DisplayName("Неизвестный заказ")
    void unknownOrder() throws Exception {
        cancel(ORDER_ID + 1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with ids [101]"));
    }

    @Test
    @DisplayName("Неправильный идентификатор клиента платформы")
    void wrongPlatformClientId() throws Exception {
        doReturn(Optional.of(defaultOrder().setPlatformClientId(2L)))
            .when(lomClient).getOrder(ORDER_ID, Set.of());

        cancel(ORDER_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with ids [100]"));
    }

    @Test
    @DisplayName("Неизвестный сендер")
    void unknownSender() throws Exception {
        doReturn(Optional.of(defaultOrder().setSenderId(-1L)))
            .when(lomClient).getOrder(ORDER_ID, Set.of());

        cancel(ORDER_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [-1]"));
    }

    @Test
    @DisplayName("Ошибка отмены")
    void lomError() throws Exception {
        String lomError = "LOM error";
        int lomStatus = 451;
        doThrow(new HttpTemplateException(lomStatus, lomError)).when(lomClient).cancelOrder(
            ORDER_ID,
            CancelOrderDto.builder().reason(CancellationOrderReason.SHOP_CANCELLED).build(),
            false
        );

        cancel(ORDER_ID)
            .andExpect(status().isUnavailableForLegalReasons())
            .andExpect(content().string(lomError));
    }

    @Test
    @DisplayName("Успешная отмена")
    void success() throws Exception {
        doReturn(CancellationOrderRequestDto.builder().id(1L).status(CancellationOrderStatus.PROCESSING).build())
            .when(lomClient)
            .cancelOrder(
                ORDER_ID,
                CancelOrderDto.builder().reason(CancellationOrderReason.SHOP_CANCELLED).build(),
                false
            );

        cancel(ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/cancel/success.json"));
    }

    private OrderDto defaultOrder() {
        return new OrderDto().setId(ORDER_ID).setPlatformClientId(3L).setSenderId(1L);
    }

    protected abstract ResultActions cancel(Long orderId) throws Exception;

}

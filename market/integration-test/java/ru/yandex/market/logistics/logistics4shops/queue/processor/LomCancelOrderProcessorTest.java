package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.queue.payload.LomCancelOrderPayload;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CancelOrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Отмена заказа в LOM")
class LomCancelOrderProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private LomCancelOrderProcessor processor;
    @Autowired
    private LomClient lomClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Успешная обработка")
    void successProcessing() {
        when(lomClient.searchOrders(
            OrderSearchFilter.builder().externalIds(Set.of("1")).senderIds(Set.of(2L)).build(),
            Pageable.unpaged()
        ))
            .thenReturn(PageResult.of(
                List.of(new OrderDto().setExternalId("1").setId(123L)),
                1,
                0,
                1
            ));

        processor.execute(defaultPayload());

        verify(lomClient).searchOrders(
            eq(OrderSearchFilter.builder().externalIds(Set.of("1")).senderIds(Set.of(2L)).build()),
            eq(Pageable.unpaged())
        );
        verify(lomClient).cancelOrder(
            eq(123L),
            eq(CancelOrderDto.builder().reason(CancellationOrderReason.SHOP_CANCELLED).build()),
            eq(true)
        );
    }

    @Test
    @DisplayName("Заказ не найден")
    void notFoundLomOrder() {
        when(lomClient.searchOrders(
            OrderSearchFilter.builder().externalIds(Set.of("1")).senderIds(Set.of(2L)).build(),
            Pageable.unpaged()
        ))
            .thenReturn(PageResult.of(List.of(), 0, 0, 1));

        softly.assertThatThrownBy(() -> processor.execute(defaultPayload()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Found 0 orders with externalId=1, shopId=2 in LOM");

        verify(lomClient).searchOrders(
            eq(OrderSearchFilter.builder().externalIds(Set.of("1")).senderIds(Set.of(2L)).build()),
            eq(Pageable.unpaged())
        );
        verify(lomClient, never()).cancelOrder(anyLong(), any(CancelOrderDto.class), anyBoolean());
    }

    @Nonnull
    private static LomCancelOrderPayload defaultPayload() {
        return LomCancelOrderPayload.builder()
            .externalId("1")
            .shopId(2L)
            .build();
    }
}

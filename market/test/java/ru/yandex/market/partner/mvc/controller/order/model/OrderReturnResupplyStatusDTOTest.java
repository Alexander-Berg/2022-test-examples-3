package ru.yandex.market.partner.mvc.controller.order.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.order.returns.model.OrderReturnResupplyStatus;
import ru.yandex.market.partner.mvc.controller.returns.model.OrderReturnResupplyStatusDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderReturnResupplyStatusDTOTest {

    @Test
    void reduce() {
        assertEquals(OrderReturnResupplyStatusDTO.PARTIALLY_RESUPPLIED_TO_BAD_AND_GOOD_STOCK,
                OrderReturnResupplyStatusDTO.fromReturnResupplyStatus(
                        OrderReturnResupplyStatus.reduce(
                                OrderReturnResupplyStatus.NOT_RESUPPLIED,
                                OrderReturnResupplyStatus.PARTIALLY_RESUPPLIED_TO_BAD_AND_GOOD_STOCK))

        );

        assertEquals(OrderReturnResupplyStatusDTO.PARTIALLY_RESUPPLIED_TO_BAD_AND_GOOD_STOCK,
                OrderReturnResupplyStatusDTO.fromReturnResupplyStatus(
                OrderReturnResupplyStatus.reduce(
                        OrderReturnResupplyStatus.RESUPPLIED_TO_BAD_STOCK,
                        OrderReturnResupplyStatus.PARTIALLY_RESUPPLIED_TO_GOOD_STOCK))

        );

        assertEquals(OrderReturnResupplyStatusDTO.PARTIALLY_RESUPPLIED_TO_GOOD_STOCK,
                OrderReturnResupplyStatusDTO.fromReturnResupplyStatus(
                OrderReturnResupplyStatus.reduce(
                        OrderReturnResupplyStatus.RESUPPLIED_TO_GOOD_STOCK,
                        OrderReturnResupplyStatus.NOT_RESUPPLIED))

        );

        assertEquals(OrderReturnResupplyStatusDTO.RESUPPLIED_TO_BAD_AND_GOOD_STOCK,
                OrderReturnResupplyStatusDTO.fromReturnResupplyStatus(
                OrderReturnResupplyStatus.reduce(
                        OrderReturnResupplyStatus.RESUPPLIED_TO_GOOD_STOCK,
                        OrderReturnResupplyStatus.RESUPPLIED_TO_BAD_STOCK))

        );

        assertEquals(OrderReturnResupplyStatusDTO.RESUPPLIED_TO_BAD_STOCK,
                OrderReturnResupplyStatusDTO.fromReturnResupplyStatus(
                OrderReturnResupplyStatus.reduce(
                        OrderReturnResupplyStatus.RESUPPLIED_TO_BAD_STOCK,
                        OrderReturnResupplyStatus.RESUPPLIED_TO_BAD_STOCK))

        );
    }
}

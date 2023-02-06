package ru.yandex.market.api.partner.controllers.order;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.order.OrderServiceControllerHelper.PapiModelMapper;
import ru.yandex.market.api.partner.controllers.order.model.OrderStatusDTO;
import ru.yandex.market.api.partner.controllers.order.model.OrderSubstatusDTO;
import ru.yandex.market.orderservice.client.model.OrderStatus;
import ru.yandex.market.orderservice.client.model.OrderSubStatus;

class PapiModelMapperTest {

    /**
     * Cм описание проблематики в OrderConverterTest#testSynchronizationOrderStatusesWithCheckouterClient.
     */
    @Test
    void testSynchronizationOrderStatusesWithOS() {
        final List<OrderStatus> unknown = Stream.of(OrderStatus.values())
                .filter(s -> PapiModelMapper.convertStatus(s) == OrderStatusDTO.UNKNOWN)
                .collect(Collectors.toList());

        Assertions.assertThat(unknown)
                .withFailMessage("Inconsistent status model between MBI and OS. You must read comments for this test!")
                .hasSameElementsAs(List.of(
                        OrderStatus.RETURNED,
                        OrderStatus.LOST,
                        OrderStatus.UNKNOWN
                ));
    }

    /**
     * Cм описание проблематики в OrderConverterTest#testSynchronizationOrderSubstatusesWithCheckouterClient.
     */
    @Test
    void testSynchronizationOrderSubStatusesWithOS() {
        final List<OrderSubStatus> unknown = Stream.of(OrderSubStatus.values())
                .filter(s -> PapiModelMapper.convertSubStatus(s) == OrderSubstatusDTO.UNKNOWN)
                .collect(Collectors.toList());

        Assertions.assertThat(unknown)
                .withFailMessage("Inconsistent sub-status model between MBI and OS. You must read comments for this test!")
                .hasSameElementsAs(List.of(
                        OrderSubStatus.USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                        OrderSubStatus.WRONG_ITEM_DELIVERED,
                        OrderSubStatus.DAMAGED_BOX,
                        OrderSubStatus.AWAIT_DELIVERY_DATES,
                        OrderSubStatus.UNKNOWN
                ));
    }

}

package ru.yandex.market.tpl.core.domain.partial_return_order;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderItemInstancePurchaseStatus;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.commands.PartialReturnOrderCommand;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
public class PartialReturnOrderCreateTest extends TplAbstractTest {
    private final PartialReturnOrderCommandService partialReturnOrderCommandService;
    private final PartialReturnOrderRepository partialReturnOrderRepository;
    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;

    @Test
    public void createPartialReturnOrderHappyTest() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        //пока нет целевого механизма проставления возврата инстансу, напишем в другом pr'е
        order.getItems().stream()
                .flatMap(OrderItem::streamInstances)
                .forEach(instance -> instance.setPurchaseStatus(OrderItemInstancePurchaseStatus.RETURNED));
        orderRepository.save(order);

        partialReturnOrderCommandService.create(
                PartialReturnOrderCommand.Create.builder()
                        .orderId(order.getId())
                        .build()
        );

        assertThat(partialReturnOrderRepository.findAll()).hasSize(1);
    }

    @Test
    public void tryCreatePartialReturnOrderWhenDisablePartialReturnOrder() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .build()
                        )
                        .build()
        );

        assertThatThrownBy(() -> partialReturnOrderCommandService.create(
                PartialReturnOrderCommand.Create.builder()
                        .orderId(order.getId())
                        .build())
        ).isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    public void tryCreatePartialReturnOrderWhenAllInstancesArePurchased() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        assertThatThrownBy(() -> partialReturnOrderCommandService.create(
                PartialReturnOrderCommand.Create.builder()
                        .orderId(order.getId())
                        .build())
        ).isInstanceOf(TplInvalidActionException.class);
    }

}

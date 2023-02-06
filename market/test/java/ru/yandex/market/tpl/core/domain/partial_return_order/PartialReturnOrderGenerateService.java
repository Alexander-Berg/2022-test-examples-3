package ru.yandex.market.tpl.core.domain.partial_return_order;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderItemInstancePurchaseStatus;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.commands.LinkBoxesPartialReturnOrderCommandHandler;
import ru.yandex.market.tpl.core.domain.partial_return_order.commands.PartialReturnOrderCommand;
import ru.yandex.market.tpl.core.domain.usershift.Task;

@Service
@RequiredArgsConstructor
@Transactional
public class PartialReturnOrderGenerateService {
    private final PartialReturnOrderCommandService partialReturnOrderCommandService;
    private final OrderRepository orderRepository;

    public PartialReturnOrder generatePartialReturnWithOnlyOneReturnItemInstance(Order order) {
        order = orderRepository.findByIdOrThrow(order.getId());
        order.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .findFirst()
                .ifPresent(instance -> instance.setPurchaseStatus(OrderItemInstancePurchaseStatus.RETURNED));
        orderRepository.save(order);

        return partialReturnOrderCommandService.create(
                PartialReturnOrderCommand.Create.builder()
                        .orderId(order.getId())
                        .build()
        );
    }

    public PartialReturnOrder generatePartialReturnWithAllReturnItemsInstances(Order order) {
        order = orderRepository.findByIdOrThrow(order.getId());
        order.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .forEach(instance -> instance.setPurchaseStatus(OrderItemInstancePurchaseStatus.RETURNED));
        orderRepository.save(order);

        return partialReturnOrderCommandService.create(
                PartialReturnOrderCommand.Create.builder()
                        .orderId(order.getId())
                        .build()
        );
    }

    public void generatePartialReturnBoxes(PartialReturnOrder partialReturnOrder, int size) {
        List<String> barcodes = IntStream.range(0, size)
                .mapToObj(i -> "BARCODE_" + partialReturnOrder.getId() + i).collect(Collectors.toList());

        partialReturnOrderCommandService.handleCommand(
                LinkBoxesPartialReturnOrderCommandHandler.builder()
                        .partialReturnId(partialReturnOrder.getId())
                        .newBoxesBarcodes(barcodes)
                        .build()
        );
    }

    public void generatePartialReturnBoxes(PartialReturnOrder partialReturnOrder, Task task, int size) {
        List<String> barcodes = IntStream.range(0, size)
                .mapToObj(i -> "BARCODE_" + partialReturnOrder.getId() + i).collect(Collectors.toList());

        partialReturnOrderCommandService.handleCommand(
                LinkBoxesPartialReturnOrderCommandHandler.builder()
                        .partialReturnId(partialReturnOrder.getId())
                        .newBoxesBarcodes(barcodes)
                        .taskId(task.getId())
                        .build()
        );
    }
}

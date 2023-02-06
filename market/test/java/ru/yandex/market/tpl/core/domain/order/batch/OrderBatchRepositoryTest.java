package ru.yandex.market.tpl.core.domain.order.batch;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.CoreTestV2;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@CoreTestV2
class OrderBatchRepositoryTest {

    private final OrderBatchRepository orderBatchRepository;
    private final OrderBatchCommandService orderBatchCommandService;
    private final TestDataFactory testDataFactory;

    @Test
    void findAllByBarcodeIn() {
        Order order1 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(OrderGenerateService.OrderGenerateParam.generateOrderPlaceDto(2))
                .build());
        Order order2 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(OrderGenerateService.OrderGenerateParam.generateOrderPlaceDto(3))
                .build());

        orderBatchCommandService.put(
                new OrderBatchCommand.Put(
                        "batch1",
                        OrderBatchPlaceMapper.mapOrderPlaces(order1.getPlaces())
                )
        );
        orderBatchCommandService.put(
                new OrderBatchCommand.Put(
                        "batch2",
                        OrderBatchPlaceMapper.mapOrderPlaces(order2.getPlaces())
                )
        );
        Set<OrderBatch> foundBatches = orderBatchRepository.findAllByBarcodeIn(Set.of("batch1", "batch2"));
        assertThat(foundBatches).extracting(OrderBatch::getBarcode).containsExactlyInAnyOrder("batch1", "batch2");
    }

}

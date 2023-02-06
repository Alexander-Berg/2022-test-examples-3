package ru.yandex.market.tpl.core.domain.order.batch;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlace;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderBatchCommandServiceTest {

    private final OrderBatchRepository orderBatchRepository;
    private final OrderBatchCommandService orderBatchCommandService;
    private final TestDataFactory testDataFactory;

    @Test
    void create() {
        Order order1 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(OrderGenerateService.OrderGenerateParam.generateOrderPlaceDto(2))
                .build());
        Order order2 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(OrderGenerateService.OrderGenerateParam.generateOrderPlaceDto(3))
                .build());

        List<OrderPlace> places = StreamEx.of(order1.getPlaces())
                .append(order2.getPlaces())
                .toList();
        orderBatchCommandService.put(
                new OrderBatchCommand.Put(
                        "barcode",
                        OrderBatchPlaceMapper.mapOrderPlaces(places)
                )
        );

        Optional<OrderBatch> foundOrderBatchO = orderBatchRepository.findByBarcode("barcode");
        assertThat(foundOrderBatchO).isNotEmpty();
        assertThat(foundOrderBatchO.get().getPlaces()).hasSize(5);

        orderBatchCommandService.put(
                new OrderBatchCommand.Put(
                        "barcode",
                        OrderBatchPlaceMapper.mapOrderPlaces(places.subList(0, 2))
                )
        );
        assertThat(foundOrderBatchO.get().getPlaces()).hasSize(2);
    }

    @Test
    void destroy() {
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(OrderGenerateService.OrderGenerateParam.generateOrderPlaceDto(2))
                .build());
        OrderBatch orderBatch = orderBatchCommandService.put(
                new OrderBatchCommand.Put(
                        "batch_barcode",
                        OrderBatchPlaceMapper.mapOrderPlaces(order.getPlaces())
                )
        );

        OrderPlace place = order.getPlaces().iterator().next();
        assertThat(place.getCurrentBatch()).isNotEmpty();
        assertThat(place.getCurrentBatch().get().getBarcode()).isEqualTo("batch_barcode");

        orderBatchCommandService.handleCommand(new OrderBatchCommand.Destroy(orderBatch.getId()));
        assertThat(place.getCurrentBatch()).isEmpty();
    }

    @Test
    void revertDestroy() {
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(OrderGenerateService.OrderGenerateParam.generateOrderPlaceDto(2))
                .build());
        OrderBatch orderBatch = orderBatchCommandService.put(
                new OrderBatchCommand.Put(
                        "batch_barcode",
                        OrderBatchPlaceMapper.mapOrderPlaces(order.getPlaces())
                )
        );

        OrderPlace place = order.getPlaces().iterator().next();

        orderBatchCommandService.handleCommand(new OrderBatchCommand.Destroy(orderBatch.getId()));
        orderBatchCommandService.handleCommand(new OrderBatchCommand.RevertDestroy(orderBatch.getId()));
        assertThat(place.getCurrentBatch()).isNotEmpty();
        assertThat(place.getCurrentBatch().get().getBarcode()).isEqualTo("batch_barcode");
    }

    @Test
    void addAndRemovePlaces() {
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(OrderGenerateService.OrderGenerateParam.generateOrderPlaceDto(2))
                .build());
        OrderBatch orderBatch = orderBatchCommandService.put(
                new OrderBatchCommand.Put(
                        "batch_barcode",
                        OrderBatchPlaceMapper.mapOrderPlaces(order.getPlaces())
                )
        );

        Iterator<OrderPlace> placeIterator = order.getPlaces().iterator();
        OrderPlace place1 = placeIterator.next();
        OrderPlace place2 = placeIterator.next();

        assertThat(place1.getCurrentBatch()).isNotEmpty();
        assertThat(place1.getCurrentBatch().get().getBarcode()).isEqualTo("batch_barcode");

        orderBatchCommandService.removePlaces(
                new OrderBatchCommand.RemovePlaces(
                        orderBatch.getId(),
                        List.of(place1)
                )
        );
        assertThat(place1.getCurrentBatch()).isEmpty();
        assertThat(getPlaceBarcodes(orderBatch.getPlaces()))
                .containsExactly(place2.getBarcode().getBarcode());

        orderBatchCommandService.addPlaces(
                new OrderBatchCommand.AddPlaces(
                        orderBatch.getId(),
                        List.of(OrderBatchPlaceMapper.mapOrderPlace(place1))
                )
        );

        assertThat(place1.getCurrentBatch()).isNotEmpty();
        assertThat(place1.getCurrentBatch().get().getBarcode()).isEqualTo("batch_barcode");

        assertThat(getPlaceBarcodes(orderBatch.getPlaces()))
                .containsExactlyInAnyOrder(
                        place1.getBarcode().getBarcode(),
                        place2.getBarcode().getBarcode()
                );
    }

    @Test
    void acceptAndDeliver() {
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(OrderGenerateService.OrderGenerateParam.generateOrderPlaceDto(2))
                .build());
        OrderBatch orderBatch = orderBatchCommandService.put(
                new OrderBatchCommand.Put(
                        "batch_barcode",
                        OrderBatchPlaceMapper.mapOrderPlaces(order.getPlaces())
                )
        );

        orderBatchCommandService.handleCommand(new OrderBatchCommand.Accept(orderBatch.getId()));
        assertThat(orderBatch.getStatus()).isEqualTo(OrderBatchStatus.ACCEPTED_BY_COURIER);

        orderBatchCommandService.handleCommand(new OrderBatchCommand.Deliver(orderBatch.getId()));
        assertThat(orderBatch.getStatus()).isEqualTo(OrderBatchStatus.DELIVERED);
    }

    private List<String> getPlaceBarcodes(Collection<OrderPlace> places) {
        return StreamEx.of(places)
                .map(OrderPlace::getBarcode)
                .filter(Objects::nonNull)
                .map(OrderPlaceBarcode::getBarcode)
                .toList();
    }
}

package ru.yandex.market.logistics.lom.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.dto.OrdersInfoPerShipmentDto;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.Shipment;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Cost;
import ru.yandex.market.logistics.lom.repository.ordershipment.OrdersShipmentRepository;

@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/order/search/orders.xml")
class OrdersShipmentRepositoryTest extends AbstractContextualTest {
    @Autowired
    private OrdersShipmentRepository repository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @ParameterizedTest
    @MethodSource("arguments")
    void test(
        List<Long> shipmentIds,
        List<Long> senderIds
    ) {
        transactionTemplate.execute(status -> {
            List<OrdersInfoPerShipmentDto> ordersInfoPerShipmentActual = repository
                .getOrdersInfoByShipmentIdsAndSenderIds(
                    shipmentIds,
                    senderIds
                );

            List<OrdersInfoPerShipmentDto> ordersInfoPerShipmentExpected = ordersInfoPerShipmentExpected(
                Set.copyOf(shipmentIds),
                Set.copyOf(senderIds)
            );

            softly.assertThat(ordersInfoPerShipmentActual)
                .usingFieldByFieldElementComparator()
                .containsAll(ordersInfoPerShipmentExpected);

            return null;
        });
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return StreamEx.of(List.of(1L), List.of(1L, 2L))
            .cross(List.of(List.of(1L), List.of(1L, 2L, 3L)))
            .mapKeyValue(Arguments::of);
    }

    @Nonnull
    private List<OrdersInfoPerShipmentDto> ordersInfoPerShipmentExpected(
        Set<Long> shipmentIds,
        Set<Long> senderIds
    ) {
        Map<Long, Order> shipmentToOrderMap = StreamEx.of(shipmentRepository.findAll())
            .mapToEntry(Shipment::getId, Shipment::getWaybill)
            .flatMapValues(Collection::stream)
            .mapValues(WaybillSegment::getOrder)
            .filterKeys(shipmentIds::contains)
            .filterValues(order -> senderIds.contains(order.getSender().getId()))
            .toImmutableMap();

        Map<Long, Long> shipmentToOrderCountMap = shipmentToOrderMap.entrySet()
            .stream()
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.counting()
                )
            );

        Map<Long, BigDecimal> shipmentToOrderCostMap = EntryStream.of(shipmentToOrderMap)
            .mapValues(Order::getCost)
            .nonNullValues()
            .mapValues(Cost::getTotal)
            .nonNullValues()
            .collapseKeys()
            .mapValues(
                costs -> costs.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            )
            .toMap();

        return shipmentToOrderMap.keySet()
            .stream()
            .map(
                shipmentId -> new OrdersInfoPerShipmentDto(
                    shipmentId,
                    shipmentToOrderCountMap.get(shipmentId),
                    shipmentToOrderCostMap.get(shipmentId)
                )
            )
            .map(OrdersInfoPerShipmentDto.class::cast)
            .collect(Collectors.toList());
    }
}

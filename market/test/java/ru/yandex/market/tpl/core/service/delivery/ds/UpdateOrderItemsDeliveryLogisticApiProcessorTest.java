package ru.yandex.market.tpl.core.service.delivery.ds;

import java.io.IOException;
import java.time.Clock;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.logistic.api.model.delivery.request.UpdateOrderItemsRequest;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceRepository;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.service.delivery.LogisticApiRequestProcessingConfiguration;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
@ContextConfiguration(classes = {
        LogisticApiRequestProcessingConfiguration.class
})
class UpdateOrderItemsDeliveryLogisticApiProcessorTest {

    private final UpdateOrderItemsDeliveryLogisticApiProcessor processor;
    private final PartnerRepository<DeliveryService> partnerRepository;
    private final OrderRepository orderRepository;
    private final OrderPlaceRepository orderPlaceRepository;
    private final DsRequestReader dsRequestReader;
    @MockBean
    private Clock clock;

    private DeliveryService partner;

    @BeforeEach
    void setUp() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        partner = partnerRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
    }

    @Test
    void apiCall() throws IOException {
        dsRequestReader.sendCreateOrder("/ds/create_order_with_multiple_items.xml", "123", partner);
        UpdateOrderItemsRequest updateOrderItemsRequest = dsRequestReader.readRequest(
                "/ds/update_order_items.xml",
                UpdateOrderItemsRequest.class,
                "123"
        );
        Order orderBeforeItemUpdate = orderRepository.findByExternalOrderId("123").orElseThrow();
        long placesBeforeUpdate = orderPlaceRepository.count();
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(OrderItem::isService)
                .hasSize(1);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(Predicate.not(OrderItem::isService))
                .hasSize(2);
        processor.apiCall(updateOrderItemsRequest, partner);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(OrderItem::isService)
                .hasSize(1);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(Predicate.not(OrderItem::isService))
                .hasSize(1);
        assertThat(orderPlaceRepository.count()).isEqualTo(placesBeforeUpdate);
        checkOrderItemsInstancesCount(orderBeforeItemUpdate);
    }

    @Test
    void apiCallWithReducingCount() throws IOException {
        dsRequestReader.sendCreateOrder("/ds/create_order_with_multiple_items_multi_count.xml", "123", partner);
        UpdateOrderItemsRequest updateOrderItemsRequest = dsRequestReader.readRequest(
                "/ds/update_order_items.xml",
                UpdateOrderItemsRequest.class,
                "123"
        );
        Order orderBeforeItemUpdate = orderRepository.findByExternalOrderId("123").orElseThrow();
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(OrderItem::isService)
                .hasSize(1);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(Predicate.not(OrderItem::isService))
                .hasSize(2);

        assertThat(((Integer) orderBeforeItemUpdate.getItems().stream().filter(e -> !e.isService())
                .map(OrderItem::getCount).mapToInt(e -> e).sum())).isEqualTo(5);
        long placesBeforeUpdate = orderPlaceRepository.count();
        processor.apiCall(updateOrderItemsRequest, partner);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(OrderItem::isService)
                .hasSize(1);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(Predicate.not(OrderItem::isService))
                .hasSize(1);
        assertThat(((Integer) orderBeforeItemUpdate.getItems().stream().filter(e -> !e.isService())
                .map(OrderItem::getCount).mapToInt(e -> e).sum())).isEqualTo(1);
        assertThat(orderPlaceRepository.count()).isEqualTo(placesBeforeUpdate);
        checkOrderItemsInstancesCount(orderBeforeItemUpdate);
    }

    @Test
    void apiCallWithReducingCountWithNullableArticle() throws IOException {
        dsRequestReader.sendCreateOrder("/ds/create_order_with_multiple_items_multi_count.xml", "123", partner);
        UpdateOrderItemsRequest updateOrderItemsRequest = dsRequestReader.readRequest(
                "/ds/update_order_items_multi_count_null_article.xml",
                UpdateOrderItemsRequest.class,
                "123"
        );
        Order orderBeforeItemUpdate = orderRepository.findByExternalOrderId("123").orElseThrow();
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(OrderItem::isService)
                .hasSize(1);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(Predicate.not(OrderItem::isService))
                .hasSize(2);

        assertThat(((Integer) orderBeforeItemUpdate.getItems().stream().filter(e -> !e.isService())
                .map(OrderItem::getCount).mapToInt(e -> e).sum())).isEqualTo(5);
        long placesBeforeUpdate = orderPlaceRepository.count();
        processor.apiCall(updateOrderItemsRequest, partner);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(OrderItem::isService)
                .hasSize(1);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(Predicate.not(OrderItem::isService))
                .hasSize(2);
        assertThat(((Integer) orderBeforeItemUpdate.getItems().stream().filter(e -> !e.isService())
                .map(OrderItem::getCount).mapToInt(e -> e).sum())).isEqualTo(3);

        assertThat(orderBeforeItemUpdate.getItems().stream().filter(e -> !e.isService())
                .filter(e -> CollectionUtils.isNonEmpty(e.getPlaceItems())).count()).isEqualTo(0);
        assertThat(orderPlaceRepository.count()).isEqualTo(placesBeforeUpdate);
        checkOrderItemsInstancesCount(orderBeforeItemUpdate);
    }

    @Test
    void apiCallWithChangingCount() throws IOException {
        dsRequestReader.sendCreateOrder("/ds/create_order_with_multiple_items_multi_count.xml", "123", partner);
        UpdateOrderItemsRequest updateOrderItemsRequest = dsRequestReader.readRequest(
                "/ds/update_order_items_multi_count.xml",
                UpdateOrderItemsRequest.class,
                "123"
        );
        Order orderBeforeItemUpdate = orderRepository.findByExternalOrderId("123").orElseThrow();
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(OrderItem::isService)
                .hasSize(1);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(Predicate.not(OrderItem::isService))
                .hasSize(2);

        assertThat(((Integer) orderBeforeItemUpdate.getItems().stream().filter(e -> !e.isService())
                .map(OrderItem::getCount).mapToInt(e -> e).sum())).isEqualTo(5);
        long placesBeforeUpdate = orderPlaceRepository.count();
        processor.apiCall(updateOrderItemsRequest, partner);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(OrderItem::isService)
                .hasSize(1);
        assertThat(orderBeforeItemUpdate.getItems())
                .filteredOn(Predicate.not(OrderItem::isService))
                .hasSize(2);
        assertThat(((Integer) orderBeforeItemUpdate.getItems().stream().filter(e -> !e.isService())
                .map(OrderItem::getCount).mapToInt(e -> e).sum())).isEqualTo(3);
        assertThat(orderPlaceRepository.count()).isEqualTo(placesBeforeUpdate);
        checkOrderItemsInstancesCount(orderBeforeItemUpdate);
    }

    private void checkOrderItemsInstancesCount(Order order) {
        order.getItems().forEach(
                i -> {
                    if (i.isService()) {
                        assertThat(i.streamInstances().count()).isEqualTo(0);
                    } else {
                        assertThat(i.getCount()).isEqualTo(i.streamInstances().count());
                    }
                }
        );
    }

}

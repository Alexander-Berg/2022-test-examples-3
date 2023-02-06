package ru.yandex.market.pvz.core.service.delivery.order;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.delivery.request.UpdateItemsInstancesRequest;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.service.delivery.DsApiBaseTest;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UpdateItemsInstancesDsApiProcessorTest extends DsApiBaseTest {

    private final TestOrderFactory orderFactory;

    private final UpdateItemsInstancesDsApiProcessor processor;
    private final OrderQueryService orderQueryService;


    @Test
    void test() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(TestOrderFactory.OrderItemParams.builder()
                                .vendorId(14353L)
                                .vendorArticle("23454")
                                .build()))
                        .build())
                .build());

        var request = readRequest("/ds/order/update_items_instances.xml", UpdateItemsInstancesRequest.class, Map.of(
                "yandex_id", order.getExternalId(),
                "vendor_id", 14353L,
                "vendor_article", "23454"
        ));

        processor.apiCall(request, order.safeGetDeliveryService());

        var updatedOrder = orderQueryService.get(order.getId());
        assertThat(updatedOrder.getItems()).hasSize(1);
        assertThat(updatedOrder.getItems().get(0).getCisValues())
                .containsExactlyInAnyOrderElementsOf(List.of("CIS-1", "CIS-2"));
        assertThat(updatedOrder.getItems().get(0).getCisFullValues())
                .containsExactlyInAnyOrderElementsOf(List.of("CIS-1-full", "CIS-2-full"));
    }

}

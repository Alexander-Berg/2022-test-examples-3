package ru.yandex.market.tpl.core.domain.order;

import java.util.List;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderHistoryServiceTest {

    private final TestDataFactory testDataFactory;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final OrderManager orderManager;

    @Test
    void cancelOrderTest() {
        String externalOrderId = "3513orderID!";
        Long orderId = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId(externalOrderId)
                .build())
                .getId();
        assertThat(orderId).isNotNull();
        testDataFactory.flushAndClear();

        orderManager.cancelOrder(
                orderId,
                new OrderDeliveryFailReasonDto(
                        OrderDeliveryTaskFailReasonType.OTHER,
                        "частые переносы",
                        Source.OPERATOR
                )
        );

        List<OrderHistoryEvent> events = orderHistoryEventRepository.findAllByOrderId(orderId);

        OrderHistoryEvent cancelEvent = StreamEx.of(events)
                .filter(e -> e.getType() == OrderEventType.CANCELLED)
                .findAny()
                .orElseThrow();

        assertThat(cancelEvent.getSource()).isEqualTo(Source.OPERATOR);
    }

}

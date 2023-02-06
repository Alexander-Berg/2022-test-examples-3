package ru.yandex.market.logistics.lom.service.order;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;

public class OrderWaybillOrderingTest extends AbstractContextualTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("/service/order/before/order_waybill_indexes.xml")
    @DisplayName("Проверяем, что сегменты лежат в вейбиле в нужном порядке")
    void orderFieldsCheck() {
        transactionTemplate.execute(arg -> {
            Order order = orderService.findById(1L);
            List<WaybillSegment> waybill = order.getWaybill();
            softly.assertThat(waybill.size()).isEqualTo(3);
            softly.assertThat(waybill.get(0).getWaybillSegmentIndex()).isEqualTo(0);
            softly.assertThat(waybill.get(1).getWaybillSegmentIndex()).isEqualTo(1);
            softly.assertThat(waybill.get(2).getWaybillSegmentIndex()).isEqualTo(2);
            return 0;
        });
    }

}

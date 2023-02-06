package ru.yandex.travel.orders.services.indexing;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.travel.orders.TestOrderItem;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.repository.OrderChangeRepository;
import ru.yandex.travel.orders.repository.OrderRepository;
import ru.yandex.travel.orders.workflows.order.OrderCreateHelper;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class OrdersIndexingTest {
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderChangeRepository orderChangeRepository;

    @Test
    public void testIndexingWorks() {
        transactionTemplate.execute(
                (notUsed) -> {
                    HotelOrder newOrder = OrderCreateHelper.createTestHotelOrder();
                    TestOrderItem orderItem = new TestOrderItem();
                    orderItem.setPayload(OrderCreateHelper.createRandomJson(10));
                    newOrder.addOrderItem(orderItem);

                    newOrder = orderRepository.saveAndFlush(newOrder);

                    return newOrder.getId();
                }
        );
        transactionTemplate.execute(
                (notUsed) -> {
                    var change = orderChangeRepository.findAll();
                    assertThat(change).isNotNull();
                    var orderIdsForIndex = orderChangeRepository.getOrdersWaitingForIndexing(List.of(), PageRequest.of(0, 10));
                    assertThat(orderIdsForIndex.size()).isEqualTo(1);
                    var orderChangesForIndex = orderChangeRepository.countOrdersWaitingForIndexing(List.of());
                    assertThat(orderChangesForIndex).isEqualTo(1);
                    return null;
                }
        );
    }
}

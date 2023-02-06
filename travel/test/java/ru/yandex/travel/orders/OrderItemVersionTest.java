package ru.yandex.travel.orders;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.travel.hotels.common.orders.ExpediaHotelItinerary;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.orders.entities.ExpediaOrderItem;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.Order;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.repository.OrderRepository;
import ru.yandex.travel.orders.workflow.hotels.expedia.proto.EExpediaItemState;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
//@TestExecutionListeners(listeners = TruncateDatabaseTestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class OrderItemVersionTest {
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    public void testVersion() {
        UUID orderId = UUID.randomUUID();
        transactionTemplate.execute(ignored -> {
            Order order = new HotelOrder();
            order.setId(orderId);
            order = orderRepository.saveAndFlush(order);
            ExpediaOrderItem orderItem = new ExpediaOrderItem();
            orderItem.setId(UUID.randomUUID());
            orderItem.setState(EExpediaItemState.IS_NEW);
            ExpediaHotelItinerary itinerary = new ExpediaHotelItinerary();
            itinerary.setCustomerPhone("123");
            itinerary.setOrderDetails(OrderDetails.builder().checkinDate(LocalDate.now()).build());
            orderItem.setItinerary(itinerary);
            order.addOrderItem(orderItem);
            return null;
        });

        var version = transactionTemplate.execute(ignored -> {
            Order order = orderRepository.findById(orderId).orElseThrow();
            OrderItem orderItem = order.getOrderItems().get(0);

            var itinerary = (ExpediaHotelItinerary) orderItem.getPayload();
            assertThat(itinerary.getCustomerPhone()).isEqualTo("123");
            itinerary.setCustomerPhone("321");
            return orderItem.getVersion();
        });

        var version2 = transactionTemplate.execute(ignored -> {
            Order order = orderRepository.findById(orderId).orElseThrow();

            var itinerary = (ExpediaHotelItinerary) order.getOrderItems().get(0).getPayload();
            assertThat(itinerary.getCustomerPhone()).isEqualTo("321");
            return order.getOrderItems().get(0).getVersion();
        });

        assertThat(version2).isNotEqualTo(version);
    }
}


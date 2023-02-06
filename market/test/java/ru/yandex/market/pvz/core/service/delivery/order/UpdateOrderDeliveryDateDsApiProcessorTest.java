package ru.yandex.market.pvz.core.service.delivery.order;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.delivery.request.CreateOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOrdersDeliveryDateRequest;
import ru.yandex.market.logistic.api.model.delivery.request.UpdateOrderDeliveryDateRequest;
import ru.yandex.market.logistic.api.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersDeliveryDateResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderDeliveryDate;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.TimeInterval;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.service.delivery.DsApiBaseTest;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval.valueOf;


@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UpdateOrderDeliveryDateDsApiProcessorTest extends DsApiBaseTest {

    public static final LocalDate DELIVERY_DATE = LocalDate.of(2020, 7, 16);

    private final DsOrderManager dsOrderManager;
    private final TestPickupPointFactory testPickupPointFactory;
    private final OrderRepository orderRepository;
    private final Clock clock;

    private PickupPoint pickupPoint;
    private String yandexId;
    private Long orderId;

    @BeforeEach
    void setUp() {
        pickupPoint = testPickupPointFactory.createPickupPoint();
        CreateOrderRequest request = readRequest("/ds/order/create_order.xml",
                CreateOrderRequest.class, Map.of(
                        "pickup_point_code", pickupPoint.getId(),
                        "delivery_date", DELIVERY_DATE.toString()
                ));
        CreateOrderResponse response = dsOrderManager.createOrder(request.getOrder(),
                pickupPoint.getLegalPartner().getDeliveryService());
        yandexId = response.getOrderId().getYandexId();
        orderId = Long.parseLong(response.getOrderId().getPartnerId());
    }

    @Test
    public void testGetOrdersDeliveryDateWhenNoInterval() {
        GetOrdersDeliveryDateResponse ordersDeliveryDate = getGetOrdersDeliveryDateResponse();
        assertThat(ordersDeliveryDate.getOrderDeliveryDates().get(0).getDeliveryDate())
                .isEqualTo(DateTime.fromLocalDateTime(
                        DELIVERY_DATE.atTime(LocalTime.of(DsOrderManager.DEFAULT_INTERVAL.getStart().getHour(), 0))
                ));
    }

    @Test
    public void testUpdateOrderDeliveryDate() {
        LocalDate deliveryDate = LocalDate.now(clock);
        LocalTimeInterval localTimeInterval = valueOf("10:00-14:00");
        UpdateOrderDeliveryDateRequest uoddRequest = readRequest("/ds/order/update_order_delivery_date.xml",
                UpdateOrderDeliveryDateRequest.class,
                Map.of("order_id", yandexId,
                        "delivery_date", deliveryDate.toString(),
                        "delivery_interval", localTimeInterval.toString()
                )
        );
        var deliveryService = pickupPoint.getLegalPartner().getDeliveryService();
        dsOrderManager.updateOrderDeliveryDate(uoddRequest.getOrderDeliveryDate(), deliveryService);

        Order order = orderRepository.findByIdOrThrow(orderId);
        assertThat(order.getDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(new Interval(order.getDeliveryIntervalFrom(), order.getDeliveryIntervalTo()))
                .isEqualTo(localTimeInterval.toInterval(deliveryDate, DateTimeUtil.DEFAULT_ZONE_ID));

        GetOrdersDeliveryDateResponse getOrdersDeliveryDateResponse = getGetOrdersDeliveryDateResponse();
        OrderDeliveryDate orderDeliveryDate = getOrdersDeliveryDateResponse.getOrderDeliveryDates().get(0);
        assertThat(orderDeliveryDate.getDeliveryDate())
                .isEqualTo(DateTime.fromLocalDateTime(DateTimeUtil.toLocalDateTime(order.getDeliveryIntervalFrom())));
        assertThat(orderDeliveryDate.getDeliveryInterval())
                .isEqualTo(TimeInterval.of(DateTimeUtil.toLocalTime(order.getDeliveryIntervalFrom()),
                        DateTimeUtil.toLocalTime(order.getDeliveryIntervalTo())));
    }

    private GetOrdersDeliveryDateResponse getGetOrdersDeliveryDateResponse() {
        GetOrdersDeliveryDateRequest goddRequest = readRequest("/ds/order/get_orders_delivery_date.xml",
                GetOrdersDeliveryDateRequest.class,
                Map.of("order_id", yandexId)
        );
        var deliveryService = pickupPoint.getLegalPartner().getDeliveryService();
        return dsOrderManager.getOrdersDeliveryDate(goddRequest.getOrdersId(), deliveryService);
    }
}

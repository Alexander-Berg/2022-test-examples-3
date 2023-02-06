package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersBySupplierShipmentDateTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    private Order order1;
    private Order order2;

    private static Instant extractShipmentDate(Order o) {
        return o.getDelivery().getParcels().get(0).getParcelItems().get(0).getSupplierShipmentDateTime();
    }

    @BeforeAll
    public void setUp() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));
        order1 = orderCreateHelper.createOrder(parameters);
        setFixedTime(getClock().instant().plus(1, ChronoUnit.DAYS));
        order2 = orderCreateHelper.createOrder(parameters);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDown() {
        super.tearDownBase();
    }

    @Test
    public void shouldFilterByShipmentDateTo() {
        OrderSearchRequest request = new OrderSearchRequest();
        request.supplierShipmentDateTo = Date.from(extractShipmentDate(order2));

        Collection<Long> orderIds = orderService.getOrderIds(request, ClientInfo.SYSTEM);
        assertThat(orderIds, hasItem(order1.getId()));
        assertThat(orderIds, not(hasItem(order2.getId())));
    }

    @Test
    public void shouldFilterByShipmentDateFrom() {
        OrderSearchRequest request = new OrderSearchRequest();
        request.supplierShipmentDateFrom = Date.from(extractShipmentDate(order2));

        Collection<Long> orderIds = orderService.getOrderIds(request, ClientInfo.SYSTEM);
        assertThat(orderIds, hasItem(order2.getId()));
        assertThat(orderIds, not(hasItem(order1.getId())));
    }

    @Test
    public void shouldFilterByShipmentDateFromAndTo() {
        OrderSearchRequest request = new OrderSearchRequest();
        request.supplierShipmentDateFrom = Date.from(extractShipmentDate(order1));
        request.supplierShipmentDateTo = Date.from(extractShipmentDate(order2));

        Collection<Long> orderIds = orderService.getOrderIds(request, ClientInfo.SYSTEM);
        assertThat(orderIds, hasItem(order1.getId()));
        assertThat(orderIds, not(hasItem(order2.getId())));
    }

    @Test
    public void shouldFilterByShipmentDateToAndPaymentStatus() {
        OrderSearchRequest request = new OrderSearchRequest();
        request.supplierShipmentDateFrom = Date.from(extractShipmentDate(order2));
        request.paymentStatus = EnumSet.of(PaymentStatus.INIT);

        Collection<Long> orderIds = orderService.getOrderIds(request, ClientInfo.SYSTEM);
        assertThat(orderIds, is(empty()));
    }
}

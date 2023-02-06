package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByShipmentDateTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    private Order order1;
    private Order order2;
    //ДСБС заказ
    private Order order3;
    private Order order4;

    private static LocalDate extractShipmentDate(Order o) {
        return o.getDelivery().getParcels().get(0).getShipmentDate();
    }

    @BeforeAll
    public void setUp() throws Exception {
        Parameters dsbsParameters = WhiteParametersProvider.simpleWhiteParameters();
        dsbsParameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfDelivery()
                .dates(DeliveryDates.deliveryDates(getClock(), 0, 1))
                .buildResponse(DeliveryResponse::new));
        dsbsParameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        order3 = orderCreateHelper.createOrder(dsbsParameters);
        order4 = orderCreateHelper.createOrder(dsbsParameters);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));
        LocalDate assumedShipmentDate = getClock().instant().plus(1, ChronoUnit.DAYS).atZone(ZoneId.systemDefault())
                .toLocalDate();
        order1 = orderCreateHelper.createOrder(parameters);
        setFixedTime(getClock().instant().plus(1, ChronoUnit.DAYS));
        order2 = orderCreateHelper.createOrder(parameters);

        assertThat(extractShipmentDate(order1), is(assumedShipmentDate));
        assertThat(extractShipmentDate(order2), is(assumedShipmentDate.plusDays(1)));
        assertThat(extractShipmentDate(order3), is(assumedShipmentDate.minusDays(1)));
        assertThat(extractShipmentDate(order4), is(assumedShipmentDate.minusDays(1)));
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @Test
    public void shouldFilterByShipmentDateTo() {
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(BLUE, WHITE)
                .withShipmentDateTo(
                        Date.from(extractShipmentDate(order2).atStartOfDay(ZoneId.systemDefault()).toInstant())
                ).build();

        Collection<Long> orderIds = orderService.getOrderIds(request, ClientInfo.SYSTEM);
        assertThat(orderIds, hasItem(order1.getId()));
        assertThat(orderIds, hasItem(order3.getId()));
        assertThat(orderIds, hasItem(order4.getId()));
        assertThat(orderIds, not(hasItem(order2.getId())));
    }

    @Test
    public void shouldFilterByShipmentDateFrom() {
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(BLUE, WHITE)
                .withShipmentDateFrom(
                        Date.from(extractShipmentDate(order2).atStartOfDay(ZoneId.systemDefault()).toInstant())
                )
                .build();

        Collection<Long> orderIds = orderService.getOrderIds(request, ClientInfo.SYSTEM);
        assertThat(orderIds, hasItem(order2.getId()));
        assertThat(orderIds, not(hasItem(order1.getId())));
    }

    @Test
    public void shouldFilterByShipmentDateFromAndTo() {
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(BLUE, WHITE)
                .withShipmentDateFrom(
                        Date.from(extractShipmentDate(order1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                )
                .withShipmentDateTo(
                        Date.from(extractShipmentDate(order2).atStartOfDay(ZoneId.systemDefault()).toInstant())
                )
                .build();

        Collection<Long> orderIds = orderService.getOrderIds(request, ClientInfo.SYSTEM);
        assertThat(orderIds, hasItem(order1.getId()));
        assertThat(orderIds, not(hasItem(order2.getId())));
    }

    @Test
    public void shouldFilterByShipmentDateToAndPaymentStatus() {
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(BLUE, WHITE)
                .withShipmentDateTo(
                        Date.from(extractShipmentDate(order2).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .withPaymentStatus(new PaymentStatus[]{PaymentStatus.INIT})
                .build();

        Collection<Long> orderIds = orderService.getOrderIds(request, ClientInfo.SYSTEM);
        assertThat(orderIds, is(empty()));
    }
}

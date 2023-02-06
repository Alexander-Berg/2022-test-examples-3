package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.math.BigDecimal;
import java.util.Collections;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.common.report.model.ActualDeliveryResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class ActualDeliveryPriceForShopTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    private Parameters parameters;

    @BeforeEach
    public void setUp() {
        parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
    }

    @Test
    public void createOrderWithPriceForShop() {
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );

        BigDecimal expectedPriceForShop = BigDecimal.valueOf(951L);

        Iterables.getOnlyElement(
                actualDeliveryResult.getDelivery()
        ).setPriceForShop(expectedPriceForShop);

        Order order = orderCreateHelper.createOrder(parameters);

        Order createdOrder = orderService.getOrder(order.getId());

        assertNotNull(createdOrder.getDelivery().getPriceForShop());
        assertThat(expectedPriceForShop, comparesEqualTo(createdOrder.getDelivery().getPriceForShop()));
    }

    @Test
    public void shouldCreateOrderWithoutPriceForShop() {
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        Iterables.getOnlyElement(
                actualDeliveryResult.getDelivery()
        ).setPriceForShop(null);

        Order order = orderCreateHelper.createOrder(parameters);

        Order createdOrder = orderService.getOrder(order.getId());

        assertNull(createdOrder.getDelivery().getPriceForShop());
    }
}

package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class CreateOrderWarehouseIdTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void shouldSaveWarehouseId() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setWarehouseForAllItems(FulfilmentProvider.TEST_WAREHOUSE_ID);
        parameters.getOrder().getItems().forEach(item -> {
            item.setSupplierId(123L);
            item.setMsku(123L);
        });
        parameters.setDimensions("10", "10", "10");
        parameters.setWeight(BigDecimal.TEN);

        Order order = orderCreateHelper.createOrder(parameters);
        order = orderService.getOrder(order.getId());
        OrderItem item = Iterables.getOnlyElement(order.getItems());

        assertThat(item.getWarehouseId(), is(FulfilmentProvider.TEST_WAREHOUSE_ID));
        assertThat(item.getFulfilmentWarehouseId(), equalTo(FulfilmentProvider.TEST_WAREHOUSE_ID.longValue()));
    }

    @Test
    public void shouldSetFulfillmentWarehouseId() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withOrder(defaultBlueOrderParameters().getOrder())
                .buildParameters();
        parameters.getOrder().getItems()
                .forEach(oi -> {
                    ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(oi.getFeedOfferId());
                    itemInfo.getFulfilment().warehouseId = 700;
                    itemInfo.setFulfillmentWarehouseId(172L);
                    itemInfo.setAtSupplierWarehouse(true);
                });
        Order order = orderCreateHelper.createOrder(parameters);
        checkOrder(order);

        Order orderFromGet = orderService.getOrder(order.getId());
        checkOrder(orderFromGet);
    }

    private void checkOrder(Order order) throws Exception {
        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.isFulfilment(), is(true));
        assertThat(order.getDelivery().getDeliveryPartnerType(), is(DeliveryPartnerType.YANDEX_MARKET));

        OrderItem item = order.getItems().iterator().next();

        assertThat(item.getWarehouseId(), is(700));
        assertThat(item.getAtSupplierWarehouse(), is(true));
        assertThat(item.getFulfilmentWarehouseId(), equalTo(172L));

        assertThat(item.getFulfilmentWarehouseId(), equalTo(172L));
    }
}

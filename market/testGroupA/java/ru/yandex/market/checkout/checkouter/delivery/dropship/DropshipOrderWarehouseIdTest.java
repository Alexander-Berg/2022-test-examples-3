package ru.yandex.market.checkout.checkouter.delivery.dropship;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DropshipOrderWarehouseIdTest extends AbstractWebTestBase {

    private static final int ANOTHER_WAREHOUSE_ID = 9999;

    @Test
    public void shouldSaveWarehouseId() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        OrderItem orderItem = Iterables.getOnlyElement(parameters.getOrder().getItems());

        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId()).setFulfilment(
                new ItemInfo.Fulfilment(FulfilmentProvider.FF_SHOP_ID, FulfilmentProvider.ANOTHER_TEST_SKU,
                        FulfilmentProvider.ANOTHER_TEST_SHOP_SKU, ANOTHER_WAREHOUSE_ID, false)
        );
        shopService.updateMeta(FulfilmentProvider.FF_SHOP_ID,
                ShopSettingsHelper.createCustomNewPrepayMeta(FulfilmentProvider.FF_SHOP_ID.intValue()));

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getItem(orderItem.getFeedOfferId()).getWarehouseId(), is(ANOTHER_WAREHOUSE_ID));
    }
}

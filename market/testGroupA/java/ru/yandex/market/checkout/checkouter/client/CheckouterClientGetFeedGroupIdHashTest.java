package ru.yandex.market.checkout.checkouter.client;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
public class CheckouterClientGetFeedGroupIdHashTest extends AbstractWebTestBase {

    @Test
    public void shouldNotReturnFeedGroupIdHashIfNotPresentInReport() throws Exception {
        OrderItem orderItem = OrderItemProvider.getOrderItem();

        Order order = OrderProvider.getBlueOrder(o -> {
            o.setItems(Collections.singletonList(orderItem));
        });

        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId());
        itemInfo.setFeedGroupIdHash(null);
        itemInfo.getFulfilment().fulfilment = false;

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        Order resultOrder = client.getOrder(createdOrder.getId(), ClientRole.USER, BuyerProvider.UID);

        assertThat(resultOrder.getId()).isEqualTo(createdOrder.getId());
        OrderItem resultItem = resultOrder.getItem(orderItem.getFeedOfferId());
        assertThat(resultItem).isNull();
    }

}

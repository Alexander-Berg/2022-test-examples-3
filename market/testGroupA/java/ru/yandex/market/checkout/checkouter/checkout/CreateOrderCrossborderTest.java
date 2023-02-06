package ru.yandex.market.checkout.checkouter.checkout;

import com.google.common.collect.Iterables;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;

public class CreateOrderCrossborderTest extends AbstractWebTestBase {

    private static final Long SAMPLE_EXTERNAL_FEED_ID = 4567789L;

    @Tag(Tags.CROSSBORDER)
    @Test
    public void shouldSaveExternalFeedIdOnItem() {
        Parameters parameters = new Parameters();
        OrderItem orderItem = Iterables.getOnlyElement(parameters.getOrder().getItems());

        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId())
                .setExternalFeedId(SAMPLE_EXTERNAL_FEED_ID);

        Order order = orderCreateHelper.createOrder(parameters);
        order = orderService.getOrder(order.getId());

        assertThat(order.getItem(orderItem.getFeedOfferId()).getExternalFeedId(),
                CoreMatchers.is(SAMPLE_EXTERNAL_FEED_ID));
    }
}

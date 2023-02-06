package ru.yandex.market.checkout.checkouter.pictures;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class ItemPictureHelperIntegrationTest extends AbstractWebTestBase {

    @Test
    void shouldRemovePictureSize() {
        Order order = orderCreateHelper.createOrder(new Parameters());

        OrderItem item = order.getItems().iterator().next();
        MatcherAssert.assertThat(item.getPictures(), hasSize(1));
        MatcherAssert.assertThat(item.getPictures().iterator().next().getUrl(), endsWith("/"));
        MatcherAssert.assertThat(item.getPictures().iterator().next().getUrl(), not(endsWith("50x50")));
    }
}

package ru.yandex.market.checkout.checkouter.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SberIdOrderCreationTest extends AbstractWebTestBase {

    @DisplayName("Сериализация длинных UID для СберИД")
    @Test
    public void serializationOfSberId() {
        final Buyer sberIdBuyer = BuyerProvider.getSberIdBuyer();
        final Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(sberIdBuyer);
        final Order sberIdOrder = orderCreateHelper.createOrder(parameters);
        assertEquals(Color.BLUE, sberIdOrder.getRgb());
        assertEquals(BuyerProvider.SBER_ID, sberIdOrder.getBuyer().getUid().longValue());

        Order order = client.getOrder(sberIdOrder.getId(), ClientRole.SYSTEM, null);
        assertNotNull(order);
        assertEquals(BuyerProvider.SBER_ID, order.getBuyer().getUid().longValue());

        order = client.getOrder(sberIdOrder.getId(), ClientRole.CALL_CENTER_OPERATOR, 1121L);
        assertNotNull(order);
        assertEquals(BuyerProvider.SBER_ID, order.getBuyer().getUid().longValue());
    }

    @Test
    public void serializationOfLongUid() {
        final long longUid = 2190550858753016800L;
        final Buyer buyer = BuyerProvider.getDefaultBuyer(longUid);
        final Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(buyer);
        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(Color.BLUE, order.getRgb());
        assertEquals(longUid, order.getBuyer().getUid().longValue());

        order = client.getOrder(order.getId(), ClientRole.SYSTEM, null);
        assertNotNull(order);
        assertEquals(longUid, order.getBuyer().getUid().longValue());

        order = client.getOrder(order.getId(), ClientRole.CALL_CENTER_OPERATOR, 1121L);
        assertNotNull(order);
        assertEquals(longUid, order.getBuyer().getUid().longValue());
    }
}

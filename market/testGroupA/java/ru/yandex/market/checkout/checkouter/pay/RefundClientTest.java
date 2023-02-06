package ru.yandex.market.checkout.checkouter.pay;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.client.ClientRole;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class RefundClientTest extends AbstractPaymentTestBase {

    @BeforeEach
    public void prepareOrder() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
    }

    @Epic(Epics.REFUND)
    @Story(Stories.REFUND)
    @Test
    public void shouldGetRefundableItems() {
        RefundableItems refundableItems = client
                .refunds().getRefundableItems(order().getId(), ClientRole.SHOP, order.get().getShopId(), null);

        Assertions.assertNotNull(refundableItems);
        Assertions.assertFalse(refundableItems.canRefundAmount());
        assertThat(refundableItems.getItems(), hasSize(1));

        Assertions.assertNotNull(refundableItems.getDelivery());
        Assertions.assertTrue(refundableItems.getDelivery().isRefundable());
    }
}

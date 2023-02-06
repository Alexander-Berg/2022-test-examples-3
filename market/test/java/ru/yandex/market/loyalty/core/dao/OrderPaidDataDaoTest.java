package ru.yandex.market.loyalty.core.dao;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.discount.PaymentFeature;
import ru.yandex.market.loyalty.core.model.trigger.event.CoreOrderStatus;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderPaidData;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

@TestFor(OrderPaidDataDao.class)
public class OrderPaidDataDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private OrderPaidDataDao orderPaidDataDao;

    @Test
    public void shouldUpsertCorrectly() {
        Long orderId = 1L;
        String multiOrderId = UUID.randomUUID().toString();
        CoreOrderStatus orderStatus = CoreOrderStatus.PROCESSING;
        String paymentSystem = "visa";
        var rrn = "test_rrn";
        var authCode = "test_authCode";
        var paymentMethodId = "test_paymentMethodId";
        var currency = "test_currency";
        var features = Set.of(PaymentFeature.YA_BANK, PaymentFeature.UNKNOWN);
        var creationDate = Instant.now();

        OrderPaidData paidData1 = new OrderPaidData(
                null, orderId, multiOrderId, orderStatus, paymentSystem,
                rrn, authCode, paymentMethodId, currency, features, creationDate
        );
        OrderPaidData upsert1 = orderPaidDataDao.upsert(paidData1, true);

        OrderPaidData paidData2 = new OrderPaidData(
                null, orderId, multiOrderId, CoreOrderStatus.CANCELLED, "mastercard",
            rrn, authCode, paymentMethodId, currency, features, creationDate
        );
        OrderPaidData upsert2 = orderPaidDataDao.upsert(paidData2, true);

        assertThat(upsert2, allOf(
                hasProperty("id", equalTo(upsert1.getId())),
                hasProperty("orderId", equalTo(upsert1.getOrderId())),
                hasProperty("multiOrderId", equalTo(upsert1.getMultiOrderId())),
                hasProperty("orderStatus", equalTo(CoreOrderStatus.CANCELLED)),
                hasProperty("paymentSystem", equalTo("mastercard")),
                hasProperty("rrn", equalTo(upsert1.getRrn())),
                hasProperty("authCode", equalTo(upsert1.getAuthCode())),
                hasProperty("paymentMethodId", equalTo(upsert1.getPaymentMethodId())),
                hasProperty("currency", equalTo(upsert1.getCurrency())),
                hasProperty("paymentFeatures", equalTo(upsert1.getPaymentFeatures())),
                hasProperty("creationDate", equalTo(upsert1.getCreationDate()))
        ));
    }
}

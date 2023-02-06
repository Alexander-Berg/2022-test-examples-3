package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundSubstatus;

public class RefundJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        Refund refund = EntityHelper.getRefund();

        String json = write(refund);
        System.out.println(json);

        checkJson(json, Names.Refund.ID, 123);
        checkJson(json, Names.Refund.ORDER_ID, 456);
        checkJson(json, Names.Refund.PAYMENT_ID, 789);
        checkJson(json, Names.Refund.TRUST_REFUND_ID, "trustRefundId");
        checkJson(json, Names.Refund.HAS_RECEIPT, true);
        checkJson(json, Names.Refund.CURRENCY, Currency.RUR.name());
        checkJson(json, Names.Refund.AMOUNT, 12.34);
        checkJson(json, Names.Refund.ORDER_REMAINDER, 56.78);
        checkJson(json, Names.Refund.COMMENT, "comment");
        checkJson(json, Names.Refund.STATUS, RefundStatus.SUCCESS.name());
        checkJson(json, Names.Refund.SUBSTATUS, RefundSubstatus.REFUND_FAILED.name());
        checkJson(json, Names.Refund.CREATED_BY, 987);
        checkJson(json, Names.Refund.CREATED_BY_ROLE, ClientRole.SHOP.name());
        checkJson(json, Names.Refund.SHOP_MANAGER_ID, 654);
        checkJson(json, Names.Refund.CREATED_AT, "21-12-5490 08:31:51");
        checkJson(json, Names.Refund.UPDATED_AT, "11-12-9011 14:03:42");
        checkJson(json, Names.Refund.STATUS_UPDATED_AT, "17-02-107599 00:55:33");
        checkJson(json, Names.Refund.STATUS_EXPIRY_AT, "02-11-142808 08:14:04");
        checkJson(json, Names.Refund.REASON, RefundReason.ORDER_CHANGED.name());
        checkJson(json, Names.Refund.FAKE, true);
    }

    @Test
    public void deserialize() throws Exception {
        String json = "{" +
                "\"id\":123," +
                "\"orderId\":456," +
                "\"paymentId\":789," +
                "\"trustRefundId\":\"trustRefundId\"," +
                "\"hasReceipt\":true," +
                "\"currency\":\"RUR\"," +
                "\"amount\":12.34," +
                "\"orderRemainder\":56.78," +
                "\"comment\":\"comment\"," +
                "\"status\":\"SUCCESS\"," +
                "\"substatus\":\"REFUND_FAILED\"," +
                "\"createdBy\":987," +
                "\"createdByRole\":\"SHOP\"," +
                "\"shopManagerId\":654," +
                "\"creationDate\":\"21-12-5490 08:31:51\"," +
                "\"updateDate\":\"11-12-9011 14:03:42\"," +
                "\"statusUpdateDate\":\"17-02-107599 00:55:33\"," +
                "\"statusExpiryDate\":\"02-11-142808 08:14:04\"," +
                "\"reason\":\"ORDER_CHANGED\"," +
                "\"fake\":true" +
                "}";

        Refund refund = read(Refund.class, json);

        Assertions.assertEquals(123L, refund.getId().longValue());
        Assertions.assertEquals(456L, refund.getOrderId().longValue());
        Assertions.assertEquals(789L, refund.getPaymentId().longValue());
        Assertions.assertEquals("trustRefundId", refund.getTrustRefundId());
        Assertions.assertEquals(true, refund.getHasReceipt());
        Assertions.assertEquals(new BigDecimal("12.34"), refund.getAmount());
        Assertions.assertEquals(new BigDecimal("56.78"), refund.getOrderRemainder());
        Assertions.assertEquals("comment", refund.getComment());
        Assertions.assertEquals(RefundStatus.SUCCESS, refund.getStatus());
        Assertions.assertEquals(RefundSubstatus.REFUND_FAILED, refund.getSubstatus());
        Assertions.assertEquals(987, refund.getCreatedBy().longValue());
        Assertions.assertEquals(ClientRole.SHOP, refund.getCreatedByRole());
        Assertions.assertEquals(654L, refund.getShopManagerId().longValue());
        Assertions.assertEquals(new Date(111111111111000L), refund.getCreationDate());
        Assertions.assertEquals(new Date(222222222222000L), refund.getUpdateDate());
        Assertions.assertEquals(new Date(3333333333333000L), refund.getStatusUpdateDate());
        Assertions.assertEquals(new Date(4444444444444000L), refund.getStatusExpiryDate());
        Assertions.assertEquals(RefundReason.ORDER_CHANGED, refund.getReason());
        Assertions.assertEquals(true, refund.isFake());
    }

    @Test
    public void deserializeUnknown() throws IOException {
        String json = "{" +
                "\"status\":\"ASDASD\"," +
                "\"substatus\":\"ASDASD\"," +
                "\"createdByRole\":\"ASDASD\"," +
                "\"reason\":\"ASDASD\"" +
                "}";

        Refund refund = read(Refund.class, json);

        Assertions.assertEquals(RefundStatus.UNKNOWN, refund.getStatus());
        Assertions.assertEquals(RefundSubstatus.UNKNOWN, refund.getSubstatus());
        Assertions.assertEquals(ClientRole.UNKNOWN, refund.getCreatedByRole());
        Assertions.assertEquals(RefundReason.UNKNOWN, refund.getReason());
    }
}

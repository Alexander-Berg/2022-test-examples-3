package ru.yandex.market.checkout.checkouter.json;

import java.text.SimpleDateFormat;

import com.google.common.collect.Maps;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentURLActionType;

public class PaymentJsonHandlerTest extends AbstractJsonHandlerTestBase {

    public static final long ID = 123L;
    public static final String BASKET_ID = "basketId";
    public static final boolean FAKE = true;
    public static final PaymentStatus STATUS = PaymentStatus.HOLD;
    public static final PaymentSubstatus SUBSTATUS = PaymentSubstatus.HOLD_FAILED;
    public static final String FAIL_REASON = "failReason";
    public static final long ORDER_ID = 456L;
    public static final long UID = 789L;

    public static final String JSON = "{" +
            "\"id\":123," +
            "\"orderId\":456," +
            "\"basketId\":\"basketId\"," +
            "\"fake\":true," +
            "\"status\":\"HOLD\"," +
            "\"substatus\":\"HOLD_FAILED\"," +
            "\"failReason\":\"failReason\"," +
            "\"uid\":789," +
            "\"currency\":\"USD\"," +
            "\"totalAmount\":3.45," +
            "\"creationDate\":\"11-11-2017 15:00:00\"," +
            "\"updateDate\":\"15-11-2017 18:00:00\"," +
            "\"statusUpdateDate\":\"13-11-2017 22:00:00\"," +
            "\"statusExpiryDate\":\"16-11-2017 00:00:00\"," +
            "\"paymentForm\":{\"token\":\"token\"}," +
            "\"prepayType\":\"YANDEX_MARKET\"," +
            "\"failDescription\":\"failDescription\"," +
            "\"balancePayMethodType\":\"BALANCE_PAY_METHOD_TYPE\"," +
            "\"maskedCardNumber\":\"12345678****2256\"," +
            "\"paymentURLActionType\": \"I_FRAME\"" +
            "}";

    @Test
    public void serialize() throws Exception {
        Payment payment = EntityHelper.getPayment();
        payment.setMaskedCardNumber(payment.getCardNumber());

        String json = write(payment);
        System.out.println(json);

        checkJson(json, "$." + Names.ID, (int) ID);
        checkJson(json, "$." + Names.Payment.ORDER_ID, (int) ORDER_ID);
        checkJson(json, "$." + Names.Payment.BASKET_ID, BASKET_ID);
        checkJson(json, "$." + Names.Payment.FAKE, FAKE);
        checkJson(json, "$." + Names.Payment.STATUS, STATUS.name());
        checkJson(json, "$." + Names.Payment.SUBSTATUS, SUBSTATUS.name());
        checkJson(json, "$." + Names.Payment.FAIL_REASON, FAIL_REASON);
        checkJson(json, "$." + Names.Payment.USER_ID, (int) UID);
        checkJson(json, "$." + Names.Payment.CURRENCY, EntityHelper.CURRENCY.name());
        checkJsonMatcher(json, "$." + Names.Payment.TOTAL_AMOUNT,
                Matchers.closeTo(EntityHelper.TOTAL_AMOUNT.doubleValue(), 0.0001));
        checkJson(json, "$." + Names.Payment.CREATED_AT,
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(EntityHelper.CREATION_DATE));
        checkJson(json, "$." + Names.Payment.UPDATED_AT,
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(EntityHelper.UPDATE_DATE));
        checkJson(json, "$." + Names.Payment.STATUS_UPDATED_AT,
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(EntityHelper.STATUS_UPDATE_DATE));
        checkJson(json, "$." + Names.Payment.STATUS_EXPIRY_AT,
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(EntityHelper.STATUS_EXPIRY_DATE));
        checkJson(json, "$." + Names.Payment.PAYMENT_FORM,
                Maps.newLinkedHashMap(EntityHelper.PAYMENT_FORM.getParameters()));
        checkJson(json, "$." + Names.Payment.PREPAY_TYPE, EntityHelper.PREPAY_TYPE.name());
        checkJson(json, "$." + Names.Payment.FAIL_DESCRIPTION, EntityHelper.FAIL_DESCRIPTION);
        checkJson(json, "$." + Names.Payment.BALANCE_PAY_METHOD_TYPE, EntityHelper.BALANCE_PAY_METHOD_TYPE);
        checkJson(json, "$." + Names.Payment.MASKED_CARD_NUMBER, EntityHelper.CARD_NUMBER);
        checkJson(json, "$." + Names.Payment.PAYMENT_URL_ACTION_TYPE, PaymentURLActionType.I_FRAME.name());
    }

    @Test
    public void deserialize() throws Exception {
        Payment payment = read(Payment.class, JSON);

        Assertions.assertEquals(ID, payment.getId().longValue());
        Assertions.assertEquals(ORDER_ID, payment.getOrderId().longValue());
        Assertions.assertEquals(BASKET_ID, payment.getBasketId());
        Assertions.assertEquals(FAKE, payment.isFake());
        Assertions.assertEquals(STATUS, payment.getStatus());
        Assertions.assertEquals(SUBSTATUS, payment.getSubstatus());
        Assertions.assertEquals(FAIL_REASON, payment.getFailReason());
        Assertions.assertEquals(UID, payment.getUid().longValue());
        Assertions.assertEquals(EntityHelper.CURRENCY, payment.getCurrency());
        Assertions.assertEquals(EntityHelper.TOTAL_AMOUNT, payment.getTotalAmount());
        Assertions.assertEquals(EntityHelper.CREATION_DATE, payment.getCreationDate());
        Assertions.assertEquals(EntityHelper.UPDATE_DATE, payment.getUpdateDate());
        Assertions.assertEquals(EntityHelper.STATUS_UPDATE_DATE, payment.getStatusUpdateDate());
        Assertions.assertEquals(EntityHelper.STATUS_EXPIRY_DATE, payment.getStatusExpiryDate());
        Assertions.assertEquals(EntityHelper.PAYMENT_FORM.getParameters(), payment.getPaymentForm().getParameters());
        Assertions.assertEquals(EntityHelper.PREPAY_TYPE, payment.getPrepayType());
        Assertions.assertEquals(EntityHelper.FAIL_DESCRIPTION, payment.getFailDescription());
        Assertions.assertEquals(EntityHelper.BALANCE_PAY_METHOD_TYPE, payment.getBalancePayMethodType());
        Assertions.assertEquals(PaymentURLActionType.I_FRAME, payment.getPaymentURLActionType());
        Assertions.assertNull(payment.getCardNumber());
        Assertions.assertEquals(EntityHelper.CARD_NUMBER, payment.getMaskedCardNumber());
    }

}

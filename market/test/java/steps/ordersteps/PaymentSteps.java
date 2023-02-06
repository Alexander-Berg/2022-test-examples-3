package steps.orderSteps;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

import steps.utils.DateUtils;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentForm;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubstatus;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;

class PaymentSteps {
    private static final Date DATE = DateUtils.getDate();

    private static final Long ID = 123L;
    private static final Long ORDER_ID = 999L;
    private static final String BASKET_ID = "888";
    private static final PaymentStatus STATUS = PaymentStatus.IN_PROGRESS;
    private static final PaymentSubstatus SUBSTATUS = PaymentSubstatus.PROGRESS_EXPIRED;
    private static final Long UID = 345L;
    private static final Currency CURRENCY = Currency.AED;
    private static final BigDecimal TOTAL_AMOUNT = BigDecimal.valueOf(5678);
    private static final Date CREATION_DATE = DATE;
    private static final Date UPDATE_DATE = DATE;
    private static final Date STATUS_UPDATE_DATE = DATE;
    private static final Date STATUS_EXPIRY_DATE = DATE;
    private static final Boolean FAKE = false;
    private static final String FAIL_REASON = "failReason";
    private static final PaymentGoal TYPE = PaymentGoal.ORDER_PREPAY;
    private static final PaymentForm PAYMENT_FORM = new PaymentForm(new HashMap<>());
    private static final PrepayType PREPAY_TYPE = PrepayType.YANDEX_MARKET;

    private PaymentSteps() {
    }

    static Payment getPayment() {
        Payment payment = new Payment();

        payment.setId(ID);
        payment.setOrderId(ORDER_ID);
        payment.setStatus(STATUS);
        payment.setSubstatus(SUBSTATUS);
        payment.setUid(UID);
        payment.setCurrency(CURRENCY);
        payment.setTotalAmount(TOTAL_AMOUNT);
        payment.setCreationDate(CREATION_DATE);
        payment.setUpdateDate(UPDATE_DATE);
        payment.setStatusUpdateDate(STATUS_UPDATE_DATE);
        payment.setStatusExpiryDate(STATUS_EXPIRY_DATE);
        payment.setFake(FAKE);
        payment.setFailReason(FAIL_REASON);
        payment.setType(TYPE);
        payment.setPaymentForm(PAYMENT_FORM);
        payment.setPrepayType(PREPAY_TYPE);

        return payment;
    }
}

package ru.yandex.market.pvz.core.test.factory;

import javax.transaction.Transactional;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.sms.SmsLogCommandService;
import ru.yandex.market.pvz.core.domain.sms.SmsLogParams;

import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_PHONE;

@Transactional
public class TestSmsLogFactory {

    @Autowired
    private SmsLogCommandService smsLogCommandService;

    @Autowired
    private TestOrderFactory orderFactory;

    public SmsLogParams create(long orderId, SmsLogTestParams smsLogTestParams) {
        return smsLogCommandService.createVerificationCodeSms(smsLogTestParams.getPhone(), orderId);
    }

    public SmsLogParams create(long orderId) {
        return create(orderId, SmsLogTestParams.builder().build());
    }

    public SmsLogParams create(SmsLogTestParams smsLogTestParams) {
        var order = orderFactory.createOrder();
        orderFactory.receiveOrder(order.getId());
        return create(order.getId(), smsLogTestParams);
    }

    public SmsLogParams create() {
        return create(SmsLogTestParams.builder().build());
    }

    @Data
    @Builder
    public static class SmsLogTestParams {

        public static final String DEFAULT_SMS_PHONE = DEFAULT_RECIPIENT_PHONE;

        @Builder.Default
        private String phone = DEFAULT_SMS_PHONE;
    }
}

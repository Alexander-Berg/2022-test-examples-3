package ru.yandex.market.pvz.core.domain.sms;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SmsLogCommandServiceTest {

    private static final String PHONE = "+78005553535";

    private final TestOrderFactory orderFactory;

    private final OrderQueryService orderQueryService;
    private final SmsLogCommandService smsLogCommandService;
    private final SmsLogQueryService smsLogQueryService;

    @Test
    void createVerificationCodeSms() {
        var order = orderFactory.createOrder();

        var sms = smsLogCommandService.createVerificationCodeSms(PHONE, order.getId());
        var actual = smsLogQueryService.get(sms.getId());

        var expected = SmsLogParams.builder()
                .id(sms.getId())
                .type(SmsType.VERIFICATION_CODE)
                .phone(PHONE)
                .orderId(order.getId())
                .build();

        assertThat(actual).isEqualToIgnoringGivenFields(expected, "updatedAt");

        var additionalInfo = orderQueryService.getOrderAdditionalParams(order.getId());
        assertThat(additionalInfo.getLastSendVerificationCodeSms()).isNotNull();
    }

    @Test
    void tryToCreateVerificationCodeSmsWithExceededLimit() {
        var order = orderFactory.createOrder();

        smsLogCommandService.createVerificationCodeSms(PHONE, order.getId());
        var additionalInfo = orderQueryService.getOrderAdditionalParams(order.getId());
        Instant firstLastSendTime = additionalInfo.getLastSendVerificationCodeSms();
        assertThat(firstLastSendTime).isNotNull();

        smsLogCommandService.createVerificationCodeSms(PHONE, order.getId());
        additionalInfo = orderQueryService.getOrderAdditionalParams(order.getId());
        Instant secondLastSendTime = additionalInfo.getLastSendVerificationCodeSms();
        assertThat(secondLastSendTime).isAfter(firstLastSendTime);

        smsLogCommandService.createVerificationCodeSms(PHONE, order.getId());
        additionalInfo = orderQueryService.getOrderAdditionalParams(order.getId());
        Instant thirdLastSendTime = additionalInfo.getLastSendVerificationCodeSms();
        assertThat(thirdLastSendTime).isAfter(secondLastSendTime);

        assertThatThrownBy(() -> smsLogCommandService.createVerificationCodeSms(PHONE, order.getId()))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

}

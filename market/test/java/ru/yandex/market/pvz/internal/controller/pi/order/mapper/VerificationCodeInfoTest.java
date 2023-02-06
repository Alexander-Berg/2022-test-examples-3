package ru.yandex.market.pvz.internal.controller.pi.order.mapper;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.core.domain.order.model.OrderType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_VERIFICATION_CODE;

class VerificationCodeInfoTest {

    @Test
    void useVerificationCodeForClientOrder() {
        var verificationCodeInfo = new VerificationCodeInfo(DEFAULT_VERIFICATION_CODE, 3, true, false);
        assertThat(verificationCodeInfo.useVerificationCode(OrderType.CLIENT)).isTrue();
    }

    @Test
    void useVerificationCodeForOnDemandOrder() {
        var verificationCodeInfo = new VerificationCodeInfo(DEFAULT_VERIFICATION_CODE, 3, false, true);
        assertThat(verificationCodeInfo.useVerificationCode(OrderType.ON_DEMAND)).isTrue();
    }

    @Test
    void doNotUseVerificationCodeForClientOrder() {
        var verificationCodeInfo = new VerificationCodeInfo(DEFAULT_VERIFICATION_CODE, 3, false, true);
        assertThat(verificationCodeInfo.useVerificationCode(OrderType.CLIENT)).isFalse();
    }

    @Test
    void doNotUseVerificationCodeForOnDemandOrder() {
        var verificationCodeInfo = new VerificationCodeInfo(DEFAULT_VERIFICATION_CODE, 3, true, false);
        assertThat(verificationCodeInfo.useVerificationCode(OrderType.ON_DEMAND)).isFalse();
    }

    @Test
    void doNotUseVerificationCodeForClientOrderWithoutCode() {
        var verificationCodeInfo = new VerificationCodeInfo(null, 3, true, false);
        assertThat(verificationCodeInfo.useVerificationCode(OrderType.CLIENT)).isFalse();
    }

    @Test
    void doNotUseVerificationCodeForOnDemandOrderWithoutCode() {
        var verificationCodeInfo = new VerificationCodeInfo(null, 3, false, true);
        assertThat(verificationCodeInfo.useVerificationCode(OrderType.ON_DEMAND)).isFalse();
    }

}

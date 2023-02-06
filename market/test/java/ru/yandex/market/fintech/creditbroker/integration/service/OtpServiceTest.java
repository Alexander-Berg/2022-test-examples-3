package ru.yandex.market.fintech.creditbroker.integration.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fintech.creditbroker.helper.CreditApplicationHelper;
import ru.yandex.market.fintech.creditbroker.integration.wiremock.PersonalMockConfigurer;
import ru.yandex.market.fintech.creditbroker.model.CreditApplication;
import ru.yandex.market.fintech.creditbroker.service.otp.OtpService;

public class OtpServiceTest
//        extends AbstractFunctionalTest
{
    @Autowired
    private PersonalMockConfigurer personalMockConfigurer;
    @Autowired
    private CreditApplicationHelper creditApplicationHelper;
    @Autowired
    private OtpService otpService;

    @Test
    @Disabled
    public void submitTest() {
        personalMockConfigurer.mockV1PhonesRetrieve();
        CreditApplication creditApplication = creditApplicationHelper.create();
        otpService.submit(creditApplication.getId(), "10.10.10.10", "FF", null);
    }
}

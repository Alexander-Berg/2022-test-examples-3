package ru.yandex.market.tpl.core.external.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.common.sms.SmsClient;
import ru.yandex.market.tpl.common.sms.SmsResult;
import ru.yandex.market.tpl.common.sms.SmsResultCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(SpringExtension.class)
class WhitelistSmsClientDecoratorTest {

    private static final String PHONE = "+79000000000";
    private static final String MESSAGE = "Hello";

    @MockBean
    private SmsClient delegate;
    @MockBean
    private SmsWhitelistPhoneFilter smsWhitelistPhoneFilter;

    private SmsClient whitelistedSmsClientUnderTest;

    @BeforeEach
    void init() {
        whitelistedSmsClientUnderTest = new WhitelistSmsClientDecorator(delegate, smsWhitelistPhoneFilter);
    }

    @Test
    void whenWhitelistPassesThenSmsIsDelegated() {
        given(smsWhitelistPhoneFilter.passes(PHONE))
                .willReturn(true);
        //when
        whitelistedSmsClientUnderTest.send(PHONE, MESSAGE);
        //then
        then(delegate).should().send(PHONE, MESSAGE);
    }

    @Test
    void whenWhitelistDoesntPassThenWhitelistFilteredIsReturned() {
        given(smsWhitelistPhoneFilter.passes(PHONE))
                .willReturn(false);
        //when
        SmsResult smsResult = whitelistedSmsClientUnderTest.send(PHONE, MESSAGE);
        //then
        then(delegate).shouldHaveNoInteractions();
        assertThat(smsResult.getCode()).isEqualTo(SmsResultCode.WHITELIST_FILTERED);
    }

}

package ru.yandex.market.hrms.core.service.isrping;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.ispring.ISpringSmsService;
import ru.yandex.market.hrms.core.service.sms.YaSmsService;
import ru.yandex.market.ispring.ISpringClient;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class ISpringSmsServiceTest extends AbstractCoreTest {

    @MockBean
    private ISpringClient iSpringClient;

    @MockBean
    private YaSmsService yaSmsService;

    @Autowired
    private ISpringSmsService sut;

    private static final String DEFAULT_ISPRING_ID = "abra-cadabra-id";
    private static final String DEFAULT_PHONE_NUMBER = "+78005555555";

    @Test
    public void should_ChangePasswordAndSendSms_When_DataCorrect() {
        String correctPhone = "+79121342341";

        sut.changePasswordAndSendSms(DEFAULT_ISPRING_ID, correctPhone);
    }

    @Test
    public void should_ThrowException_When_SmsFailSent() throws Exception {
        String phone = "+79121236574";
        mockSendSmsFail(phone);

        Assertions.assertThatThrownBy(() -> sut.changePasswordAndSendSms(DEFAULT_ISPRING_ID, phone))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void should_ThrowException_When_IspringClientThrowError() {
        String ispringIdNotExisted = "ispring-id-not-found";
        mockIspringChangePasswordFail(ispringIdNotExisted);

        Assertions.assertThatThrownBy(() -> sut.changePasswordAndSendSms(ispringIdNotExisted, DEFAULT_PHONE_NUMBER))
                .isInstanceOf(TplIllegalArgumentException.class);
    }

    @Test
    public void should_ThrowException_When_PhoneFormatNotCorrectly() {
        String incorrectPhoneNumber = "NOT-NUMBER-PHONE";

        Assertions.assertThatThrownBy(() -> sut.changePasswordAndSendSms(DEFAULT_ISPRING_ID, incorrectPhoneNumber))
                .isInstanceOf(TplIllegalArgumentException.class);
    }

    @Test
    public void should_ThrowException_When_PhoneIsNull() {
        Assertions.assertThatThrownBy(() -> sut.changePasswordAndSendSms(DEFAULT_ISPRING_ID, null))
                .isInstanceOf(TplIllegalArgumentException.class);
    }

    @Test
    @DbUnitDataSet(before = "ISpringSmsServiceTest.before.csv")
    public void should_ThrowException_When_ThrottleService() throws Exception {
        mockClock(LocalDateTime.parse("2022-01-10T00:00:15"));

        Assertions.assertThatThrownBy(() -> sut.changePasswordAndSendSms(DEFAULT_ISPRING_ID, DEFAULT_PHONE_NUMBER))
                .isInstanceOf(TplIllegalArgumentException.class)
                .hasMessageContaining("Слишком много запросов");
    }

    private void mockSendSmsFail(String phone) throws Exception {
        when(yaSmsService.sendSms(eq(phone), anyString()))
                .thenThrow(new RuntimeException("Ошибка отправки SMS"));
    }

    private void mockIspringChangePasswordFail(String ispringId) {
        doThrow(new RuntimeException("Ошибка смены пароля в iSpring"))
                .when(iSpringClient).changePassword(eq(ispringId), anyString());
    }
}

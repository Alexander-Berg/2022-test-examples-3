package ru.yandex.direct.web.entity.smsauth;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.entity.smsauth.SmsAuthUtils.SMS_PASSWORD_LENGTH;

class SmsAuthUtilsTest {

    @Test
    void generateRandomSmsPasswordTest() {
        for (int iter = 0; iter < 3000000; iter++) {
            String password = SmsAuthUtils.generateRandomSmsPassword();

            assertThat(password.length(), is(SMS_PASSWORD_LENGTH));
            assertThat(Integer.parseInt(password), greaterThanOrEqualTo(0));
        }
    }
}

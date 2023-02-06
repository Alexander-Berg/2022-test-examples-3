package ru.yandex.direct.captcha.passport;

import com.google.common.collect.ImmutableMap;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.captcha.passport.entity.check.response.CaptchaCheckResponse;
import ru.yandex.direct.captcha.passport.entity.check.response.CheckStatus;
import ru.yandex.direct.captcha.passport.entity.check.response.ErrorDescription;
import ru.yandex.direct.captcha.passport.entity.generate.response.CaptchaGenerateResponse;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class YandexCaptchaClientTest {
    public static final String KNOWN_CAPTCHA_KEY = "20ehmZOtr1NNmuKn7gEeQ6ZIeQ7sc85C";
    public static final String KNOWN_CAPTCHA_VALUE = "236888";
    @Rule
    public final MockedCaptchaService mockedCaptchaService = new MockedCaptchaService(
            ImmutableMap.of(KNOWN_CAPTCHA_KEY, KNOWN_CAPTCHA_VALUE));

    private YandexCaptchaClient captchaClient;

    @Before
    public void setup() {
        YandexCaptchaConfig config = new YandexCaptchaConfig(
                mockedCaptchaService.getBaseUrl(),
                "api.captcha.yandex.net");
        captchaClient = new YandexCaptchaClient(config, new DefaultAsyncHttpClient());
    }

    @Test
    public void generate() throws Exception {
        CaptchaGenerateResponse response = captchaClient.generate(3);
        assertThat(response, notNullValue());
        assertThat(response.getRequestId(), notNullValue());
        assertThat(response.getUrl(), notNullValue());
    }

    @Test
    public void check() throws Exception {
        CaptchaCheckResponse response = captchaClient.check(KNOWN_CAPTCHA_KEY, KNOWN_CAPTCHA_VALUE);
        assertThat(response.getCheckStatus(), Matchers.is(CheckStatus.OK));
    }

    @Test
    public void checkFailsForUnknownCaptchaKey() throws Exception {
        CaptchaCheckResponse response = captchaClient.check("UnknownKey", KNOWN_CAPTCHA_VALUE);
        assertThat(response.getCheckStatus(), is(CheckStatus.FAILED));
        assertThat(response.getErrorType(), is(ErrorDescription.NOT_FOUND));
    }

    @Test
    public void checkFailsForWrongCaptchaRep() throws Exception {
        CaptchaCheckResponse response = captchaClient.check(KNOWN_CAPTCHA_KEY, "-4242");
        assertThat(response.getCheckStatus(), is(CheckStatus.FAILED));
    }
}

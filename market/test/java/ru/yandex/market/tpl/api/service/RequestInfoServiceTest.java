package ru.yandex.market.tpl.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RequestInfoService.class})
public class RequestInfoServiceTest {

    @Autowired
    private RequestInfoService service;

    @Test
    void testIpFromXRealIp() {
        // given
        var headers = new HttpHeaders();
        headers.add("x-real-ip", "123");
        // when
        var result = service.extractRequestClientInfo(headers);
        // then
        assertThat(result.getClientIp()).isEqualTo("123");
    }

    @Test
    void testIpFromMultipleXForwardedFor() {
        // given
        var headers = new HttpHeaders();
        headers.add("x-forwarded-for", "123,456,789");
        // when
        var result = service.extractRequestClientInfo(headers);
        // then
        assertThat(result.getClientIp()).isEqualTo("123");
    }

    @Test
    void testIpFromXForwardedFor() {
        // given
        var headers = new HttpHeaders();
        headers.add("x-forwarded-for", "123");
        // when
        var result = service.extractRequestClientInfo(headers);
        // then
        assertThat(result.getClientIp()).isEqualTo("123");
    }

    @Test
    void testUserAgent() {
        // given
        var headers = new HttpHeaders();
        headers.add("user-agent", "Yandex Browser");
        // when
        var result = service.extractRequestClientInfo(headers);
        // then
        assertThat(result.getClientUserAgent()).isEqualTo("Yandex Browser");
    }

    @Test
    void testClientOriginalScheme() {
        // given
        var headers = new HttpHeaders();
        headers.add("x-real-scheme", "scheme");
        // when
        var result = service.extractRequestClientInfo(headers);
        // then
        assertThat(result.getClientOriginalScheme()).isEqualTo("scheme");
    }

}

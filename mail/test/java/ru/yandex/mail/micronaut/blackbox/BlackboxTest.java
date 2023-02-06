package ru.yandex.mail.micronaut.blackbox;

import javax.inject.Inject;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MicronautTest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.mail.tvmlocal.junit_jupiter.WithLocalTvm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithLocalTvm(TestTvmToolOptionsProvider.class)
@Slf4j
@MicronautTest
public class BlackboxTest {
    @Inject
    @Client("/")
    HttpClient client;

    @Test
    @DisplayName("Check that the ping returns OK without authentication")
    public void pingTest() {
        assertThat(client.toBlocking().retrieve("/ping"))
                .isEqualTo("pong");
    }

    @Test
    @DisplayName("Check that the ping returns OK with basic authentication")
    public void authTest() {
        val request = HttpRequest.GET("/auth")
                .basicAuth("caldavos-blackbox", "8vPcXHs")
                .header(HttpHeaders.X_REAL_IP, "172.16.10.1");
        assertThat(client.toBlocking().retrieve(request))
                .isEqualTo("auth BlackboxUid(value=4030896882, hosted=false, domid=OptionalLong.empty, domain=Optional.empty, mx=Optional.empty)");
    }

    @Test
    @DisplayName("Check that the ping returns 401 for invalid basic auth")
    public void unauthorizedTest() {
        val request = HttpRequest.GET("/auth").header(io.micronaut.http.HttpHeaders.AUTHORIZATION, "Basic SmF2YWNvZGVnZWVrcw==");
        assertThatThrownBy(() -> client.toBlocking().retrieve(request))
                .isInstanceOf(HttpClientResponseException.class)
                .hasMessage("Unauthorized");
    }
}

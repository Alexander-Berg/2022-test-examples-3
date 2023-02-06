package ru.yandex.calendar.frontend.caldav.impl;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginShortenerTest {
    @Test
    public void test() {
        assertThat(LoginShortener.shortenLoginForLog("stepancheg@yandex-team.ru")).isEqualTo("stepancheg@ytr");
        assertThat(LoginShortener.shortenLoginForLog("levin-matveev@yandex.ru")).isEqualTo("levin-matveev@yr");
        assertThat(LoginShortener.shortenLoginForLog("rrr@fgfg.ru")).isEqualTo("rrr@fgfg.ru");
    }
}

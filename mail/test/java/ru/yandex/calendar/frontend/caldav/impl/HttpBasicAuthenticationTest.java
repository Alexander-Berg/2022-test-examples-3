package ru.yandex.calendar.frontend.caldav.impl;

import lombok.val;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpBasicAuthenticationTest {
    @Test
    public void parseBasic() {
        val value = "Basic aGVsbG86d29ybGQ=";
        val loginPassword = HttpBasicAuthentication.parseBasic(value);
        assertThat(loginPassword._1).isEqualTo("hello");
        assertThat(loginPassword._2).isEqualTo("world");
    }
}

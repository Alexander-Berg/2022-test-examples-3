package ru.yandex.direct.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HostPortTest {

    @Test
    public void fromString_parseIp() {
        assertThat(HostPort.fromString("10.10.10.10:12345"))
                .isEqualToComparingFieldByField(new HostPort("10.10.10.10", 12345));
    }

    @Test
    public void fromString_parseHostname() {
        assertThat(HostPort.fromString("yandex.ru:12345"))
                .isEqualToComparingFieldByField(new HostPort("yandex.ru", 12345));
    }
}

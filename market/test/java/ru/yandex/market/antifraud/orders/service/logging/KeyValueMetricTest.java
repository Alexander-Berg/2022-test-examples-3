package ru.yandex.market.antifraud.orders.service.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class KeyValueMetricTest {

    @Test
    public void serializationTest() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss Z")
                .withZone(ZoneId.of("Europe/Moscow"))
                .withLocale(Locale.ENGLISH);
        Instant time = Instant.ofEpochSecond(1700000000);
        KeyValueMetric kvMetric = KeyValueMetric.builder()
                .datetime(formatter.format(time))
                .environment("PRESTABLE")
                .dc("sas")
                .host("sas-1")
                .key("key")
                .subkey("subkey")
                .value(2.1d)
                .build();
        assertThat(AntifraudJsonUtil.toJson(kvMetric)).isEqualTo(
                "{\"datetime\":\"2023-11-15T01:13:20 +0300\",\"dc\":\"sas\",\"host\":\"sas-1\",\"environment\":\"PRESTABLE\"," +
                        "\"key\":\"key\",\"subkey\":\"subkey\",\"value\":2.1}"
        );
    }

}

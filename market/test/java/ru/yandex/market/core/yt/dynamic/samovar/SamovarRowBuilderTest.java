package ru.yandex.market.core.yt.dynamic.samovar;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Date: 29.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class SamovarRowBuilderTest {

    @DisplayName("Проверка на то, если время выключения не задано, оно не добавится в map")
    @Test
    void build_disabledTimestampNull_mapNotContainsTimestamp() {
        Map<String, ?> map = new SamovarRowBuilder()
                .setAuth(null)
                .setEnabled(true)
                .setDisabledTimestamp(null)
                .setUrl("http://test.scheme.ru/")
                .setPeriodMinutes(20)
                .setTimeoutSeconds(200)
                .setContext(new byte[30])
                .build();

        Assertions.assertFalse(map.containsKey(SamovarYtSchemaUtils.DISABLED_TIMESTAMP_COLUMN));
    }

    @DisplayName("Проверка на то, если время выключения задано, оно добавится в map")
    @Test
    void build_disabledTimestamp_mapContainsTimestamp() {
        Instant now = Instant.now();
        Map<String, ?> map = new SamovarRowBuilder()
                .setAuth(null)
                .setEnabled(true)
                .setDisabledTimestamp(now)
                .setUrl("http://test.scheme.ru/")
                .setPeriodMinutes(20)
                .setTimeoutSeconds(200)
                .setContext(new byte[30])
                .build();

        Assertions.assertEquals(SamovarYtSchemaUtils.DISABLED_TIMESTAMP_FORMATTER.format(now),
                map.get(SamovarYtSchemaUtils.DISABLED_TIMESTAMP_COLUMN));
    }
}

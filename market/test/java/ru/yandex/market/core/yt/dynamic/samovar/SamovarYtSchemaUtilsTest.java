package ru.yandex.market.core.yt.dynamic.samovar;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.core.yt.dynamic.samovar.SamovarYtSchemaUtils.DISABLED_TIMESTAMP_FORMATTER;

/**
 * Date: 29.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class SamovarYtSchemaUtilsTest {

    @DisplayName("Проверка на то, что при наличии времени выключения в справочнике, оно корректно распарсится")
    @Test
    void parseDisabledTimestamp_disableTimestampExist_correctResult() {
        Instant res = SamovarYtSchemaUtils.parseDisabledTimestamp(Map.of(
                SamovarYtSchemaUtils.DISABLED_TIMESTAMP_COLUMN, "2020-10-09 14:33:23"
        ));
        Assertions.assertEquals(DISABLED_TIMESTAMP_FORMATTER.parse("2020-10-09 14:33:23", Instant::from),
                res);
    }

    @DisplayName("Проверка на то, что при отсутствии времени выключения в справочнике, вернется null")
    @Test
    void parseDisabledTimestamp_nullDisableTimestampExist_nullResult() {
        Instant res = SamovarYtSchemaUtils.parseDisabledTimestamp(Collections.emptyMap());
        Assertions.assertNull(res);
    }
}

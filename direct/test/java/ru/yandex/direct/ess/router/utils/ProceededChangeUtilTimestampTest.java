package ru.yandex.direct.ess.router.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.binlog.model.Operation;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.ess.router.utils.ProceededChangeUtil.getProceededChangeTimestamp;

public class ProceededChangeUtilTimestampTest {

    @Test
    public void localDateTimeConvertedToValidLong() {
        LocalDateTime expected = LocalDateTime.now().withNano(0);
        ProceededChange proceededChange = getProceededChange(expected);

        Long actualLong = getProceededChangeTimestamp(proceededChange);
        LocalDateTime actual = Instant.ofEpochMilli(actualLong).atZone(ZoneOffset.UTC).toLocalDateTime();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void nullConvertedToNull() {
        ProceededChange proceededChange = getProceededChange(null);

        Long actualLong = getProceededChangeTimestamp(proceededChange);
        assertThat(actualLong).isNull();
    }

    private ProceededChange getProceededChange(LocalDateTime eventTime) {
        return new ProceededChange.Builder()
                .setTableName("banners")
                .setOperation(Operation.UPDATE)
                .setPrimaryKeys(Map.of("bid", 123L))
                .setBefore(Map.of("statusModerate", "New"))
                .setAfter(Map.of("statusModerate", "Ready"))
                .setBinlogTimestamp(eventTime)
                .build();
    }
}

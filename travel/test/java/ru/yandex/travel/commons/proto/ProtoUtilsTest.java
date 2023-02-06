package ru.yandex.travel.commons.proto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.google.protobuf.Timestamp;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtoUtilsTest {

    @Test
    public void testLocalDateTimeToInstant() {
        LocalDateTime dt = LocalDateTime.of(2020, 10, 1, 20, 1, 5);
        Timestamp protoTimestamp = ProtoUtils.fromLocalDateTime(dt);
        Instant fromProto = ProtoUtils.toInstant(protoTimestamp);
        LocalDateTime utcDt = fromProto.atZone(ZoneId.of("UTC")).toLocalDateTime();
        assertThat(dt).isEqualTo(utcDt);
    }
}

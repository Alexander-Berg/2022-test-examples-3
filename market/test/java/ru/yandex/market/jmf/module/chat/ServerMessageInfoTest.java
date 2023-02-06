package ru.yandex.market.jmf.module.chat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.module.chat.controller.model.ServerMessageInfo;

public class ServerMessageInfoTest {
    @Test
    public void testMicrosecondsDeserialization() {
        var now = Instant.now();
        var epochMicros = ChronoUnit.MICROS.between(Instant.EPOCH, now);
        var serverMessageInfo = new ServerMessageInfo(epochMicros);

        Assertions.assertEquals(now.truncatedTo(ChronoUnit.MICROS), serverMessageInfo.getMessageCreationDate());
        Assertions.assertEquals(epochMicros, serverMessageInfo.getTimestamp());
    }

    @Test
    public void testMicrosecondsSerialization() {
        var now = Instant.now();
        var serverMessageInfo = new ServerMessageInfo(now);

        Assertions.assertEquals(now, serverMessageInfo.getMessageCreationDate());
        Assertions.assertEquals(ChronoUnit.MICROS.between(Instant.EPOCH, now), serverMessageInfo.getTimestamp());
    }
}

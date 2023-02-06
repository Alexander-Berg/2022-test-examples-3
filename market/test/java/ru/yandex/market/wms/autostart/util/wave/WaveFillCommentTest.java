package ru.yandex.market.wms.autostart.util.wave;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.Wave;
import ru.yandex.market.wms.common.spring.enums.WaveType;

public class WaveFillCommentTest extends BaseTest {

    @Test
    void fillWaveCommentForWithdrawal() {
        var expected = "Изъятие просрочки ПДО 11.12.2021";
        Wave wave = Wave.builder()
                .waveKey("12345")
                .realOrders(List.of(Order.builder()
                        .orderKey("1")
                        .type("16")
                        .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-11T10:15:30+01:00"))
                        .build()))
                .waveType(WaveType.WITHDRAWAL)
                .build();
        wave.fillWaveComment();
        String actual = wave.getComment();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void fillWaveCommentForMixedWithdrawalDifferentTypes() {
        var expected = "МИКС Изъятие ПДО 9.12.2021";
        Wave wave = Wave.builder()
                .waveKey("12345")
                .realOrders(List.of(
                        Order.builder()
                                .orderKey("1")
                                .type("16")
                                .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-11T10:15:30+01:00"))
                                .build(),
                        Order.builder()
                                .orderKey("2")
                                .type("15")
                                .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-09T10:15:30+01:00"))
                                .build(),
                        Order.builder()
                                .orderKey("3")
                                .type("15")
                                .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-09T10:15:30+01:00"))
                                .build(),
                        Order.builder()
                                .orderKey("4")
                                .type("14")
                                .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-10T10:15:30+01:00"))
                                .build()
                ))
                .waveType(WaveType.WITHDRAWAL)
                .build();
        wave.fillWaveComment();
        String actual = wave.getComment();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void fillWaveCommentForMixedWithdrawalSameTypes() {
        var expected = "МИКС Изъятие брака ПДО 11.12.2021";
        Wave wave = Wave.builder()
                .waveKey("12345")
                .realOrders(List.of(
                        Order.builder()
                                .orderKey("1")
                                .type("15")
                                .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-11T10:15:30+01:00"))
                                .build(),
                        Order.builder()
                                .orderKey("2")
                                .type("15")
                                .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-11T10:15:30+01:00"))
                                .build(),
                        Order.builder()
                                .orderKey("3")
                                .type("15")
                                .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-14T10:15:30+01:00"))
                                .build(),
                        Order.builder()
                                .orderKey("4")
                                .type("15")
                                .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-15T10:15:30+01:00"))
                                .build()
                ))
                .waveType(WaveType.WITHDRAWAL)
                .build();
        wave.fillWaveComment();
        String actual = wave.getComment();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void fillWaveCommentForBigWithdrawal() {
        var expected = "98765 Изъятие просрочки ПДО 11.12.2021";
        Wave wave = Wave.builder()
                .waveKey("12345")
                .realOrders(List.of(Order.builder()
                        .orderKey("98765")
                        .type("16")
                        .scheduledShipDateInDB(OffsetDateTime.parse("2021-12-11T10:15:30+01:00"))
                        .build()))
                .waveType(WaveType.BIG_WITHDRAWAL)
                .build();
        wave.fillWaveComment();
        String actual = wave.getComment();
        Assertions.assertEquals(expected, actual);
    }
}

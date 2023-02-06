package ru.yandex.market.logistics.lom.utils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.WaybillSegmentStatusHistory;
import ru.yandex.market.logistics.lom.entity.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;

@DisplayName("Unit-тесты для WaybillUtilsTest")
class WaybillUtilsTest extends AbstractTest {

    @Test
    @DisplayName("Найти первый чекпоинт на сегменте среди одинаковых")
    void getLastSegmentStatusOneSegment() {
        WaybillSegment firstSegment = new WaybillSegment();
        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            firstSegment,
            SegmentStatus.IN,
            Instant.parse("2022-02-08T03:00:00Z")
        );

        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            firstSegment,
            SegmentStatus.IN,
            Instant.parse("2022-02-10T03:00:00Z")
        );
        Order order = WaybillSegmentFactory.joinInOrder(List.of(firstSegment));

        WaybillSegmentStatusHistory foundSegmentStatus =
            WaybillUtils.getLastSegmentStatusHistoryWithStatus(order, Set.of(SegmentStatus.IN)).get();
        softly.assertThat(foundSegmentStatus.getDate()).isEqualTo(Instant.parse("2022-02-08T03:00:00Z"));
    }

    @Test
    @DisplayName("Чекпоинты на следующих сегментах считаются последними")
    void getLastSegmentStatusTwoSegments() {
        WaybillSegment firstSegment = new WaybillSegment();
        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            firstSegment,
            SegmentStatus.IN,
            Instant.parse("2022-02-10T03:00:00Z")
        );
        WaybillSegment secondSegment = new WaybillSegment();
        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            secondSegment,
            SegmentStatus.IN,
            Instant.parse("2022-02-09T03:00:00Z")
        );
        Order order = WaybillSegmentFactory.joinInOrder(List.of(firstSegment, secondSegment));

        WaybillSegmentStatusHistory foundSegmentStatus =
            WaybillUtils.getLastSegmentStatusHistoryWithStatus(order, Set.of(SegmentStatus.IN)).get();
        softly.assertThat(foundSegmentStatus.getDate()).isEqualTo(Instant.parse("2022-02-09T03:00:00Z"));
    }

    @Test
    @DisplayName("Найти чекпоинт с максимальным временем среди первых на каждом сегменте(разные статусы)")
    void getLastSegmentStatusHistoryWithStatusTestInOut() {
        WaybillSegment firstSegment = new WaybillSegment();
        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            firstSegment,
            SegmentStatus.IN,
            Instant.parse("2022-02-08T03:00:00Z")
        );

        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            firstSegment,
            SegmentStatus.OUT,
            Instant.parse("2022-02-10T03:00:00Z")
        );
        WaybillSegment secondSegment = new WaybillSegment();
        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            secondSegment,
            SegmentStatus.IN,
            Instant.parse("2022-02-09T03:00:00Z")
        );
        Order order = WaybillSegmentFactory.joinInOrder(List.of(firstSegment, secondSegment));

        WaybillSegmentStatusHistory foundSegmentStatus =
            WaybillUtils.getLastSegmentStatusHistoryWithStatus(order, Set.of(SegmentStatus.IN, SegmentStatus.OUT))
                .get();
        softly.assertThat(foundSegmentStatus.getDate()).isEqualTo(Instant.parse("2022-02-09T03:00:00Z"));
    }

    @Test
    @DisplayName("Для МК брать последний чекпоинт среди одинаковых на одном сегменте")
    void getLastSegmentStatusOneSegmentMk() {
        WaybillSegment firstSegment = new WaybillSegment().setPartnerSubtype(PartnerSubtype.MARKET_COURIER);
        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            firstSegment,
            SegmentStatus.IN,
            Instant.parse("2022-02-08T03:00:00Z")
        );

        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            firstSegment,
            SegmentStatus.IN,
            Instant.parse("2022-02-10T03:00:00Z")
        );
        Order order = WaybillSegmentFactory.joinInOrder(List.of(firstSegment));

        WaybillSegmentStatusHistory foundSegmentStatus =
            WaybillUtils.getLastSegmentStatusHistoryWithStatus(order, Set.of(SegmentStatus.IN)).get();
        softly.assertThat(foundSegmentStatus.getDate()).isEqualTo(Instant.parse("2022-02-10T03:00:00Z"));
    }
}

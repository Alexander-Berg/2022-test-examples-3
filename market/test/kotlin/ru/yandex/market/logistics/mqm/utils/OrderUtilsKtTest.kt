package ru.yandex.market.logistics.mqm.utils

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus

internal class OrderUtilsKtTest {

    @DisplayName("Проверка поиска последнего сегмента с нужным статусом")
    @Test
    fun lastSegmentWithAndNextWithout() {
        val testSegment = makeSegment(1, SegmentStatus.OUT)
        val order = joinInOrder(
            listOf(
                testSegment,
                makeSegment(1, SegmentStatus.IN),
            )
        )
        Assertions.assertThat(order. getLastSegmentWithCheckpointAndNextWithoutOrNull(SegmentStatus.OUT)).isEqualTo(testSegment);
    }

    @DisplayName("Ничего не возвращается, если следующего сегмента нет")
    @Test
    fun lastSegmentWithAndNextWithoutReturnNullIfNoNextSegment() {
        val order = joinInOrder(
            listOf(
                makeSegment(1, SegmentStatus.IN),
                makeSegment(2, SegmentStatus.OUT),
            )
        )
        Assertions.assertThat(order.getLastSegmentWithCheckpointAndNextWithoutOrNull(SegmentStatus.OUT)).isNull();
    }

    @DisplayName("Ничего не возвращается, если сегмента с нужным статусом нет")
    @Test
    fun lastSegmentWithAndNextWithoutReturnNullIfStatusNotFound() {
        val order = joinInOrder(
            listOf(
                makeSegment(1, SegmentStatus.IN),
                makeSegment(2, SegmentStatus.IN),
            )
        )
        Assertions.assertThat(order.getLastSegmentWithCheckpointAndNextWithoutOrNull(SegmentStatus.OUT)).isNull();
    }

    private fun makeSegment(id: Long, status: SegmentStatus) = WaybillSegment(id = id).apply {
        waybillSegmentStatusHistory = mutableSetOf(WaybillSegmentStatusHistory(status = status))
    }
}

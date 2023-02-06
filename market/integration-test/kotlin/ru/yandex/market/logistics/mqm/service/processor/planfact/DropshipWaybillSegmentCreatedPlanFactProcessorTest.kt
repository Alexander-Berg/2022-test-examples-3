package ru.yandex.market.logistics.mqm.service.processor.planfact

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import java.util.stream.Stream

@DisplayName("Создание план-фактов для первого сегмента-дропшипа")
class DropshipWaybillSegmentCreatedPlanFactProcessorTest : AbstractCreatedPlanFactProcessorTest() {

    @Autowired
    private lateinit var dropshipCreatedPlanFactProcessor: DropshipCreatedPlanFactProcessor

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Проверка на доступность процессора")
    fun eligible(
        name: String?,
        waybillSegment: WaybillSegment,
        eligible: Boolean
    ) {
        orderWithSegments(mutableListOf(waybillSegment))
        softly.assertThat(dropshipCreatedPlanFactProcessor.isEligible(waybillSegment)).isEqualTo(eligible)
    }

    companion object {
        @JvmStatic
        private fun eligible() =
            Stream.of(
                Arguments.of(
                    "Сегмент не дропшип, заказ не создан",
                    mockSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    false
                ),
                Arguments.of(
                    "Сегмент не дропшип, заказ создан",
                    mockSegmentWithOrderCreated(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    false
                ),
                Arguments.of(
                    "Сегмент дропшип, заказ создан",
                    mockSegmentWithOrderCreated(PartnerType.DROPSHIP, SegmentType.FULFILLMENT, 0),
                    false
                ),
                Arguments.of(
                    "Не первый сегмент",
                    mockSegmentWithOrderCreated(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER, 3),
                    false
                )
            )
    }
}

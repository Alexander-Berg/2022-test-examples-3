package ru.yandex.market.logistics.mqm.admin.model.enums

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.logistics.mqm.entity.enums.Cause
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import java.util.stream.Stream

class AdminModelEnumMappingTest {

    @ParameterizedTest
    @MethodSource("getMappedEnums")
    fun testEnumsMapping(enum1: Class<out Enum<*>>, enum2: Class<out Enum<*>>) {
        assertSoftly {
            toStringSet(enum1) shouldContainExactly toStringSet(enum2)
        }
    }

    private fun toStringSet(enum: Class<out Enum<*>>) = enum.enumConstants.map { it.name }.toSet()

    companion object {
        @JvmStatic
        fun getMappedEnums() = Stream.of(
            Arguments.of(
                EntityType::class.java,
                AdminPlanFactEntityType::class.java
            ),
            Arguments.of(
                SegmentType::class.java,
                AdminPlanFactSegmentType::class.java
            ),
            Arguments.of(
                PlanFactStatus::class.java,
                AdminPlanFactStatus::class.java
            ),
            Arguments.of(
                OrderStatus::class.java,
                AdminLomOrderStatus::class.java
            ),
            Arguments.of(
                ProcessingStatus::class.java,
                AdminPlanFactProcessingStatus::class.java
            ),
            Arguments.of(
                Cause::class.java,
                AdminPlanFactCause::class.java
            ),
            Arguments.of(
                DeliveryType::class.java,
                AdminLomOrderDeliveryType::class.java
            ),
        )
    }
}

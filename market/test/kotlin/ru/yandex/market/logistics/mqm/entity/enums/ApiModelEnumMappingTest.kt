package ru.yandex.market.logistics.mqm.entity.enums

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.model.enums.AggregationType
import ru.yandex.market.logistics.mqm.model.enums.Cause
import ru.yandex.market.logistics.mqm.model.enums.EntityType
import ru.yandex.market.logistics.mqm.model.enums.EventType
import ru.yandex.market.logistics.mqm.model.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.model.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.model.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.model.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.model.lom.enums.SegmentType
import java.util.stream.Stream

class ApiModelEnumMappingTest : AbstractTest() {

    @ParameterizedTest
    @MethodSource("getMappedEnums")
    fun testEnumsMapping(enum1: Class<out Enum<*>>, enum2: Class<out Enum<*>>) {
        assertSoftly {
            toStringSet(enum1) shouldContainExactly toStringSet(enum2)
        }
    }

    private fun toStringSet(enum: Class<out Enum<*>>) = enum.enumConstants.map { it.name } .toSet()

    companion object {
        @JvmStatic
        fun getMappedEnums() = Stream.of(
            Arguments.of(
                Cause::class.java,
                ru.yandex.market.logistics.mqm.entity.enums.Cause::class.java
            ),
            Arguments.of(
                EntityType::class.java,
                ru.yandex.market.logistics.mqm.entity.enums.EntityType::class.java
            ),
            Arguments.of(
                QualityRuleProcessorType::class.java,
                ru.yandex.market.logistics.mqm.entity.enums.QualityRuleProcessorType::class.java
            ),
            Arguments.of(
                PlanFactStatus::class.java,
                ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus::class.java
            ),
            Arguments.of(
                ProcessingStatus::class.java,
                ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus::class.java
            ),
            Arguments.of(
                SegmentStatus::class.java,
                ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus::class.java
            ),
            Arguments.of(
                SegmentType::class.java,
                ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType::class.java
            ),
            Arguments.of(
                AggregationType::class.java,
                ru.yandex.market.logistics.mqm.entity.enums.AggregationType::class.java
            ),
            Arguments.of(
                EventType::class.java,
                ru.yandex.market.logistics.mqm.monitoringevent.event.EventType::class.java
            )
        )
    }
}

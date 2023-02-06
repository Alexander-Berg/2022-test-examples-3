package ru.yandex.market.logistics.les

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.logistics.les.admin.model.enums.AdminEntityType
import ru.yandex.market.logistics.les.base.EntityType

class EnumMappingTest {

    @ParameterizedTest
    @MethodSource("getMappedEnums")
    fun testEnumsMapping(enum1: Class<out Enum<*>>, enum2: Class<out Enum<*>>) {
        assertSoftly {
            toStringSet(enum1) shouldBe toStringSet(enum2)
        }
    }

    companion object {
        @JvmStatic
        fun getMappedEnums() = listOf(
            Arguments.of(
                EntityType::class.java,
                AdminEntityType::class.java
            )
        )
    }

    private fun toStringSet(enum: Class<out Enum<*>>) = enum.enumConstants.map { it.name }.toSet()
}

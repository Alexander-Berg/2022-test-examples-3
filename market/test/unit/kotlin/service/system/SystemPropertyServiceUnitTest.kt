package ru.yandex.market.logistics.calendaring.service.system

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.calendaring.base.SoftAssertionSupport
import ru.yandex.market.logistics.calendaring.model.entity.SystemProperty
import ru.yandex.market.logistics.calendaring.repository.SystemPropertyJpaRepository
import ru.yandex.market.logistics.calendaring.service.system.keys.SystemPropertySetIntegerKey
import ru.yandex.market.logistics.calendaring.service.system.keys.SystemPropertySetLongKey


class SystemPropertyServiceUnitTest : SoftAssertionSupport() {

    private var systemPropertyRepository: SystemPropertyJpaRepository =
        Mockito.mock(SystemPropertyJpaRepository::class.java)
    private var systemPropertyService: SystemPropertyService = SystemPropertyService(systemPropertyRepository)


    @Test
    fun systemPropertySetLongForNotEmptyString() {
        Mockito.`when`(
            systemPropertyRepository
                .findByName(SystemPropertySetLongKey.FEATURE_GATE_INDIVIDUAL_SCHEDULE_ENABLED_FOR_WAREHOUSES.name)
        )
            .thenReturn(createSystemProperty(" 123, 456   "))

        val actual: Set<Long> =
            systemPropertyService.getProperty(SystemPropertySetLongKey.FEATURE_GATE_INDIVIDUAL_SCHEDULE_ENABLED_FOR_WAREHOUSES)!!
        softly.assertThat(actual).containsExactlyInAnyOrder(123L, 456L)
    }

    @Test
    fun systemPropertySetLongForEmptyString() {
        Mockito.`when`(
            systemPropertyRepository
                .findByName(SystemPropertySetLongKey.FEATURE_GATE_INDIVIDUAL_SCHEDULE_ENABLED_FOR_WAREHOUSES.name)
        )
            .thenReturn(createSystemProperty(" "))

        val actual: Set<Long> =
            systemPropertyService.getProperty(SystemPropertySetLongKey.FEATURE_GATE_INDIVIDUAL_SCHEDULE_ENABLED_FOR_WAREHOUSES)!!
        softly.assertThat(actual).isEmpty()
    }

    @Test
    fun systemPropertySetIntegerForNotEmptyString() {
        Mockito.`when`(
            systemPropertyRepository
                .findByName(SystemPropertySetIntegerKey.EXAMPLE.name)
        )
            .thenReturn(createSystemProperty(" 123, 456   "))
        val actual: Set<Int> = systemPropertyService.getProperty(SystemPropertySetIntegerKey.EXAMPLE)
        softly.assertThat(actual).containsExactlyInAnyOrder(123, 456)
    }

    @Test
    fun systemPropertySetIntegerForEmptyString() {
        Mockito.`when`(
            systemPropertyRepository
                .findByName(SystemPropertySetIntegerKey.EXAMPLE.name)
        )
            .thenReturn(createSystemProperty(""))
        val actual: Set<Int> = systemPropertyService.getProperty(SystemPropertySetIntegerKey.EXAMPLE)
        softly.assertThat(actual).isEmpty()
    }

    private fun createSystemProperty(value: String): SystemProperty {
        val systemProperty = SystemProperty()
        systemProperty.value = value
        return systemProperty
    }
}

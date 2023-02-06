package ru.yandex.market.logistics.mqm.service.processor.settings

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.convert.ConversionService
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.processor.settings.payload.TestPlanFactProcessorSettings
import java.time.Duration
import java.time.LocalTime
import java.util.EnumSet
import java.util.Locale

class PlanFactProcessorSettingServiceImplTest: AbstractContextualTest() {
    @Autowired
    private lateinit var conversionService: ConversionService

    @Autowired
    private lateinit var settingService: PlanFactProcessorSettingService

    @ParameterizedTest
    @EnumSource(SettingType::class)
    @DisplayName("Проверка, что все типы параметров имеют конвертер из строки, и обратно")
    fun checkAllParamsTypeConverters(type: SettingType) {
        assertSoftly {
            conversionService.canConvert(String::class.java, type.kotlinType.java)
            conversionService.canConvert(type.kotlinType.java, String::class.java)
        }
    }

    @Test
    @DisplayName("Проверка загрузки из бд настроек всех типов")
    @DatabaseSetup("/service/processor/settings/settings_service/before/all_types.xml")
    fun loadSettingsOfAllTypes() {
        //При запуске через ya make там en, через idea ru. Из-за чего разные результаты тест
        LocaleContextHolder.setDefaultLocale(Locale.ENGLISH)
        data class TestDataDto(
            val settingType: SettingType,
            val key: String,
            val expectedValue: Any,
            val defaultValue: Any,
        )

        val testData = listOf(
            TestDataDto(SettingType.STRING, key = "string_key", expectedValue = "string_value", defaultValue = ""),
            TestDataDto(SettingType.LONG, key = "long_key", expectedValue = 123456789L, defaultValue = 1L),
            TestDataDto(SettingType.BOOLEAN, key = "boolean_key", expectedValue = true, defaultValue = false),
            TestDataDto(
                SettingType.TIME,
                key = "time_key",
                expectedValue = LocalTime.of(12, 15),
                defaultValue = LocalTime.MIN,
            ),
            TestDataDto(
                SettingType.DURATION,
                key = "duration_key",
                expectedValue = Duration.ofSeconds(65),
                defaultValue = Duration.ZERO,
            ),
            TestDataDto(
                SettingType.JSON,
                key = "json_key",
                expectedValue = TestPlanFactProcessorSettings(name = "test"),
                defaultValue = TestPlanFactProcessorSettings(name = ""),
            ),
        )

        //Проверка, что в тестовых данных есть все типы.
        testData.map { it.settingType }.toSet() shouldBe EnumSet.allOf(SettingType::class.java)
        assertSoftly {
            testData.forEach {
                val value = settingService.getOrDefault("TestPlanFactProcessor", it.key, it.defaultValue)
                value shouldBe it.expectedValue
            }
        }
    }

    @Test
    @DisplayName("Бросить ошибку, если передан тип несоответствующий настройке в бд")
    @DatabaseSetup("/service/processor/settings/settings_service/before/all_types.xml")
    fun throwExceptionIfWrongType() {
        assertThrows<IllegalStateException> {
            settingService.getOrDefault("TestPlanFactProcessor", "boolean_key", "")
        }
    }

    @Test
    @DisplayName("Бросить ошибку, если передан тип несоответствующий настройке в бд для json")
    @DatabaseSetup("/service/processor/settings/settings_service/before/all_types.xml")
    fun throwExceptionIfWrongTypeJson() {
        data class Bar(val name: String)
        assertThrows<IllegalStateException> {
            settingService.getOrDefault("TestPlanFactProcessor", "json_key", Bar(name = ""))
        }
    }
}

package ru.yandex.market.logistics.mqm.service.processor.settings.converter

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.processor.settings.payload.LomOrderSegmentsCreationAttributes
import ru.yandex.market.logistics.mqm.service.processor.settings.PlanFactProcessorSettingService
import ru.yandex.market.logistics.mqm.service.processor.settings.payload.ShootingPlanFactProcessorSettings

class ShootingPlanFactProcessorSettingsConverterTest: AbstractContextualTest() {

    @Autowired
    private lateinit var settingService: PlanFactProcessorSettingService

    @Test
    @DisplayName("Проверка корректной загрузки настроек")
    @DatabaseSetup("/service/processor/settings/converter/shooting/before/settings.xml")
    fun loadSettings() {
        val value = settingService.getOrDefault(
            PROCESSOR_NAME,
            SETTINGS_KEY,
            createSettings(),
        )
        value shouldBe createSettings(count = 1)
    }

    private fun createSettings(count: Int = 2): ShootingPlanFactProcessorSettings {
        return ShootingPlanFactProcessorSettings(
            lomOrderSegmentsCreationAttributes = listOf(
                LomOrderSegmentsCreationAttributes(
                    segmentType = SegmentType.FULFILLMENT.name,
                    count = count,
                    expectedActiveCountFactor = 1.0,
                ),
            ),
        )
    }

    companion object {
        private const val PROCESSOR_NAME = "TestPlanFactProcessor"
        private const val SETTINGS_KEY = "SETTINGS"
    }
}

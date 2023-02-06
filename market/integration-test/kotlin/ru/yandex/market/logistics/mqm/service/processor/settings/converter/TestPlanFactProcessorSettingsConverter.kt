package ru.yandex.market.logistics.mqm.service.processor.settings.converter

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import ru.yandex.market.logistics.mqm.service.processor.settings.payload.TestPlanFactProcessorSettings

@Component
class TestPlanFactProcessorSettingsConverter(
    private val objectMapper: ObjectMapper,
): Converter<String, TestPlanFactProcessorSettings> {
    override fun convert(source: String): TestPlanFactProcessorSettings =
        objectMapper.readValue(source, TestPlanFactProcessorSettings::class.java)
}

package ru.yandex.market.logistics.mqm.utils

import org.springframework.data.domain.Pageable
import ru.yandex.market.logistics.mqm.entity.processor.settings.PlanFactProcessorSetting
import ru.yandex.market.logistics.mqm.filter.InternalPlanFactProcessorSettingSearchFilter
import ru.yandex.market.logistics.mqm.service.processor.settings.PlanFactProcessorSettingService

class TestableSettingsService: PlanFactProcessorSettingService {
    override fun <T: Any> getOrDefault(processorName: String, key: String, defaultValue: T): T {
        return defaultValue
    }

    override fun findByFilter(
        filter: InternalPlanFactProcessorSettingSearchFilter,
        pageable: Pageable
    ): List<PlanFactProcessorSetting> = listOf()

    override fun getByIdOrThrow(id: Long): PlanFactProcessorSetting {
        TODO("Not yet implemented")
    }
}

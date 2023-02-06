package ru.yandex.market.mapi.mock

import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestComponent
import ru.yandex.market.mapi.core.contract.InfraConfigProvider

@TestComponent
class InfraConfigMocker {
    @Autowired
    private lateinit var infraConfigProvider: InfraConfigProvider

    fun setJobEnabled(jobName: String, value: Boolean?) {
        whenever(infraConfigProvider.isPumpkinEnabled(eq(jobName))).thenReturn(value)
    }

    fun setPumpkinEnabled(pumpkinId: String, value: Boolean?) {
        whenever(infraConfigProvider.isPumpkinEnabled(eq(pumpkinId))).thenReturn(value)
    }

    fun setSectionsDegradationEnabled(value: Boolean) {
        whenever(infraConfigProvider.isDegradationEnabled()).thenReturn(value)
    }

    fun setSectionDegradationEnabled(sectionId: String, value: Boolean) {
        whenever(infraConfigProvider.isSectionDegradationActive(sectionId)).thenReturn(value)
    }
}

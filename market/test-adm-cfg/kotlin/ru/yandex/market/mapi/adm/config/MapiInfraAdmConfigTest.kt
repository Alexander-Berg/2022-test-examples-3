package ru.yandex.market.mapi.adm.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.AbstractAdmConfigTest
import ru.yandex.market.mapi.configprovider.InfraConfigProviderImpl
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MapiInfraAdmConfigTest: AbstractAdmConfigTest() {
    private lateinit var infraConfigProvider: InfraConfigProviderImpl

    @BeforeEach
    fun setup() {
        infraConfigProvider = InfraConfigProviderImpl(client)
    }

    @Test
    fun testDegradationConfig() {
        infraConfigProvider.refreshConfig()

        assertNotNull(infraConfigProvider.getConfig())
        assertEquals(true, infraConfigProvider.isDegradationEnabled())

        assertEquals(true, infraConfigProvider.isSectionDegradationActive("sectionId1"))
        assertEquals(false, infraConfigProvider.isSectionDegradationActive("sectionId2"))
    }

    @Test
    fun testDefaultDegradationConfig() {
        assertNotNull(infraConfigProvider.getConfig())
        assertEquals(false, infraConfigProvider.isDegradationEnabled())

        assertEquals(true, infraConfigProvider.isSectionDegradationActive("defaultSection1")) // defaultSection1
        assertEquals(false, infraConfigProvider.isSectionDegradationActive("sectionId1"))
    }

    @Test
    fun testPumpkinConfig() {
        infraConfigProvider.refreshConfig()

        assertEquals(true, infraConfigProvider.isPumpkinEnabled("pumpkin.id.1"))
        assertEquals(false, infraConfigProvider.isPumpkinEnabled("pumpkin.id.2"))

        assertNull(infraConfigProvider.isPumpkinEnabled("pumpkin.id.3"))
    }

    @Test
    fun testDefaultPumpkinConfig() {
        assertEquals(true, infraConfigProvider.isPumpkinEnabled("pumpkin.default.id.1"))
        assertEquals(false, infraConfigProvider.isPumpkinEnabled("pumpkin.default.id.2"))

        assertNull(infraConfigProvider.isPumpkinEnabled("pumpkin.id.3"))
    }

    @Test
    fun testTmsConfig() {
        infraConfigProvider.refreshConfig()

        assertEquals(true, infraConfigProvider.isJobEnabled("job1"))
        assertEquals(false, infraConfigProvider.isJobEnabled("job2"))
    }

    @Test
    fun testDefaultTmsConfig() {
        assertEquals(true, infraConfigProvider.isJobEnabled("job1"))
        assertEquals(false, infraConfigProvider.isJobEnabled("defaultJob2"))
    }
}

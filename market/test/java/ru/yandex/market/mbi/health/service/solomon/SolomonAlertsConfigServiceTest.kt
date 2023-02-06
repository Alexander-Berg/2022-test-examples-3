package ru.yandex.market.mbi.health.service.solomon

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.lang.Exception

class SolomonAlertsConfigServiceTest {

    @Test
    fun `should read default config`() {
        try {
            val configService = SolomonAlertsConfigService()
            configService.readConfig()
        } catch (e: Exception) {
            fail("failed to parse default configuration", e)
        }
    }

    @Test
    fun `should not allow empty fields in default config`() {
        val config = SolomonAlertsConfigService().readConfig()
        checkEmptyFields(config)
    }

    private fun checkEmptyFields(config: List<ProjectAlertConfig>) {
        config.forEach { project ->
            assertFalse(project.dashboardLink.isBlank())
            assertFalse(project.dashboardName.isBlank())
            assertFalse(project.alerts.isEmpty());
            project.alerts.forEach { alert ->
                assertFalse(alert.projectId.isEmpty())
                assertFalse(alert.alert.isEmpty())
                assertTrue(alert.weight.isFinite())
            }
        }
    }
}




package ru.yandex.market.ext.marketplace.integrator.app.settings.ozon

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.ext.marketplace.integrator.app.AbstractFunctionalTest

@DbUnitDataSet(before = ["ozon-integration-repository-test-before.csv"])
class OzonIntegrationSettingsRepositoryTest : AbstractFunctionalTest() {
    @Autowired
    lateinit var repository: OzonIntegrationSettingsRepository

    @Test
    fun testGetSettingsByPartnerId() {
        val entity1 = OzonIntegrationEntity(1, 11, "X11x1", true)
        val entity2 = OzonIntegrationEntity(2, 22, "X22x2", false)
        Assertions.assertThat(repository.findSettingsByPartnerId(1L)).isEqualTo(entity1)
        Assertions.assertThat(repository.findSettingsByPartnerId(2L)).isEqualTo(entity2)
    }

    @Test
    @DbUnitDataSet(after = ["ozon-integration-repository-test-update-after.csv"])
    fun testUpdateSettingsByPartnerId() {
        val entityForUpdate1 = OzonIntegrationEntity(1, 11, "X11x1v1", true)
        val entityForUpdate2 = OzonIntegrationEntity(2, 22, "X22x2", true)
        val entityForUpdate3 = OzonIntegrationEntity(3, 33, "X33x3", true)
        val entityForUpdate4 = OzonIntegrationEntity(4, null, null, null)
        val entityForUpdate5 = OzonIntegrationEntity(5, null, "newKey", null)
        val entityForUpdate6 = OzonIntegrationEntity(6, 66, "newKey6", null)
        val entityForUpdate7 = OzonIntegrationEntity(7, null, null, null)
        repository.upsertSettings(entityForUpdate1)
        repository.upsertSettings(entityForUpdate2)
        repository.upsertSettings(entityForUpdate3)
        repository.upsertSettings(entityForUpdate4)
        repository.upsertSettings(entityForUpdate5)
        repository.upsertSettings(entityForUpdate6)
        repository.upsertSettings(entityForUpdate7)
    }
}

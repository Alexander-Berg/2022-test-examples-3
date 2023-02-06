package ru.yandex.market.wms.core.service.impl

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.service.DimensionsConfigService
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.core.dao.LocDao
import ru.yandex.market.wms.core.dao.SkuDao
import ru.yandex.market.wms.core.service.UpdateSusr7Service
import ru.yandex.market.wms.common.dao.SkuDao as CommonSkuDao

class UpdateSusr7ServiceImplTest(
    @Autowired private val locDao: LocDao,
    @Autowired private val skuDao: SkuDao,
    @Autowired private val dbConfig: DbConfigService,
    @Autowired private val dimService: DimensionsConfigService,
    @Autowired private val commonSkuDao: CommonSkuDao,
) : IntegrationTest() {
    private val updateSusr7Service: UpdateSusr7Service = UpdateSusr7ServiceImpl(locDao, skuDao, dbConfig, dimService,
        commonSkuDao)

    @Test
    @DatabaseSetup("/service/update-susr7/loc-null-skus-empty/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/loc-null-skus-empty/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 doesn't update susr7 when loc is null and SKUs is empty`() {
        updateSusr7Service.updateSusr7(null, listOf())
    }

    @Test
    @DatabaseSetup("/service/update-susr7/loc-null-skus-not-relevant/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/loc-null-skus-not-relevant/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 doesn't update susr7 when loc is null and SKUs is not relevant`() {
        updateSusr7Service.updateSusr7(null, listOf(SkuId("10000","ROV0000000000000000001")))
    }

    @Test
    @DatabaseSetup("/service/update-susr7/loc-null-skus-relevant/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/loc-null-skus-relevant/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 updates suitable susr7 when loc is null and SKUs contains relevant SKU`() {
        updateSusr7Service.updateSusr7(null, listOf(SkuId("10000","ROV0000000000000000001")))
    }

    @Test
    @DatabaseSetup("/service/update-susr7/loc-not-relevant-skus-empty/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/loc-not-relevant-skus-empty/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 doesn't update susr7 when loc is not relevant and SKUs is empty`() {
        updateSusr7Service.updateSusr7("LOC-A", listOf())
    }

    @Test
    @DatabaseSetup("/service/update-susr7/loc-not-relevant-skus-not-relevant/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/loc-not-relevant-skus-not-relevant/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 doesn't update susr7 when loc is not relevant and SKUs is not relevant`() {
        updateSusr7Service.updateSusr7("LOC-A", listOf(SkuId("10000","ROV0000000000000000001")))
    }

    @Test
    @DatabaseSetup("/service/update-susr7/loc-not-relevant-skus-relevant/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/loc-not-relevant-skus-relevant/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 doesn't update suitable susr7 when loc is not relevant and SKUs contains relevant SKU`() {
        updateSusr7Service.updateSusr7("LOC-A", listOf(SkuId("9999","ROV0000000000000000001")))
    }

    @Test
    @DatabaseSetup("/service/update-susr7/loc-relevant-skus-empty/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/loc-relevant-skus-empty/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 doesn't update susr7 when loc is relevant and SKUs is empty`() {
        updateSusr7Service.updateSusr7("LOC-A", listOf())
    }

    @Test
    @DatabaseSetup("/service/update-susr7/loc-relevant-skus-not-relevant/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/loc-relevant-skus-not-relevant/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 doesn't update susr7 when loc is relevant and SKUs is not relevant`() {
        updateSusr7Service.updateSusr7("LOC-A", listOf(SkuId("10000","ROV0000000000000000001")))
    }

    @Test
    @DatabaseSetup("/service/update-susr7/loc-relevant-skus-relevant/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/loc-relevant-skus-relevant/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 updates suitable susr7 when loc is relevant and SKUs contains relevant SKU`() {
        updateSusr7Service.updateSusr7("LOC-A", listOf(SkuId("10000","ROV0000000000000000001")))
    }

    @Test
    @DatabaseSetup("/service/update-susr7/conf-not-set/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/conf-not-set/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 doesn't update susr7 when loc is not null and YM_AREAS_FOR_SUSR7_IGNORE is not set`() {
        updateSusr7Service.updateSusr7("LOC-A", listOf(SkuId("10000","ROV0000000000000000001")))
    }

    @Test
    @DatabaseSetup("/service/update-susr7/conf-blank/before.xml")
    @ExpectedDatabase(
        value = "/service/update-susr7/conf-blank/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Update susr7 doesn't update susr7 when loc is not null and YM_AREAS_FOR_SUSR7_IGNORE is blank`() {
        updateSusr7Service.updateSusr7("LOC-A", listOf(SkuId("10000","ROV0000000000000000001")))
    }
}

package ru.yandex.market.wms.inbound_management.dao

import com.github.springtestdbunit.annotation.DatabaseOperation.CLEAN_INSERT
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.TestConstructor
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.inbound_management.controller.dto.SkuToOos
import java.math.BigDecimal
import java.time.Clock
import kotlin.test.assertTrue

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SkuToOosDaoTest(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val clock: Clock,
    private val securityDataProvider: ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider
) : IntegrationTest() {

    @Autowired
    private lateinit var skuToOosDao: SkuToOosDao

    @BeforeEach
    fun setupDao() {
        skuToOosDao = SkuToOosDao(jdbcTemplate, clock, securityDataProvider)
    }

    companion object {
        val sku1 = SkuId("942665", "ROV0000000000000000359")
        val sku2 = SkuId("546763", "ROV0000000000000000360")
        val sku3 = SkuId("546019", "ROV0000000000000000361")
    }

    @Test
    @DatabaseSetup(value = ["/db/sku-to-oos/before-insert.xml"])
    @ExpectedDatabase(value = "/db/sku-to-oos/after-insert.xml", assertionMode = NON_STRICT_UNORDERED)
    fun insert() {
        val skuOos1 = SkuToOos(
            sku1.sku, sku1.storerKey, BigDecimal.valueOf(459333, 4),
            BigDecimal.valueOf(0.0833)
        )
        val skuOos2 = SkuToOos(sku2.sku, sku2.storerKey, BigDecimal.valueOf(6.5), BigDecimal.valueOf(0.0166))
        val skuOos3 = SkuToOos(sku3.sku, sku3.storerKey, BigDecimal.valueOf(17.234), BigDecimal.valueOf(10.3))
        skuToOosDao.insert(listOf(skuOos1, skuOos2))
        skuToOosDao.insert(skuOos3)
    }

    @Test
    @DatabaseSetup(value = ["/db/sku-to-oos/before-update.xml"], type = CLEAN_INSERT)
    @ExpectedDatabase(value = "/db/sku-to-oos/after-update.xml", assertionMode = NON_STRICT_UNORDERED)
    fun updatePriority() {
        val skuOos1 = SkuToOos(sku1.sku, sku1.storerKey, BigDecimal.valueOf(2.4), BigDecimal.valueOf(1.98))
        val skuOos2 = SkuToOos(sku2.sku, sku2.storerKey, BigDecimal.valueOf(58), BigDecimal.valueOf(15.0))
        val skuOos3 = SkuToOos(sku3.sku, sku3.storerKey, BigDecimal.valueOf(8.0), BigDecimal.valueOf(0.12))
        skuToOosDao.update(skuOos1)
        skuToOosDao.update(listOf(skuOos2, skuOos3))
    }

    @Test
    @DatabaseSetup(value = ["/db/sku-to-oos/after-update.xml"])
    fun selectBySkuIds() {
        val selected = skuToOosDao.selectBySkuIds(listOf(sku1, sku2))
        assertTrue {
            selected[sku1] == SkuToOos(
                sku1.sku,
                sku1.storerKey,
                BigDecimal.valueOf(24000, 4),
                BigDecimal.valueOf(19800, 4)
            )
        }
        assertTrue {
            selected[sku2] == SkuToOos(
                sku2.sku,
                sku2.storerKey,
                BigDecimal.valueOf(580000, 4),
                BigDecimal.valueOf(150000, 4)
            )
        }
    }
}

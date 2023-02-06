package ru.yandex.market.wms.constraints.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.isA
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionParam
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionType
import ru.yandex.market.wms.constraints.core.domain.SkuCharacteristic
import ru.yandex.market.wms.constraints.core.domain.SkuSpecification
import ru.yandex.market.wms.constraints.integration.core.SkuService
import ru.yandex.market.wms.core.base.dto.LocationDto
import ru.yandex.market.wms.core.base.dto.LocationType
import ru.yandex.market.wms.core.base.response.GetLocationByLocIdResponse

class CheckServiceTest : ConstraintsIntegrationTest() {

    @Autowired
    private lateinit var checkService: CheckService

    @Autowired
    @MockBean
    private lateinit var skuService: SkuService

    @BeforeEach
    fun setUp() {
        whenever(coreClient.getLocationByLocId(isA())).then {
            val loc = it.arguments[0].toString()
            GetLocationByLocIdResponse(LocationDto(loc, LocationType.OTHER, ""))
        }
    }

    @Test
    @DatabaseSetup("/service/check/rules.xml")
    @ExpectedDatabase(
        value = "/service/check/rules.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `test rule 1`() {
        val loc = "MEZ_01_06"
        val sku = "ROV0000000824"
        val storerKey = "465852"

        val skuInfo = SkuSpecification(
            "ROV0000000824", "465852", null, null,
            listOf(
                SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "750"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.WEIGHT, "2"),
            )
        )

        Mockito.`when`(skuService.findSku(listOf(SkuId(sku, storerKey)))).thenReturn(listOf(skuInfo))

        checkService.checkByLocAndSku(loc, listOf(SkuId(sku, storerKey)))
    }
}

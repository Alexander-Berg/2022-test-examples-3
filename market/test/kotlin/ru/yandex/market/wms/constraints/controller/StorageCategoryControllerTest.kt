package ru.yandex.market.wms.constraints.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.isA
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.model.dto.SkuIdDto
import ru.yandex.market.wms.common.model.enums.MeasurementReason
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionParam
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionType
import ru.yandex.market.wms.constraints.core.domain.SkuCharacteristic
import ru.yandex.market.wms.constraints.core.domain.SkuSpecification
import ru.yandex.market.wms.constraints.integration.core.SkuService
import ru.yandex.market.wms.core.base.dto.SkuCharacteristicType
import ru.yandex.market.wms.core.base.request.SkuCharacteristicsRequest
import ru.yandex.market.wms.core.base.request.SkuNeedMeasurementRequest
import ru.yandex.market.wms.core.base.response.SkuCharacteristicsResponse
import ru.yandex.market.wms.core.base.dto.SkuCharacteristic as SkuCharacteristicDto

class StorageCategoryControllerTest : ConstraintsIntegrationTest() {

    @Autowired
    @MockBean
    private lateinit var skuService: SkuService

    private val storerKey = "465852"
    private val expSku1 = "ROV0000000825"
    private val expMsku1 = "MSKU000000825"
    private val expSkuInfo1 = SkuSpecification(
        expSku1, storerKey, expMsku1, "Айфон",
        listOf(
            SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "999"),
        )
    )
    private val thermalSku = "ROV0000000826"
    private val thermalMsku = "MSKU000000826"
    private val thermalSkuInfo = SkuSpecification(
        thermalSku, storerKey, thermalMsku, "Киндер-сюрприз",
        listOf(
            SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "111"),
        )
    )

    private val normalSku = "ROV0000000824"
    private val normalMsku = "MSKU000000824"
    private val normalSkuInfo = SkuSpecification(
        normalSku, storerKey, normalMsku, "Товар 1",
        listOf(
            SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "750"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.WEIGHT, "1200"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "10"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "12"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "35"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.MAX_SIDE, "35.000"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.MIN_SIDE, "10.000"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.MID_SIDE, "12.000"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.WLH_SUM, "57.000"),
        )
    )

    @Test
    @DatabaseSetup("/controller/storage-category/ok/db.xml")
    fun `should define as expensive`() {
        whenever(skuService.findSku(argThat { size == 3 }))
            .thenReturn(listOf(expSkuInfo1, thermalSkuInfo, normalSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/storage-category/find-by-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/storage-category/ok/request.json"))

        sendRequest(request, "ok")
    }

    @Test
    @DatabaseSetup("/controller/storage-category/two-reasons/db.xml")
    fun `should return two reasons even if each range has only one`() {
        whenever(skuService.findSku(argThat { size == 1 }))
            .thenReturn(listOf(normalSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/storage-category/find-by-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/storage-category/two-reasons/request.json"))

        sendRequest(request, "two-reasons")
    }

    @Test
    @DatabaseSetup("/controller/storage-category/issue/before.xml")
    @ExpectedDatabase(
        value = "/controller/storage-category/issue/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `should create two issues`() {
        val sku = "ROV0000000824"
        val storerKey = "465852"
        val skuSpecification = ru.yandex.market.wms.core.base.dto.SkuSpecification(
            sku, storerKey, "MSKU01", "Товар 1",
            listOf(
                SkuCharacteristicDto(SkuCharacteristicType.CARGO_TYPE, "110"),
                SkuCharacteristicDto(SkuCharacteristicType.CARGO_TYPE, "150"),
                SkuCharacteristicDto(SkuCharacteristicType.WEIGHT, "1200"),
                SkuCharacteristicDto(SkuCharacteristicType.LENGTH, "10"),
                SkuCharacteristicDto(SkuCharacteristicType.HEIGHT, "12"),
                SkuCharacteristicDto(SkuCharacteristicType.WIDTH, "35"),
            )
        )

        whenever(coreClient.getSkuCharacteristics(isA()))
            .thenReturn(SkuCharacteristicsResponse(listOf(skuSpecification)))

        val request = MockMvcRequestBuilders
            .post("/storage-category/issue")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/storage-category/issue/request.json"))

        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)

        verify(coreClient, times(2))
            .getSkuCharacteristics(SkuCharacteristicsRequest(listOf(SkuId(storerKey, sku))))
        verify(coreClient)
            .skuNeedMeasurement(SkuNeedMeasurementRequest(
                SkuIdDto(storerKey.toLong(), sku),
                MeasurementReason.CONSTRAINTS)
            )
    }

    @Test
    @DatabaseSetup("/controller/storage-category/issue-exist/db.xml")
    @ExpectedDatabase(
        value = "/controller/storage-category/issue-exist/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `should not create issue if exist`() {
        val request = MockMvcRequestBuilders
            .post("/storage-category/issue")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/storage-category/issue-exist/request.json"))

        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent("controller/storage-category/issue-exist/response.json"),
                    true
                )
            )

    }

    @Test
    @DatabaseSetup("/controller/storage-category/filter-issues/db.xml")
    fun `should ignore with issues`() {
        whenever(skuService.findSku(argThat { size == 3 }))
            .thenReturn(listOf(expSkuInfo1, thermalSkuInfo, normalSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/storage-category/find-by-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/storage-category/filter-issues/request.json"))

        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(
                        FileContentUtils.getFileContent("controller/storage-category/filter-issues/response.json"),
                        true
                    )
            )
    }

    private fun sendRequest(request: RequestBuilder, case: String) {
        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(FileContentUtils.getFileContent("controller/storage-category/$case/response.json"), true)
            )
    }
}

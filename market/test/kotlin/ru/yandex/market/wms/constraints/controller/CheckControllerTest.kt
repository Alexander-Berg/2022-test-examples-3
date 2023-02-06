package ru.yandex.market.wms.constraints.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.isA
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionParam
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionType
import ru.yandex.market.wms.constraints.core.domain.SkuCharacteristic
import ru.yandex.market.wms.constraints.core.domain.SkuSpecification
import ru.yandex.market.wms.constraints.integration.core.SkuService
import ru.yandex.market.wms.core.base.dto.LocationDto
import ru.yandex.market.wms.core.base.dto.LocationType
import ru.yandex.market.wms.core.base.dto.SerialInventoryDto
import ru.yandex.market.wms.core.base.dto.SkuCharacteristicType
import ru.yandex.market.wms.core.base.request.SkuCharacteristicsRequest
import ru.yandex.market.wms.core.base.response.GetLocationByLocIdResponse
import ru.yandex.market.wms.core.base.response.GetSerialInventoryResponse
import ru.yandex.market.wms.core.base.response.SkuCharacteristicsResponse
import ru.yandex.market.wms.dimensionmanagement.client.DimensionManagementClient
import ru.yandex.market.wms.dimensionmanagement.core.request.CreateBySkuIdRequest
import java.math.BigDecimal
import java.util.stream.Stream
import ru.yandex.market.wms.core.base.dto.SkuCharacteristic as CoreCharacteristic
import ru.yandex.market.wms.core.base.dto.SkuSpecification as CoreSpecification

class CheckControllerTest : ConstraintsIntegrationTest() {
    @MockBean
    @Autowired
    private lateinit var dimensionManagementClient: DimensionManagementClient

    @Autowired
    @MockBean
    private lateinit var skuService: SkuService
    private val normalSku = "ROV0000000824"
    private val normalMsku = "MSKU000000824"
    private val heavySku = "ROV0000000825"
    private val heavyMsku = "ROV0000000825"
    private val noCargoTypeSku = "ROV0000000826"
    private val noCargoTypeMsku = "ROV0000000826"
    private val storerKey = "465852"
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
    private val heavySkuInfo = SkuSpecification(
        heavySku, storerKey, heavyMsku, "Товар 2",
        listOf(
            SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "750"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.WEIGHT, "2000"),
        )
    )
    private val noCargoTypeSkuInfo = SkuSpecification(
        noCargoTypeSku, storerKey, noCargoTypeMsku, "Товар 3",
        listOf(
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.WEIGHT, "1200"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "10"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "12"),
            SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "35"),
        )
    )

    @BeforeEach
    fun setUp() {
        whenever(coreClient.getLocationByLocId(isA())).then {
            val loc = it.arguments[0].toString()
            GetLocationByLocIdResponse(LocationDto(loc, LocationType.OTHER, ""))
        }
    }

    @AfterEach
    fun clear() {
        clearInvocations(coreClient, dimensionManagementClient)
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/rules.xml")
    fun `checkByLocAndSku correct sku and loc`() {
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(normalSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/ok-request.json"))

        sendRequest(request, "ok")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/db-without-rules.xml")
    fun `return true if no rules`() {
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(normalSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/ok-request.json"))

        sendRequest(request, "ok")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/rules.xml")
    fun `checkByLocAndSku too heavy sku`() {
        whenever(skuService.findSku(argThat { size == 1 && first().sku == heavySku }))
            .thenReturn(listOf(heavySkuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/heavy/request.json"))

        sendRequest(request, "heavy")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/rules.xml")
    fun `should forbid without cargo type`() {
        whenever(skuService.findSku(argThat { size == 1 && first().sku == noCargoTypeSku }))
            .thenReturn(listOf(noCargoTypeSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/no-cargo/request.json"))

        sendRequest(request, "no-cargo")
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/check/loc-and-sku/rules.xml"),
        DatabaseSetup("/controller/check/loc-and-sku/receiving-issue/issues.xml")
    )
    fun `should allow because receiving issue exists`() {
        whenever(coreClient.getLocationByLocId("B2-25-04A3"))
            .thenReturn(GetLocationByLocIdResponse(
                LocationDto("B2-25-04A3", LocationType.OTHER, "MEZONIN_1")
            ))

        whenever(skuService.findSku(argThat { size == 1 && first().sku == noCargoTypeSku }))
            .thenReturn(listOf(noCargoTypeSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/receiving-issue/request.json"))

        sendRequest(request, "receiving-issue")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/rules.xml")
    fun `ignore rules for other zone`() {
        whenever(skuService.findSku(argThat { size == 1 && first().sku == heavySku }))
            .thenReturn(listOf(heavySkuInfo))

        whenever(coreClient.getLocationByLocId("B2-25-04A3"))
            .thenReturn(GetLocationByLocIdResponse(
                LocationDto("B2-25-04A3", LocationType.OTHER, "ANOTHER_MEZONIN")
            ))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/heavy/request.json"))

        sendRequest(request, "ok")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/two-cargotype/forbid.xml")
    fun `two cargotypes forbid`() {
        val sku = "ROV0000000001002242586"
        val storerKey = "10264169"
        val skuInfo = SkuSpecification(
            sku, storerKey, "MSKU000000001002242586", "Товар 1",
            listOf(
                SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "500"),
                SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "980"),
            )
        )

        whenever(skuService.findSku(argThat { size == 1 && first().sku == sku }))
            .thenReturn(listOf(skuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/two-cargotype/request.json"))

        sendRequest(request, "two-cargotype/forbid")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/two-cargotype/allow.xml")
    fun `two cargotypes allow`() {
        val sku = "ROV0000000001002242586"
        val storerKey = "10264169"
        val skuInfo = SkuSpecification(
            sku, storerKey, "MSKU000000001002242586", "Товар 1",
            listOf(
                SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "500"),
                SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "980"),
            )
        )

        whenever(skuService.findSku(argThat { size == 1 && first().sku == sku }))
            .thenReturn(listOf(skuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/two-cargotype/request.json"))

        sendRequest(request, "two-cargotype/allow")
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/check/loc-and-sku/aggr/rules-aggr.xml")
    )
    fun `checkByLocAndSku failed by aggregate characteristic`() {
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(normalSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/aggr/request.json"))

        sendRequest(request, "aggr")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/cross-range/rules.xml")
    @ExpectedDatabase(
        value = "/controller/check/loc-and-sku/cross-range/rules.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `do not ignore cross range`() {
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(normalSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/cross-range/request.json"))

        sendRequest(request, "cross-range")
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/check/issue/cargo-type/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createIssue with cargo type`() {
        val sku = "ROV001"
        val storerKey = "465852"
        val serialInventory = SerialInventoryDto("123", storerKey, sku, "LOT", "LOC", "ID", BigDecimal.ONE, "" , "")
        val skuSpecification = CoreSpecification(
            sku, storerKey, "MSKU01", "Товар 1",
            listOf(
                CoreCharacteristic(SkuCharacteristicType.CARGO_TYPE, "10"),
                CoreCharacteristic(SkuCharacteristicType.CARGO_TYPE, "50")
            )
        )

        whenever(coreClient.getSerialInventoryBySerialNumber(any()))
            .thenReturn(GetSerialInventoryResponse(serialInventory))
        whenever(coreClient.getSkuCharacteristics(any()))
            .thenReturn(SkuCharacteristicsResponse(listOf(skuSpecification)))
        whenever(coreClient.getLocationByLocId(any()))
            .thenReturn(GetLocationByLocIdResponse(LocationDto("A1-01-01A1", LocationType.PICK, "ZONE")))

        val request = MockMvcRequestBuilders.post("/check/issue")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/issue/cargo-type/request.json"))

        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)

        verify(coreClient)
            .getSerialInventoryBySerialNumber("123")
        verify(coreClient)
            .getSkuCharacteristics(SkuCharacteristicsRequest(listOf(SkuId(storerKey, sku))))
        verify(coreClient)
            .getLocationByLocId("A1-01-01A1")
        verifyNoMoreInteractions(coreClient)
        verify(dimensionManagementClient, times(0))
            .createMeasurementOrderBySkuId(CreateBySkuIdRequest(storerKey, sku))
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/check/issue/dimension-type/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createIssue with dimension type when the creation of a measurement order disabled`() {
        val sku = "ROV001"
        val storerKey = "465852"
        val serialInventory = SerialInventoryDto("123", storerKey, sku, "LOT", "LOC", "ID", BigDecimal.ONE, "" , "")
        val skuSpecification = CoreSpecification(
            sku, storerKey, "MSKU01", "Товар 1",
            listOf(
                CoreCharacteristic(SkuCharacteristicType.WIDTH, "5"),
                CoreCharacteristic(SkuCharacteristicType.HEIGHT, "2"),
                CoreCharacteristic(SkuCharacteristicType.LENGTH, "1"),
                CoreCharacteristic(SkuCharacteristicType.VOLUME, "10"),
                CoreCharacteristic(SkuCharacteristicType.WEIGHT, "15"),
            )
        )

        whenever(coreClient.getSerialInventoryBySerialNumber(any()))
            .thenReturn(GetSerialInventoryResponse(serialInventory))
        whenever(coreClient.getSkuCharacteristics(any()))
            .thenReturn(SkuCharacteristicsResponse(listOf(skuSpecification)))
        whenever(coreClient.getLocationByLocId(any()))
            .thenReturn(GetLocationByLocIdResponse(LocationDto("A1-01-01A1", LocationType.PICK, "ZONE")))

        val request = MockMvcRequestBuilders.post("/check/issue")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/issue/dimension-type/request.json"))

        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)

        verify(coreClient)
            .getSerialInventoryBySerialNumber("123")
        verify(coreClient)
            .getSkuCharacteristics(SkuCharacteristicsRequest(listOf(SkuId(storerKey, sku))))
        verify(coreClient)
            .getLocationByLocId("A1-01-01A1")
        verifyNoMoreInteractions(coreClient)
        verify(dimensionManagementClient, times(0))
            .createMeasurementOrderBySkuId(CreateBySkuIdRequest(storerKey, sku))
    }

    @Test
    @DatabaseSetup("/controller/check/issue/dimension-type-with-measurement-order-creation/before.xml")
    @ExpectedDatabase(
        value = "/controller/check/issue/dimension-type-with-measurement-order-creation/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createIssue with dimension type when the creation of a measurement order enabled`() {
        val sku = "ROV001"
        val storerKey = "465852"
        val serialInventory = SerialInventoryDto("123", storerKey, sku, "LOT", "LOC", "ID", BigDecimal.ONE, "" , "")
        val skuSpecification = CoreSpecification(
            sku, storerKey, "MSKU01", "Товар 1",
            listOf(
                CoreCharacteristic(SkuCharacteristicType.WIDTH, "5"),
                CoreCharacteristic(SkuCharacteristicType.HEIGHT, "2"),
                CoreCharacteristic(SkuCharacteristicType.LENGTH, "1"),
                CoreCharacteristic(SkuCharacteristicType.VOLUME, "10"),
                CoreCharacteristic(SkuCharacteristicType.WEIGHT, "15"),
            )
        )

        whenever(coreClient.getSerialInventoryBySerialNumber(any()))
            .thenReturn(GetSerialInventoryResponse(serialInventory))
        whenever(coreClient.getSkuCharacteristics(any()))
            .thenReturn(SkuCharacteristicsResponse(listOf(skuSpecification)))
        whenever(coreClient.getLocationByLocId(any()))
            .thenReturn(GetLocationByLocIdResponse(LocationDto("A1-01-01A1", LocationType.PICK, "ZONE")))

        val request = MockMvcRequestBuilders.post("/check/issue")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/issue/dimension-type/request.json"))

        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)

        verify(coreClient)
            .getSerialInventoryBySerialNumber("123")
        verify(coreClient)
            .getSkuCharacteristics(SkuCharacteristicsRequest(listOf(SkuId(storerKey, sku))))
        verify(coreClient)
            .getLocationByLocId("A1-01-01A1")
        verifyNoMoreInteractions(coreClient)
        verify(dimensionManagementClient)
            .createMeasurementOrderBySkuId(CreateBySkuIdRequest(storerKey, sku))
        verifyNoMoreInteractions(dimensionManagementClient)
    }

    @Test
    fun `createIssue - not found serial`() {
        whenever(coreClient.getSerialInventoryBySerialNumber(any()))
            .thenReturn(GetSerialInventoryResponse(null))

        val request = MockMvcRequestBuilders.post("/check/issue")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/issue/not-found-serial/request.json"))

        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(getFileContent("controller/check/issue/not-found-serial/response.json"), true)
            )
    }

    @Test
    @DatabaseSetups(DatabaseSetup("/controller/check/issue/duplicate/data.xml"))
    @ExpectedDatabase(
        value = "/controller/check/issue/duplicate/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try to create duplicate issue`() {
        val sku = "ROV001"
        val storerKey = "465852"
        val serialInventory = SerialInventoryDto("123", storerKey, sku, "LOT", "LOC", "ID", BigDecimal.ONE, "" , "")
        val skuSpecification = CoreSpecification(
            sku, storerKey, normalMsku, "Товар 1",
            listOf(
                CoreCharacteristic(SkuCharacteristicType.WIDTH, "5"),
                CoreCharacteristic(SkuCharacteristicType.HEIGHT, "2"),
                CoreCharacteristic(SkuCharacteristicType.LENGTH, "1"),
                CoreCharacteristic(SkuCharacteristicType.VOLUME, "10"),
                CoreCharacteristic(SkuCharacteristicType.WEIGHT, "15"),
            )
        )

        whenever(coreClient.getSerialInventoryBySerialNumber(any()))
            .thenReturn(GetSerialInventoryResponse(serialInventory))
        whenever(coreClient.getSkuCharacteristics(any()))
            .thenReturn(SkuCharacteristicsResponse(listOf(skuSpecification)))

        val request = MockMvcRequestBuilders.post("/check/issue")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/issue/duplicate/request.json"))

        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/issues/db.xml")
    @ExpectedDatabase(
        value = "/controller/check/loc-and-sku/issues/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `should not fail if issue exists`() {
        whenever(skuService.findSku(argThat { size == 1 && first().sku == heavySku }))
            .thenReturn(listOf(heavySkuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/issues/request.json"))

        mockMvc
            .perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(getFileContent("controller/check/loc-and-sku/issues/response.json"), true)
            )
    }

    @ParameterizedTest
    @MethodSource("rangeToSkuSpecs")
    @DatabaseSetup("/controller/check/loc-and-sku/except/rules-except.xml")
    fun `checkByLocAndSku failed with except rule group`(range: String, cargoTypes: List<String>) {
        val skuSpecs = generateSkuSpecsWithCargoTypes("ROV0000000824", "465852", cargoTypes)
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(skuSpecs))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/except/fail/$range/request.json"))

        sendRequest(request, "except/fail")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/except/rules-except.xml")
    fun `checkByLocAndSku ok with except rule group`() {
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(normalSkuInfo))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/except/ok/request.json"))

        sendRequest(request, "except/ok")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/combined/rules-combined.xml")
    fun `checkByLocAndSku failed with combined rule groups`() {
        val skuSpecs = generateSkuSpecsWithCargoTypes("ROV0000000824", "465852", listOf("750", "720"))
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(skuSpecs))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/combined/request.json"))

        sendRequest(request, "fail")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/combined/rules-combined.xml")
    fun `checkByLocAndSku - ok - with combined rule groups`() {
        val skuSpecs = generateSkuSpecsWithCargoTypes("ROV0000000824", "465852", listOf("750", "800"))
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(skuSpecs))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/combined/request.json"))

        sendRequest(request, "ok")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/rules.xml")
    fun `checkByLocAndSku - ok - 2 rule groups with ONLY collation`() {
        val skuSpecs = generateSkuSpecsWithCargoTypes("ROV0000000824", "465852", listOf("780", "720"))
        skuSpecs.specification
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(skuSpecs))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/two-rule-group/request.json"))

        sendRequest(request, "ok")
    }

    @Test
    @DatabaseSetup("/controller/check/loc-and-sku/rules.xml")
    fun `checkByLocAndSku - fail - 2 rule groups with ONLY collation`() {
        val skuSpecs = generateSkuSpecsWithCargoTypes("ROV0000000824", "465852", listOf("750", "720"))
        skuSpecs.specification
        whenever(skuService.findSku(argThat { size == 1 && first().sku == normalSku }))
            .thenReturn(listOf(skuSpecs))

        val request = MockMvcRequestBuilders
            .post("/check/loc-and-sku")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/loc-and-sku/two-rule-group/request.json"))

        sendRequest(request, "fail")
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when putawayzone is not passed`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/putawayzone-empty/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when putawayzone is null`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/putawayzone-null/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when putawayzone is blank`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/putawayzone-blank/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when sku is not passed`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/sku-empty/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when sku is null`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/sku-null/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when sku is empty list`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/sku-empty-list/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when sku element has empty sku`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/sku-sku-empty/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when sku element has null sku`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/sku-sku-null/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when sku element has blank sku`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/sku-sku-blank/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when sku element has empty storerKey`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/sku-storerKey-empty/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when sku element has null storerKey`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/sku-storerKey-null/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns bad request response when sku element has blank storerKey`() {
        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                getFileContent("controller/check/restricted-rows/sku-storerKey-blank/request.json")
            )
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/check/restricted-rows/before.xml")
    fun `getRestrictedRows returns successful response`() {
        val skuSpecs = generateSkuSpecsWithCargoTypes("ROV0000000825", "465852", listOf("780", "720"))
        skuSpecs.specification
        whenever(skuService.findSku(any()))
            .thenReturn(listOf(skuSpecs))

        whenever(coreClient.getRowsBetweenLocs("B1-01-01A1", "B1-09-01B8"))
            .thenReturn(listOf("B1-01", "B1-03", "B1-09"))
        whenever(coreClient.getRowsBetweenLocs("B1-15-01A1", "B1-18-01B8"))
            .thenReturn(listOf("B1-15", "B1-17", "B1-18"))

        val request = MockMvcRequestBuilders
            .post("/check/restricted-rows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/check/restricted-rows/success/request.json"))
        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    getFileContent("controller/check/restricted-rows/success/response.json"),
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
                    .json(getFileContent("controller/check/loc-and-sku/$case/response.json"), true)
            )
    }

    companion object {
        @JvmStatic
        private fun rangeToSkuSpecs(): Stream<Arguments> {
            return Stream.of(
                //1 cargo types
                Arguments.of("range1", listOf("700")),
                Arguments.of("range2", listOf("700")),
                Arguments.of("range3", listOf("700")),
                //2 cargo types
                Arguments.of("range1", listOf("700", "720")),
                Arguments.of("range2", listOf("700", "720")),
                Arguments.of("range3", listOf("700", "720")),
                //3 cargo types
                Arguments.of("range1", listOf("700", "720", "780")),
                Arguments.of("range2", listOf("700", "720", "780")),
                Arguments.of("range3", listOf("700", "720", "780")),
                //4 cargo types
                Arguments.of("range1", listOf("700", "720", "780", "800")),
                Arguments.of("range2", listOf("700", "720", "780", "800")),
                Arguments.of("range3", listOf("700", "720", "780", "800"))
            )
        }
    }

    /**
     * Нужен тест на несколько групп, которые должны одновременно выполняться
     */

    private fun generateSkuSpecsWithCargoTypes(
        sku: String,
        storerKey: String,
        cargoTypes: List<String>
    ): SkuSpecification = SkuSpecification(
        sku,
        storerKey,
        "1234567890",
        "Товар",
        cargoTypes.map { SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, it) }
    )
}

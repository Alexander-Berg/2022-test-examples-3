package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.isA
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.constraints.client.ConstraintsClient
import ru.yandex.market.wms.constraints.core.domain.RangeRuleCollation
import ru.yandex.market.wms.constraints.core.domain.RangeType
import ru.yandex.market.wms.constraints.core.dto.RangeDto
import ru.yandex.market.wms.constraints.core.dto.RangeGroupDto
import ru.yandex.market.wms.constraints.core.dto.RangeRuleGroupDto
import ru.yandex.market.wms.constraints.core.response.GetRangeGroupsResponse

class LocationControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @MockBean
    @Autowired
    private lateinit var constraintsClient: ConstraintsClient

    @AfterEach
    fun resetMocks() {
        reset(constraintsClient)
    }

    @Test
    @DatabaseSetup("/controller/location/get-by-loc-id/loc.xml")
    fun `getLocationByLocId returns location`() {
        testGetLocationByLocId(locId = "A1-02-01", testCase = "found")
    }

    @Test
    @DatabaseSetup("/controller/location/get-by-loc-id/loc.xml")
    fun `getLocationByLocId can't find location`() {
        testGetLocationByLocId(locId = "SOMELOC", testCase = "not-found")
    }

    @Test
    @DatabaseSetup("/controller/location/validate/loc.xml")
    fun `validateLocation successfully validates location`() {
        testValidateLocation(
            testCase = "ok",
            expectedStatus = status().isOk,
            checkResponse = false
        )
    }

    @Test
    fun `validateLocation returns not found location error when loc doesn't exist`() {
        testValidateLocation(
            testCase = "not-found",
            expectedStatus = status().isNotFound,
        )
    }

    @Test
    @DatabaseSetup("/controller/location/validate/loc.xml")
    fun `validateLocation returns invalid loc type error when location types don't match`() {
        testValidateLocation(
            testCase = "invalid-type",
            expectedStatus = status().isBadRequest,
        )
    }

    @Test
    @DatabaseSetup("/controller/location/validate-placement-buf/loc.xml")
    fun `validatePlacementBuf successfully validates location`() {
        testValidatePlacementBuf(
            testCase = "ok",
            expectedStatus = status().isOk,
            checkResponse = false
        )
    }

    @Test
    fun `validatePlacementBuf returns not found location error when loc doesn't exist`() {
        testValidatePlacementBuf(
            testCase = "not-found",
            expectedStatus = status().isNotFound,
        )
    }

    @Test
    @DatabaseSetup("/controller/location/validate-placement-buf/loc.xml")
    fun `validatePlacementBuf returns invalid loc type error when location types don't match`() {
        testValidatePlacementBuf(
            testCase = "invalid-type",
            expectedStatus = status().isBadRequest,
        )
    }

    @Test
    @DatabaseSetup("/controller/location/validate-placement-buf-conveyor/loc.xml")
    fun `validatePlacementBufWConv successfully validates location`() {
        testValidatePlacementBufWConv(
            testCase = "ok",
            expectedStatus = status().isOk,
            checkResponse = false
        )
    }

    @Test
    @DatabaseSetup("/controller/location/validate-placement-buf-conveyor/loc.xml")
    fun `validatePlacementBufWConv returns invalid loc type error when location types don't match`() {
        testValidatePlacementBufWConv(
            testCase = "invalid-type",
            expectedStatus = status().isBadRequest,
        )
    }

    @Test
    @DatabaseSetup("/controller/location/validate-placement-buf-conveyor/loc.xml")
    fun `validatePlacementBufWConv returns loc isn't connected to transported out`() {
        testValidatePlacementBufWConv(
            testCase = "not-connected",
            expectedStatus = status().isBadRequest,
        )
    }

    @Test
    @DatabaseSetup("/controller/location/validate-locs-zone/before.xml")
    fun `validateLocationsZones successfully validates`() {
        testValidateLocationsZones(
            testCase = "ok",
            expectedStatus = status().isOk,
        )
    }

    @Test
    @DatabaseSetup("/controller/location/validate-locs-zone/before.xml")
    fun `validateLocationsZones returns not found because location not exists`() {
        testValidateLocationsZones(
            testCase = "not-found",
            expectedStatus = status().isNotFound,
        )
    }

    @Test
    @DatabaseSetup("/controller/location/validate-locs-zone/before.xml")
    fun `validateLocationsZones fails validations`() {
        testValidateLocationsZones(
            testCase = "validate-fail",
            expectedStatus = status().isOk,
        )
    }

    @Test
    @DatabaseSetup("/controller/location/external-zone-name/immutable.xml")
    fun `get transit loc by external zone name`() {
        testGetTransitLoc(
            request = "/location/transit-zone/SHIPPING1",
            expectedStatus = status().isOk,
            expectedResponse = content().json(FileContentUtils.getFileContent(
                "controller/location/external-zone-name/ok/response.json"), true)
        )
    }

    @Test
    @DatabaseSetup("/controller/location/external-zone-name/immutable.xml")
    fun `get transit loc by external zone name returns not found`() {
        testGetTransitLoc(
            request = "/location/transit-zone/NON_EXISTING_ZONE",
            expectedStatus = status().isNotFound,
            expectedResponse = content().json(FileContentUtils.getFileContent(
                "controller/location/external-zone-name/not-found/response.json"), true)
        )
    }

    @Test
    @DatabaseSetup("/controller/location/update-vgh/1/initial.xml")
    @ExpectedDatabase(value = "/controller/location/update-vgh/1/expected.xml",
        assertionMode=DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `location VGH update should update only known locations`() {
        val request = FileContentUtils.getFileContent("controller/location/update-vgh/1/request.txt")
        val file = MockMultipartFile(
            "file",
            "request.txt",
            MediaType.TEXT_PLAIN_VALUE,
            request.toByteArray()
        )

        val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val requestBuilder = multipart("/location/upload/99").file(file)
        val result = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk).andReturn()

        assertEquals("1 locations was updated", result.response.contentAsString)
    }

    @Test
    @DatabaseSetup("/controller/location/update-vgh/2/initial.xml")
    @ExpectedDatabase(value = "/controller/location/update-vgh/2/expected.xml",
        assertionMode=DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `location VGH update should update only changed values`() {
        val request = FileContentUtils.getFileContent("controller/location/update-vgh/2/request.txt")
        val file = MockMultipartFile(
            "file",
            "request.txt",
            MediaType.TEXT_PLAIN_VALUE,
            request.toByteArray()
        )

        val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val requestBuilder = multipart("/location/upload/99").file(file)
        val result = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk).andReturn()

        assertEquals("0 locations was updated", result.response.contentAsString)
    }

    @Test
    @DatabaseSetup("/controller/location/update-vgh/3/initial.xml")
    @ExpectedDatabase(value = "/controller/location/update-vgh/3/expected.xml",
        assertionMode=DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `location VGH update should use first entry if locations update contains duplicates`() {
        val request = FileContentUtils.getFileContent("controller/location/update-vgh/3/request.txt")
        val file = MockMultipartFile(
            "file",
            "request.txt",
            MediaType.TEXT_PLAIN_VALUE,
            request.toByteArray()
        )

        val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val requestBuilder = multipart("/location/upload/99").file(file)
        val result = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk).andReturn()

        assertEquals("1 locations was updated", result.response.contentAsString)
    }

    @Test
    @DatabaseSetup("/controller/location/update-vgh/4/initial.xml")
    @ExpectedDatabase(value = "/controller/location/update-vgh/4/expected.xml",
        assertionMode=DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `location VGH update should be disabled if flag not set`() {
        val request = FileContentUtils.getFileContent("controller/location/update-vgh/4/request.txt")
        val file = MockMultipartFile(
            "file",
            "request.txt",
            MediaType.TEXT_PLAIN_VALUE,
            request.toByteArray()
        )

        val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val requestBuilder = multipart("/location/upload/99").file(file)
        val result = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk).andReturn()

        assertEquals("VGH update disabled", result.response.contentAsString)
    }

    @Test
    @DatabaseSetup("/controller/location/update-vgh/5/initial.xml")
    @ExpectedDatabase(value = "/controller/location/update-vgh/5/expected.xml",
        assertionMode=DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `location VGH update should not be updated if warehouse id does not match`() {
        val request = FileContentUtils.getFileContent("controller/location/update-vgh/5/request.txt")
        val file = MockMultipartFile(
            "file",
            "request.txt",
            MediaType.TEXT_PLAIN_VALUE,
            request.toByteArray()
        )

        val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val requestBuilder = multipart("/location/upload/66").file(file)
        val result = mockMvc.perform(requestBuilder)
            .andExpect(status().`is`(404)).andReturn()

        assertTrue(result.response.contentAsString
            .contains("Id Recipient Warehouse ID does not match provided value: 66 not found"))
    }

    @Test
    @DatabaseSetup("/controller/location/get-by-sku-list/before.xml")
    fun `Get locations by skus returns valid result when locations are found`() {
        testValidateLocation(
            testCase = "found",
            expectedStatus = status().isOk,
            checkResponse = true,
            url = "get-by-sku-list"
        )
    }

    @Test
    @DatabaseSetup("/controller/location/get-by-sku-list/before.xml")
    fun `Get locations by skus returns valid result when locations are not found`() {
        testValidateLocation(
            testCase = "not-found",
            expectedStatus = status().isOk,
            checkResponse = true,
            url = "get-by-sku-list"
        )
    }

    @Test
    fun `Get locations by skus returns bad request when limitPerSku is less than min`() {
        testValidateLocation(
            testCase = "limit-per-sku-lt-min",
            expectedStatus = status().isBadRequest,
            checkResponse = false,
            url = "get-by-sku-list"
        )
    }

    @Test
    fun `Get locations by skus returns bad request when skuList is empty`() {
        testValidateLocation(
            testCase = "sku-list-empty",
            expectedStatus = status().isBadRequest,
            checkResponse = false,
            url = "get-by-sku-list"
        )
    }

    @Test
    fun `Get locations by skus returns bad request when skuList is null`() {
        testValidateLocation(
            testCase = "sku-list-null",
            expectedStatus = status().isBadRequest,
            checkResponse = false,
            url = "get-by-sku-list"
        )
    }

    @Test
    @DatabaseSetup("/controller/location/visualization/before-fullness.xml")
    fun `getVisualizationInfo for full locs`() {
        whenever(constraintsClient.getRangeGroups(isA()))
            .thenReturn(GetRangeGroupsResponse(emptyList()))

        testVisualization("full-locs", "2", "3")
    }

    @Test
    @DatabaseSetup("/controller/location/visualization/before-fullness.xml")
    fun `getVisualizationInfo for multiple cells`() {
        whenever(constraintsClient.getRangeGroups(isA()))
            .thenReturn(GetRangeGroupsResponse(emptyList()))

        testVisualization("multiple-cells", "1", "1")
    }

    @Test
    @DatabaseSetup("/controller/location/visualization/before-fullness.xml")
    fun `getVisualizationInfo without z aggregation`() {
        testVisualization("without-z", "1", null)
        verifyNoInteractions(constraintsClient)
    }

    @Test
    @DatabaseSetup("/controller/location/visualization/before-fullness.xml")
    fun `getVisualizationInfo with zone filter`() {
        testVisualization("zone-filter", null, null, "MEZONIN1_2")
        verifyNoInteractions(constraintsClient)
    }

    @Test
    @DatabaseSetup("/controller/location/visualization/before-constraints.xml")
    fun `getVisualizationInfo with one suitable constraints range`() {
        val range = RangeDto(0, "A1-00-01A1", "A1-10-01A1", RangeType.CROSS, null)
        val otherTierRange = RangeDto(0, "A1-11-01A1", "A1-30-01A1", RangeType.TIER, "C")
        val rule = RangeRuleGroupDto(0, "Rule", RangeRuleCollation.ONLY)

        val rangeGroup = RangeGroupDto(0, "MEZONIN2_1", listOf(range, otherTierRange), listOf(rule))

        whenever(constraintsClient.getRangeGroups(isA()))
            .thenReturn(GetRangeGroupsResponse(listOf(rangeGroup)))

        testVisualization("one-constraints-range", "2", "1")

        verify(constraintsClient)
            .getRangeGroups(argThat { zones ->
                zones.size == 2 && zones.containsAll(listOf("MEZONIN2_1", "MEZONIN2_2"))
            })
    }

    @Test
    @DatabaseSetup("/controller/location/visualization/before-constraints.xml")
    fun `getVisualizationInfo with multiple ranges and rule`() {
        val ranges1 = listOf(
            RangeDto(0, "A1-02-01A1", "A1-03-01A1", RangeType.CROSS, null),
            RangeDto(1,  "A1-10-01A1", "A1-10-01A1", RangeType.TIER, "A")
        )
        val rules1 = listOf(RangeRuleGroupDto(0, "Rule 0", RangeRuleCollation.ONLY))
        val rangeGroup1 = RangeGroupDto(0, "MEZONIN2_1", ranges1, rules1)

        val ranges2 = listOf(RangeDto(2, "A1-22-01A1", "A1-99-01A1", RangeType.CROSS, null))
        val rules2 = listOf(
            RangeRuleGroupDto(1, "Rule 1", RangeRuleCollation.EXCEPT),
            RangeRuleGroupDto(2, "Rule 2", RangeRuleCollation.ONLY)
        )
        val rangeGroup2 = RangeGroupDto(1, "MEZONIN2_2", ranges2, rules2)

        whenever(constraintsClient.getRangeGroups(isA()))
            .thenReturn(GetRangeGroupsResponse(listOf(rangeGroup1, rangeGroup2)))

        testVisualization("multiple-range-rules", "2", "1")

        verify(constraintsClient)
            .getRangeGroups(argThat { zones ->
                zones.size == 2 && zones.containsAll(listOf("MEZONIN2_1", "MEZONIN2_2"))
            })
    }

    @Test
    @DatabaseSetup("/controller/location/visualization/before-constraints.xml")
    fun `getVisualizationInfo same rules grouped in constraints group`() {
        val ranges1 = listOf(RangeDto(0, "A0-00-01A1", "A1-03-01A1", RangeType.CROSS, null))
        val ranges2 = listOf(RangeDto(1, "A1-03-01A1", "A9-99-99A9", RangeType.CROSS, null))
        val rules = listOf(
            RangeRuleGroupDto(1, "Rule 1", RangeRuleCollation.EXCEPT),
            RangeRuleGroupDto(2, "Rule 2", RangeRuleCollation.ONLY)
        )
        val rangeGroup1 = RangeGroupDto(0, "MEZONIN2_1", ranges1, rules)
        val rangeGroup2 = RangeGroupDto(1, "MEZONIN2_2", ranges2, rules)

        whenever(constraintsClient.getRangeGroups(isA()))
            .thenReturn(GetRangeGroupsResponse(listOf(rangeGroup1, rangeGroup2)))

        testVisualization("constraints-group", "2", "1")

        verify(constraintsClient)
            .getRangeGroups(argThat { zones ->
                zones.size == 2 && zones.containsAll(listOf("MEZONIN2_1", "MEZONIN2_2"))
            })
    }

    @Test
    @DatabaseSetup("/controller/location/loc-levels/before.xml")
    fun getLocLevelValues() {
        mockMvc.perform(get("/location/loc-level/values"))
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(getResponse("loc-levels", "ok"), true))
    }

    @Test
    @DatabaseSetup("/controller/location/z-coords/before.xml")
    fun getZCoordinateValues() {
        mockMvc.perform(get("/location/loc-level/2/z-coords"))
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(getResponse("z-coords", "ok"), true))
    }

    @Test
    @DatabaseSetup("/controller/location/get-range/before.xml")
    fun `getRangeByCoordinates without z param`() {
        val request = get("/location/putaway-zone/MEZONIN_1/range-by-coordinates")
            .param("minX", "1")
            .param("minY", "1")
            .param("maxX", "9")
            .param("maxY", "9")
        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(getResponse("get-range", "without-z"), true))
    }

    @Test
    @DatabaseSetup("/controller/location/get-range/before.xml")
    fun `getRangeByCoordinates with z param`() {
        val request = get("/location/putaway-zone/MEZONIN_1/range-by-coordinates")
            .param("minX", "1")
            .param("minY", "1")
            .param("maxX", "9")
            .param("maxY", "9")
            .param("z", "2")
        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(getResponse("get-range", "with-z"), true))
    }

    @Test
    @DatabaseSetup("/controller/location/get-measure-buffers/before.xml")
    fun `getMeasurementBuffers with INBOUND putawayZone type`() {
        val request = get("/location/measure-buffers")
            .param("locationId", "DIMSTAGE_1")
            .param("zoneType", "DIMENSIONS_INBOUND")
        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(
                getResponse("get-measure-buffers", "inbound-zone-type"), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/location/get-measure-buffers/before.xml")
    fun `getMeasurementBuffers with OUTBOUND putawayZone type`() {
        val request = get("/location/measure-buffers")
            .param("locationId", "DIMSTAGE_1")
            .param("zoneType", "DIMENSIONS_OUTBOUND")
        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(
                getResponse("get-measure-buffers", "outbound-zone-type"), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/location/select-transport-buffers/immutable.xml")
    fun `selectTransportLocationsByZone found locs`() {
        val request = get("/location/transport/select")
            .param("zoneType", "PLACEMENT_KGT")
        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(
                getResponse("select-transport-buffers", "found"), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/location/select-transport-buffers/immutable.xml")
    fun `selectTransportLocationsByZone not found locs`() {
        val request = get("/location/transport/select")
            .param("zoneType", "EXPENSIVE")
        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(
                getResponse("select-transport-buffers", "not-found"), true)
            )
    }

    fun `getLocCoordinates return bad request response when loc is not passed`() {
        val request = get("/location/get-loc-coordinates")
        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getLocCoordinates return bad request response when blank loc is passed`() {
        val request = get("/location/get-loc-coordinates")
            .param("loc", "")
        mockMvc.perform(request)
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getLocCoordinates return not found response when loc is not found`() {
        val request = get("/location/get-loc-coordinates")
            .param("loc", "A1-01-01A1")
        mockMvc.perform(request)
            .andExpect(status().isNotFound)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(getResponse("get-loc-coordinates", "loc-not-found"), true))
    }

    @Test
    @DatabaseSetup("/controller/location/get-loc-coordinates/before.xml")
    fun `getLocCoordinates success`() {
        val request = get("/location/get-loc-coordinates")
            .param("loc", "A1-01-01A1")
        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(getResponse("get-loc-coordinates", "success"), true))
    }

    private fun testGetLocationByLocId(locId: String, testCase: String) {
        mockMvc.perform(get("/location/$locId"))
            .andExpect(status().isOk)
            .andExpect(content().json(getResponse("get-by-loc-id", testCase), true))
    }

    private fun testGetTransitLoc(request: String, expectedStatus: ResultMatcher, expectedResponse: ResultMatcher) {
        val requestBuilder = get(request)

        mockMvc.perform(requestBuilder)
            .andExpect(expectedStatus)
            .andExpect(expectedResponse)
    }

    private fun testValidatePlacementBuf(testCase: String, expectedStatus: ResultMatcher, checkResponse: Boolean = true) =
        testValidateLocation(testCase, expectedStatus, checkResponse, "validate-placement-buf")

    private fun testValidatePlacementBufWConv(testCase: String, expectedStatus: ResultMatcher, checkResponse: Boolean = true) =
        testValidateLocation(testCase, expectedStatus, checkResponse, "validate-placement-buf-conveyor")

    private fun testValidateLocation(testCase: String, expectedStatus: ResultMatcher, checkResponse: Boolean = true) =
        testValidateLocation(testCase, expectedStatus, checkResponse, "validate")

    private fun testValidateLocationsZones(testCase: String, expectedStatus: ResultMatcher) =
        testValidateLocation(testCase, expectedStatus, true, "validate-locs-zone")

    private fun testValidateLocation(
        testCase: String,
        expectedStatus: ResultMatcher,
        checkResponse: Boolean,
        url: String,
    ) {
        val requestBuilder = post("/location/$url")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getRequest(url, testCase))

        val resultActions = mockMvc.perform(requestBuilder)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(expectedStatus)

        if (checkResponse) {
            resultActions
                .andExpect(content().json(getResponse(url, testCase), true))
        }
    }

    private fun testVisualization(testCase: String, locLevel: String?, z: String?, zone: String? = null) {
        val request = get("/location/visualization")
        locLevel?.let { request.param("locLevel", it) }
        z?.let { request.param("z", it) }
        zone?.let { request.param("putawayZone", it) }

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json(getResponse("visualization", testCase), true))

    }

    private fun getRequest(testGroup: String, testCase: String): String =
        FileContentUtils.getFileContent("controller/location/$testGroup/$testCase/request.json")

    private fun getResponse(testGroup: String, testCase: String): String =
        FileContentUtils.getFileContent("controller/location/$testGroup/$testCase/response.json")
}

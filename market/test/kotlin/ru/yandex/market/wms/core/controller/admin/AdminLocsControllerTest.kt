package ru.yandex.market.wms.core.controller.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import junit.framework.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.columnFilters.LocFilter
import ru.yandex.market.wms.core.HttpAssert
import ru.yandex.market.wms.core.base.dto.LocTemplateParams
import ru.yandex.market.wms.core.base.request.GenerateLocsRequest
import ru.yandex.market.wms.core.dao.admin.AdminLocsDao
import ru.yandex.market.wms.core.dao.admin.querygenerator.locs.AdminLocation
import ru.yandex.market.wms.scheduler.client.SchedulerClient

class AdminLocsControllerTest: IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @MockBean
    @Autowired
    private lateinit var schedulerClient: SchedulerClient

    @SpyBean
    @Autowired
    private lateinit var dao: AdminLocsDao

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    companion object {
        private const val CHECK_NEW_LOCS_COUNT = """
            select count(*) from wmwhse1.LOC_MANAGEMENT
            where PUTAWAYZONE = :zone
        """
    }

    fun before() {
        Mockito.`when`(schedulerClient.executingStatusJobGenerate).thenReturn("complete")
        Mockito.`when`(schedulerClient.executingStatusJobUpdate).thenReturn("complete")

        Mockito.`when`(schedulerClient.executeLocCreateJob()).thenReturn("1234")
        Mockito.`when`(schedulerClient.executeLocUpdateJob()).thenReturn("1234")
    }

    @Test
    fun getLocsEmptyTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/locs"),
            responseFile = "controller/admin/locs/get-locs-empty.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_get_locs.xml")
    fun getLocsWithOrderDescTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/locs?order=DESC"),
            responseFile = "controller/admin/locs/get-locs-with-order-desc.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_get_locs.xml")
    fun getLocsWithInFilterTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/locs?filter=zoneDescription=in=('зона 1','зона 2')&sort=width"),
            responseFile = "controller/admin/locs/get-locs-with-in-filter.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_get_locs.xml")
    fun getLocsWithFilterAndSortTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/locs?filter=putawayZone=='ZONE2'&sort=width"),
            responseFile = "controller/admin/locs/get-locs-with-filter-and-sort.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_get_locs.xml")
    fun getLocsWithOffsetAndSortTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/locs?offset=2&sort=length&order=DESC"),
            responseFile = "controller/admin/locs/get-locs-with-offset-and-sort.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_get_locs.xml")
    fun getLocsWithLimitAndSortAndOrderTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/locs?limit=3&sort=length&order=DESC"),
            responseFile = "controller/admin/locs/get-locs-with-limit-and-sort-and-order.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_create_locs.xml")
    @ExpectedDatabase(
        columnFilters  = [LocFilter::class],
        value = "/controller/admin/locs/create_loc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun createLocTest() {
        before()
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/loc"),
            requestFile = "controller/admin/locs/create-loc.json",
            status = MockMvcResultMatchers.status().isCreated
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_get_locs.xml")
    fun createLocIfExistsTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/loc"),
            requestFile = "controller/admin/locs/create-existing-loc.json",
            responseFile = "controller/admin/locs/loc-is-exists.json",
            status = MockMvcResultMatchers.status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_generate_locs.xml", connection = "wmwhseConnection")
    fun generateLocsTest() {
        before()
        Mockito.doReturn(null).`when`(dao).checkLocsExistence(anyList())

        val expectedLocsCount = 4 * 4 * 5 * 6 * 3
        val zone = "зона 23"
        val request = GenerateLocsRequest(
            locTemplateParams = LocTemplateParams(
            template = "generate",
            floorFirst = "a",
            floorLast = "d",
            lineFirst= "a",
            lineLast = "d",
            sectionFirst = "a1",
            sectionLast = "c1",
            tierFirst = "a",
            tierLast = "f",
            placeFirst = "1",
            placeLast = "3"),

            locationType = "ANO_CONS",
            zoneDescription = zone,
            commingleSku = true,
            commingleLot = true,
            loseId = true,
            cubicCapacity = 10.0,
            weightCapacity = 10.0,
            length = 10.0,
            width = 10.0,
            height = 10.0,
            locationHandling = "OTHER",
            locLevel = 0,
            xCoord = 0,
            yCoord = 0,
            zCoord = 0,
            warehouseProcess = "PICKING"
        )
        val content = ObjectMapper().writeValueAsString(request)
        mockMvc.perform(MockMvcRequestBuilders.post("/admin-locs/generate?generate=true")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON))

        val actualLocsCount = jdbcTemplate?.queryForObject(CHECK_NEW_LOCS_COUNT, mapOf("zone" to "ZONE23"), Int::class.java)

        assertEquals(expectedLocsCount, actualLocsCount)
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_generate_locs.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
        columnFilters  = [LocFilter::class],
        value = "/controller/admin/locs/after_generate_locs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun generateLocsCheckDBTest() {
        before()
        Mockito.doReturn(null).`when`(dao).checkLocsExistence(anyList())

        val zone = "зона 23"
        val request = GenerateLocsRequest(
            locTemplateParams = LocTemplateParams(
            template = "generate",
            floorFirst = "a",
            floorLast = "a",
            lineFirst= "a",
            lineLast = "b",
            sectionFirst = "a1",
            sectionLast = "b2",
            tierFirst = "a",
            tierLast = "a",
            placeFirst = "1",
            placeLast = "1"),

            locationType = "ANO_CONS",
            zoneDescription = zone,
            commingleSku = true,
            commingleLot = true,
            loseId = true,
            cubicCapacity = 10.0,
            weightCapacity = 10.0,
            length = 10.0,
            width = 10.0,
            height = 10.0,
            locationHandling = "OTHER",
            locLevel = 0,
            xCoord = 0,
            yCoord = 0,
            zCoord = 0,
            warehouseProcess = "PICKING"
        )
        val content = ObjectMapper().writeValueAsString(request)
        mockMvc.perform(MockMvcRequestBuilders.post("/admin-locs/generate?generate=true")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_generate_locs.xml", connection = "wmwhseConnection")
    fun generateLocsWithBadRequestTest() {
        before()
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/generate?generate=true"),
            requestFile = "controller/admin/locs/generate-loc-bad-request.json",
            responseFile = "controller/admin/locs/generate-loc-bad-response.json",
            status = MockMvcResultMatchers.status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_generate_locs_with_limit.xml", connection = "wmwhseConnection")
    fun generateMoreThenLimitTest() {
        before()
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/generate?generate=true"),
            requestFile = "controller/admin/locs/generate-locs-more-than-limit.json",
            responseFile = "controller/admin/locs/limit-exception.json",
            status = MockMvcResultMatchers.status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_update_locs.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
        columnFilters  = [LocFilter::class],
        value = "/controller/admin/locs/after_update_locs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateLocsTest() {
        before()
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/update?update=true"),
            requestFile = "controller/admin/locs/update-locs-request.json",
            responseFile = "controller/admin/locs/update-locs-response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_update_locs.xml", connection = "wmwhseConnection")
    fun updateLocsWhenExecutingTest() {
        Mockito.`when`(schedulerClient.executingStatusJobUpdate).thenReturn("executing")
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/update?update=true"),
            requestFile = "controller/admin/locs/update-locs-request.json",
            responseFile = "controller/admin/locs/update-locs-executing-response.json",
            status = MockMvcResultMatchers.status().isBadRequest
        )
    }


    @Test
    @DatabaseSetup("/controller/admin/locs/before_update_locs.xml", connection = "wmwhseConnection")
    fun updateLocsWhenFailedTest() {
        Mockito.`when`(schedulerClient.executingStatusJobUpdate).thenReturn("failed")
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/update?update=true"),
            requestFile = "controller/admin/locs/update-locs-request.json",
            responseFile = "controller/admin/locs/update-locs-failed-response.json",
            status = MockMvcResultMatchers.status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_update_loc.xml", connection = "wmwhseConnection")
    fun updateLocWhenNotExist() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/admin-locs/loc/S10"),
            requestFile = "controller/admin/locs/update-loc-request.json",
            responseFile = "controller/admin/locs/loc-not-found.json",
            status = MockMvcResultMatchers.status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_update_loc.xml", connection = "wmwhseConnection")
    fun updateLoc() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/admin-locs/loc/S01"),
            requestFile = "controller/admin/locs/update-loc-request.json",
            status = MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_update_logical_location.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
        columnFilters  = [LocFilter::class],
        value = "/controller/admin/locs/after_snake_update_logical_location.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateLogicalLocationWithSnakeType() {
        before()
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/update/logical-location?update=true"),
            requestFile = "controller/admin/locs/update-logical-location-snake-request.json",
            status = MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/locs/before_update_logical_location.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
        columnFilters  = [LocFilter::class],
        value = "/controller/admin/locs/after_straight_update_logical_location.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateLogicalLocationWithStraightType() {
        before()
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/update/logical-location?update=true"),
            requestFile = "controller/admin/locs/update-logical-location-straight-request.json",
            status = MockMvcResultMatchers.status().isOk
        )
    }
}

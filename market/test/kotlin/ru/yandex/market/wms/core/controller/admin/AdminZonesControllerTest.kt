package ru.yandex.market.wms.core.controller.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.core.HttpAssert

class AdminZonesControllerTest: IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    fun getZonesDescriptionTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-zones/zones/descr"),
            responseFile = "controller/admin/zones/get-zones-descr.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    fun getZonesWithOrder() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-zones/zones?order=DESC"),
            responseFile = "controller/admin/zones/get-zones-with-order.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    fun getZonesWithFilterTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-zones/zones?filter=pickToLoc==bcc"),
            responseFile = "controller/admin/zones/get-zones-with-filter.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    fun getZonesWithOffsetTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-zones/zones?offset=2&sort=pickToLoc&order=DESC"),
            responseFile = "controller/admin/zones/get-zones-with-offset.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    fun getZonesWithLimitTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-zones/zones?limit=3&sort=pickToLoc"),
            responseFile = "controller/admin/zones/get-zones-with-limit.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    fun getZoneTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-zones/zone?zone=ZONE1"),
            responseFile = "controller/admin/zones/get-zone.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    fun getZonesByTypeTest() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin-zones/zones-by-type?type=INB_BUF"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(
                FileContentUtils.getFileContent("controller/admin/zones/get-zones-by-type.json"),
                true))
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    @ExpectedDatabase(
        value = "/controller/admin/zones/create_zone.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createZoneTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-zones/zone"),
            requestFile = "controller/admin/zones/create-zone.json",
            status = MockMvcResultMatchers.status().isCreated
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    fun createZoneIfExistsTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-zones/zone"),
            requestFile = "controller/admin/zones/create-existing-zone.json",
            responseFile = "controller/admin/zones/zone-is-exist.json",
            status = MockMvcResultMatchers.status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    @ExpectedDatabase(
        value = "/controller/admin/zones/update_zone.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateZoneTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/admin-zones/zone/ZONE6"),
            requestFile = "controller/admin/zones/update-zone-request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/zones/before_get_zones.xml")
    fun updateZoneWhenNotFoundTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/admin-zones/zone/ZONE100"),
            requestFile = "controller/admin/zones/update-zone-request.json",
            responseFile = "controller/admin/zones/zone-not-found.json",
            status = MockMvcResultMatchers.status().isNotFound
        )
    }

    @Test
    fun getPickToLocs() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-zones/locs/types"),
            responseFile = "controller/admin/zones/loc-types.json",
            compareMode =  JSONCompareMode.NON_EXTENSIBLE
        )
    }
}


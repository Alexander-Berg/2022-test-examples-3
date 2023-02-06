package ru.yandex.market.wms.core.controller.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.HttpAssert

class AdminAreasControllerTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    fun getAreasEmptyTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-areas/areas"),
            responseFile = "controller/admin/areas/get-areas-empty.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    fun getAreasWithOrderTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-areas/areas?order=DESC"),
            responseFile = "controller/admin/areas/get-areas-with-order.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    fun getAreasWithFilterTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-areas/areas?order=DESC&filter=zones=='%ZONE1%'"),
            responseFile = "controller/admin/areas/get-areas-with-filter.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    fun getAreasWithLimitTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-areas/areas?limit=2"),
            responseFile = "controller/admin/areas/get-areas-with-limit.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    fun getAreasWithOffsetTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-areas/areas?offset=1"),
            responseFile = "controller/admin/areas/get-areas-with-offset.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    @ExpectedDatabase(
        value = "/controller/admin/areas/create_area_with_zones.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAreaWithZones() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-areas/area"),
            status = MockMvcResultMatchers.status().isCreated,
            requestFile = "controller/admin/areas/create-area-with-zones-request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    @ExpectedDatabase(
        value = "/controller/admin/areas/create_area_without_zones.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAreaWithoutZones() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-areas/area"),
            status = MockMvcResultMatchers.status().isCreated,
            requestFile = "controller/admin/areas/create-area-without-zones-request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    fun createAreaWhenZonesNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-areas/area"),
            status = MockMvcResultMatchers.status().isNotFound,
            requestFile = "controller/admin/areas/create-area-when-zones-not-found-request.json",
            responseFile = "controller/admin/areas/create-area-when-zones-not-found-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    fun createAreaWhenExists() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-areas/area"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/admin/areas/create-area-when-exists-request.json",
            responseFile = "controller/admin/areas/create-area-when-exists-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    @ExpectedDatabase(
        value = "/controller/admin/areas/delete_areas.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteAreas() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-areas/delete-areas"),
            requestFile = "controller/admin/areas/delete-areas-request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    fun updateAreaWhenNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/admin-areas/area/AREA100"),
            requestFile = "controller/admin/areas/update-area-request.json",
            responseFile = "controller/admin/areas/update-area-when-not-found-response.json",
            status = MockMvcResultMatchers.status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/areas/before_get_areas.xml")
    @ExpectedDatabase(
        value = "/controller/admin/areas/update_area.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateArea() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/admin-areas/area/AREA1"),
            requestFile = "controller/admin/areas/update-area-request.json",
        )
    }
}

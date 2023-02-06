package ru.yandex.market.wms.core.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.core.base.request.ValidateIdRequest
import ru.yandex.market.wms.core.base.response.GetMostPopulatedZoneResponse
import ru.yandex.market.wms.core.fromJson

class IdControllerTest : IntegrationTest() {
    private val mapper = ObjectMapper()
        .registerKotlinModule()

    @Test
    @DatabaseSetup("/controller/id/serial-inventories/immutable.xml")
    @ExpectedDatabase(
        "/controller/id/serial-inventories/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Get serial inventories by id`() {
        getWithAllChecks(
            urlPart = "serial-inventories",
            params = mapOf("id" to "PLT001"),
            expectedStatus = status().isOk,
            testResourceDir = "ok"
        )
    }

    @Test
    fun `Id not exists`() {
        validateIdWithAllChecks(status().isNotFound, "not-found")
    }

    @Test
    @DatabaseSetup("/controller/id/validate/empty/before.xml")
    fun `Id is empty`() {
        validateIdWithAllChecks(status().isBadRequest, "empty")
    }

    @Test
    @DatabaseSetup("/controller/id/validate/picked/before.xml")
    fun `Id has picked goods`() {
        validateIdWithAllChecks(status().isBadRequest, "picked")
    }

    @Test
    @DatabaseSetup("/controller/id/validate/ok/before.xml")
    fun `Validation is OK`() {
        validateIdWithAllChecks(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/id/fake-uit-exists/fake-uit-exists/before.xml")
    fun `Fake UIT exists method returns true when there is at least one fake UIT in given Ids`() {
        postWithAllChecks("fake-uit-exists", status().isOk, "fake-uit-exists")
    }

    @Test
    @DatabaseSetup("/controller/id/fake-uit-exists/fake-uit-not-exists/before.xml")
    fun `Fake UIT exists method returns false when there is no any fake UITs in given Ids`() {
        postWithAllChecks("fake-uit-exists", status().isOk, "fake-uit-not-exists")
    }

    @Test
    @DatabaseSetup("/controller/id/fake-uit-exists/no-uits/before.xml")
    fun `Fake UIT exists method returns false when there is no UITs in given Ids`() {
        postWithAllChecks("fake-uit-exists", status().isOk, "no-uits")
    }

    @Test
    fun `Fake UIT exists method returns error when there is null Ids given`() {
        postWithAllChecks("fake-uit-exists", status().isBadRequest, "null-ids", false)
    }

    @Test
    fun `Fake UIT exists method returns error when there is empty Ids given`() {
        postWithAllChecks("fake-uit-exists", status().isBadRequest, "empty-ids", false)
    }

    @Test
    fun `Fake UIT exists method returns error when there is empty request given`() {
        postWithAllChecks("fake-uit-exists", status().isBadRequest, "empty-request", false)
    }

    @Test
    fun `Move serials from Id to Id method returns error when there is null newId given`() =
        postWithAllChecks(
            "move-serials-from-id-to-id",
            status().isBadRequest,
            "null-new-id",
            false
        )

    @Test
    fun `Move serials from Id to Id method returns error when there is null serialNumbers given`() =
        postWithAllChecks(
            "move-serials-from-id-to-id",
            status().isBadRequest,
            "null-serial-numbers",
            false
        )

    @Test
    fun `Move serials from Id to Id method returns error when there is empty serialNumbers given`() =
        postWithAllChecks(
            "move-serials-from-id-to-id",
            status().isBadRequest,
            "empty-serial-numbers",
            false
        )

    @Test
    fun `Move serials from Id to Id method returns error when there is empty request given`() =
        postWithAllChecks(
            "move-serials-from-id-to-id",
            status().isBadRequest,
            "empty-request",
            false
        )

    @Test
    @DatabaseSetup("/controller/id/move-serials-from-id-to-id/serial-inventories-not-found/before.xml")
    @ExpectedDatabase("/controller/id/move-serials-from-id-to-id/serial-inventories-not-found/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move serials from Id to Id method returns error when there is no serial inventories for given request`() =
        postWithAllChecks(
            "move-serials-from-id-to-id",
            status().isNotFound,
            "serial-inventories-not-found",
            true
        )

    @Test
    @DatabaseSetup("/controller/id/move-serials-from-id-to-id/id-not-found/before.xml")
    @ExpectedDatabase("/controller/id/move-serials-from-id-to-id/id-not-found/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move serials from Id to Id method throws exception cos ID doesn't exist`() {
        postWithAllChecks(
            "move-serials-from-id-to-id",
            status().isNotFound,
            "id-not-found",
            true
        )
    }

    @Test
    @DatabaseSetup("/controller/id/move-serials-from-id-to-id/successful/before.xml")
    @ExpectedDatabase("/controller/id/move-serials-from-id-to-id/successful/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move serials from Id to Id method moves serials`() =
        postWithAllChecks(
            "move-serials-from-id-to-id",
            status().isOk,
            "successful",
            false
        )

    @Test
    @DatabaseSetup("/controller/id/move-id-to-loc/successful/before.xml")
    @ExpectedDatabase("/controller/id/move-id-to-loc/successful/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move id to location method moves id with serials to location`() =
        postWithAllChecks(
            "move-id-to-loc",
            status().isOk,
            "successful",
            false
        )

    @Test
    @DatabaseSetup("/controller/id/move-id-to-loc/tote-without-serials/before.xml")
    @ExpectedDatabase("/controller/id/move-id-to-loc/tote-without-serials/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move id to location method moves empty tote to location`() =
        postWithAllChecks(
            "move-id-to-loc",
            status().isOk,
            "tote-without-serials",
            false
        )

    @Test
    @DatabaseSetup("/controller/id/move-id-to-loc/id-without-serials/before.xml")
    @ExpectedDatabase("/controller/id/move-id-to-loc/id-without-serials/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Move id to location method moves non tote with filling status empty to location`() =
        postWithAllChecks(
            "move-id-to-loc",
            status().isOk,
            "id-without-serials",
            false
        )

    @Test
    @DatabaseSetup("/controller/id/check-serial-or-get-by-id/before.xml")
    @ExpectedDatabase("/controller/id/check-serial-or-get-by-id/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Check serial or get by id exists serial`() =
        getWithAllChecks(
            "check-serial-or-get-by-id",
            mapOf("serialNumberOrId" to "100001"),
            status().isOk,
            "by-serial"
        )

    @Test
    @DatabaseSetup("/controller/id/check-serial-or-get-by-id/before.xml")
    @ExpectedDatabase("/controller/id/check-serial-or-get-by-id/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Check serial or get by id exist by id`() =
        getWithAllChecks(
            "check-serial-or-get-by-id",
            mapOf("serialNumberOrId" to "RCP001"),
            status().isOk,
            "by-id"
        )

    @Test
    @DatabaseSetup("/controller/id/check-serial-or-get-by-id/before.xml")
    @ExpectedDatabase("/controller/id/check-serial-or-get-by-id/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Check serial or get by id exist error`() =
        getWithAllChecks(
            "check-serial-or-get-by-id",
            mapOf("serialNumberOrId" to "wrong sn"),
            status().isNotFound,
            "not-found"
        )

    @Test
    @DatabaseSetup("/controller/id/check-serial-or-get-by-id/before.xml")
    @ExpectedDatabase("/controller/id/check-serial-or-get-by-id/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Check serial or get by id picked error`() =
        getWithAllChecks(
            "check-serial-or-get-by-id",
            mapOf("serialNumberOrId" to "100004"),
            status().isBadRequest,
            "picked"
        )

    @Test
    @DatabaseSetup("/controller/id/check-serial/before.xml")
    @ExpectedDatabase("/controller/id/check-serial/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Check serial exist ok`() =
        getWithAllChecks(
            "check-serial",
            mapOf("serialNumber" to "100001"),
            status().isOk
        )

    @Test
    @DatabaseSetup("/controller/id/check-serial/before.xml")
    @ExpectedDatabase("/controller/id/check-serial/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Check serial exist error`() =
        getWithAllChecks(
            "check-serial",
            mapOf("serialNumber" to "wrong sn"),
            status().isNotFound,
            "not-found"
        )

    @Test
    @DatabaseSetup("/controller/id/check-serial/pick-location-w-defect-zone/before.xml")
    @ExpectedDatabase( "/controller/id/check-serial/pick-location-w-defect-zone/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Check serial in pick location error`() =
        getWithAllChecks(
            "check-serial",
            mapOf("serialNumber" to "100002"),
            status().isBadRequest,
            "pick-location"
        )

    @Test
    @DatabaseSetup("/controller/id/check-serial/pick-location-w-defect-zone/before.xml")
    @ExpectedDatabase( "/controller/id/check-serial/pick-location-w-defect-zone/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Check serial in pick location with defect zone`() =
        getWithAllChecks(
            "check-serial",
            mapOf("serialNumber" to "100003"),
            status().isOk
        )

//    @Test
//    @DatabaseSetup("/controller/id/check-serial-or-get-by-id/before.xml")
//    @ExpectedDatabase("/controller/id/check-serial-or-get-by-id/before.xml",
//        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
//    fun `Check serial or get by id - serial in pick location error`() =
//        getWithAllChecks(
//            "check-serial-or-get-by-id",
//            mapOf("serialNumberOrId" to "100005"),
//            status().isBadRequest,
//            "pick-location"
//        )
//
//    @Test
//    @DatabaseSetup("/controller/id/check-serial-or-get-by-id/before.xml")
//    @ExpectedDatabase("/controller/id/check-serial-or-get-by-id/before.xml",
//        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
//    fun `Check serial or get by id - id in pick location error`() =
//        getWithAllChecks(
//            "check-serial-or-get-by-id",
//            mapOf("serialNumberOrId" to "RCP002"),
//            status().isBadRequest,
//            "pick-location"
//        )

    @Test
    @DatabaseSetup("/controller/id/qty-allocated-or-picked/get/before.xml")
    @ExpectedDatabase(
        "/controller/id/qty-allocated-or-picked/get/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Get ids with qty allocated or picked`() {
        val ids = listOf("ID0", "ID1", "ID2", "ID3").joinToString(",")
        getWithAllChecks(
            "qty-allocated-or-picked",
            mapOf("ids" to ids),
            status().isOk,
            "get",
            strict = false
        )
    }

    @Test
    @DatabaseSetup("/controller/id/clear/before.xml")
    @ExpectedDatabase("/controller/id/clear/clear-nesting/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Clear nesting ok`() =
        postWithAllChecks(
            "clear",
            status().isOk,
            "clear-nesting",
            true
        )

    @Test
    @DatabaseSetup("/controller/id/clear/before.xml")
    @ExpectedDatabase("/controller/id/clear/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Clear nesting mask false`() =
        postWithAllChecks(
            "clear",
            status().isOk,
            "clear-nesting-mask-false",
            true
        )

    @Test
    @DatabaseSetup("/controller/id/clear/before.xml")
    @ExpectedDatabase("/controller/id/clear/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Clear serials reserved error`() =
        postWithAllChecks(
            "clear",
            status().isBadRequest,
            "clear-serials-reserved",
            true
        )

    @Test
    @DatabaseSetup("/controller/id/clear/before.xml")
    @ExpectedDatabase("/controller/id/clear/clear-serials/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Clear serials ok`() =
        postWithAllChecks(
            "clear",
            status().isOk,
            "clear-serials",
            true
        )

    @Test
    fun `Is id exists returns bad request response when id is null`() {
        getWithAllChecks(
            "is-id-exists",
            emptyMap(),
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/id/is-id-exists/id-exists/before.xml")
    fun `Is id exists returns true response when id exists`() {
        getWithAllChecks(
            "is-id-exists",
            mapOf("id" to "ID0"),
            status().isOk,
            "id-exists"
        )
    }

    @Test
    fun `Is id exists returns false response when id not exists`() {
        getWithAllChecks(
            "is-id-exists",
            mapOf("id" to "ID0"),
            status().isOk,
            "id-not-exists"
        )
    }

    @Test
    @DatabaseSetup("/controller/id/new/not-exists/before.xml")
    @ExpectedDatabase("/controller/id/new/not-exists/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Create id if not exists ok`() =
        postWithAllChecks(
            "new",
            status().isCreated,
            "not-exists",
            false
        )

    @Test
    @DatabaseSetup("/controller/id/new/exists/before.xml")
    @ExpectedDatabase("/controller/id/new/exists/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Create id if not exists but it exists`() =
        postWithAllChecks(
            "new",
            status().isOk,
            "not-exists",
            false
        )

    @Test
    @DatabaseSetup("/controller/id/update-filling-status/ok/before.xml")
    @ExpectedDatabase("/controller/id/update-filling-status/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Update id's filling status ok`() =
        postWithAllChecks(
            "update-filling-status",
            status().isOk,
            "ok",
            false
        )

    @Test
    @DatabaseSetup("/controller/id/get-id-info/ok/before.xml")
    fun `get id info ok`() {
        val requestBuilder = MockMvcRequestBuilders.get("/id/TM00001/info")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils
                .getFileContent("controller/id/get-id-info/ok/response.json")))
    }

    @Test
    @DatabaseSetup("/controller/id/get-id-info/not-found/before.xml")
    fun `get id info not found`() {
        val requestBuilder = MockMvcRequestBuilders.get("/id/TM00002/info")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/id/get-id-info/not-found/response.json")))
    }

    @Test
    @DatabaseSetup("/controller/id/get-most-populated-zone/before.xml")
    fun `Get most populated zone by container id`() {
        val requestBuilder = MockMvcRequestBuilders.get("/id/RCP02/most-populated-zone")
            .contentType(MediaType.APPLICATION_JSON)

        val response = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.fromJson<GetMostPopulatedZoneResponse>()

        assertEquals("Z2", response.zone)
    }

    @Test
    @DatabaseSetup("/controller/id/get-most-populated-zone/before.xml")
    fun `Get most populated zone by container id with child id`() {
        val requestBuilder = MockMvcRequestBuilders.get("/id/TM01/most-populated-zone")
            .contentType(MediaType.APPLICATION_JSON)

        val response = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.fromJson<GetMostPopulatedZoneResponse>()

        assertEquals("Z2", response.zone)
    }

    @Test
    @DatabaseSetup("/controller/id/validate-or-create-parent-measure-id/id-not-exists/before.xml")
    @ExpectedDatabase("/controller/id/validate-or-create-parent-measure-id/id-not-exists/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Measure CART not exists`() {
        postWithAllChecks(
            "validate-or-create-parent-measure-id",
            status().isCreated,
            "id-not-exists",
            false
        )
    }

    @Test
    @DatabaseSetup("/controller/id/validate-or-create-parent-measure-id/id-not-cart/before.xml")
    fun `Measure parent container not CART`() {
        postWithAllChecks(
            "validate-or-create-parent-measure-id",
            status().isBadRequest,
            "id-not-cart",
            true
        )
    }

    @Test
    @DatabaseSetup("/controller/id/validate-or-create-parent-measure-id/ok/before.xml")
    fun `Measure parent ok`() {
        postWithAllChecks(
            "validate-or-create-parent-measure-id",
            status().isOk,
            "ok",
            false
        )
    }

    private fun validateIdWithAllChecks(expectedStatus: ResultMatcher, responseJsonDir: String? = null, id: String = "PLT100") {
        val requestBuilder = MockMvcRequestBuilders.post("/id/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(ValidateIdRequest(id, checkQty = true, checkIfEmpty = true)))

        val result = mockMvc.perform(requestBuilder)
            .andExpect(expectedStatus)
        if (responseJsonDir != null) {
            result.andExpect(MockMvcResultMatchers.content().json(FileContentUtils
                .getFileContent("controller/id/validate/$responseJsonDir/response.json"), true))
        }
    }

    private fun postWithAllChecks(
        urlPart: String,
        expectedStatus: ResultMatcher,
        testResourceDir: String,
        checkResponse: Boolean = true,
    ) {
        val requestBuilder = MockMvcRequestBuilders.post("/id/$urlPart")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("controller/id/$urlPart/$testResourceDir/request.json"))

        val result = mockMvc.perform(requestBuilder)
            .andExpect(expectedStatus)

        if (checkResponse) {
            result.andExpect(MockMvcResultMatchers.content().json(FileContentUtils
                .getFileContent("controller/id/$urlPart/$testResourceDir/response.json"), true))
        }
    }

    private fun getWithAllChecks(
        urlPart: String,
        params: Map<String, String>,
        expectedStatus: ResultMatcher,
        testResourceDir: String? = null,
        strict: Boolean = true
    ) {
        val paramsMVM = LinkedMultiValueMap<String, String>()
        paramsMVM.setAll(params)

        val requestBuilder = MockMvcRequestBuilders.get("/id/$urlPart")
            .params(paramsMVM)
            .contentType(MediaType.APPLICATION_JSON)

        val result = mockMvc.perform(requestBuilder)
            .andExpect(expectedStatus)

        if (testResourceDir != null) {
            result.andExpect(MockMvcResultMatchers.content().json(FileContentUtils
                .getFileContent("controller/id/$urlPart/$testResourceDir/response.json"), strict))
        }
    }
}

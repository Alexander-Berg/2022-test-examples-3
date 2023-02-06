package ru.yandex.market.wms.core.controller.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import ru.yandex.market.wms.common.model.enums.AuthenticationParam
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils
import ru.yandex.market.wms.core.base.dto.ZoneConfigDto
import ru.yandex.market.wms.core.fromJson
import ru.yandex.market.wms.shared.libs.querygenerator.CursorPage
import java.nio.charset.StandardCharsets
import javax.servlet.http.Cookie

class ConveyorAdminControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/update-success-before.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/update-success-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun shouldSuccessfullyUpdateZones() {
        callApi(
            "controller/conveyor-admin/zoneconfig/update-success-request.json",
            "/admin/zones",
            null
        )
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/update-success-before.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/update-one-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun updateOneZone() {
        callApi(
            "controller/conveyor-admin/zoneconfig/update-one-request.json",
            "/admin/zones",
            null
        )
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/update-success-before.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/add-one-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun addOneZone() {
        callApi(
            "controller/conveyor-admin/zoneconfig/add-one-request.json",
            "/admin/zones",
            null
        )
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/exchange-default-before.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/exchange-default-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun exchangeDefaultZone() {
        callApi(
            "controller/conveyor-admin/zoneconfig/exchange-default-request.json",
            "/admin/zones",
            null
        )
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/the-only-default-zone.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/the-only-default-zone.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun shouldExistTheOnlyDefaultZone() {
        val response = callApiWithStatus(
            "controller/conveyor-admin/zoneconfig/the-only-default-zone-request.json",
            "/admin/zones",
            400,
            null
        )
        val content = response.getContentAsString(StandardCharsets.UTF_8)
        assertThat(content).contains("Допускается только одна зона по умолчанию")
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/change-editdate-before.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/change-editdate-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun noChanges() {
        callApi(
            "controller/conveyor-admin/zoneconfig/change-editdate-request.json",
            "/admin/zones",
            null
        )
    }

    @Test
    @Throws(Exception::class)
    fun checkMaxCongestionPercentConstraint() {
        val response = callApiWithStatus(
            "controller/conveyor-admin/zoneconfig/congestion-over-100-request.json",
            "/admin/zones",
            400,
            null
        )
        val content = response.getContentAsString(StandardCharsets.UTF_8)
        assertThat(content).contains("must be less than or equal to 100")
    }

    @Test
    @Throws(Exception::class)
    fun `should not convert float to int`() {
        val response = callApiWithStatus(
            "controller/conveyor-admin/zoneconfig/congestion-float-request.json",
            "/admin/zones",
            400,
            null
        )
        val content = response.getContentAsString(StandardCharsets.UTF_8)
        assertThat(content).contains("JSON parse error: Cannot coerce Floating-point value (22.8) " +
                "to `java.lang.Integer` value")
    }

    @Test
    @Throws(Exception::class)
    fun processEmptyRequest() {
        val response = callApiWithStatus(
            "controller/conveyor-admin/zoneconfig/null-zones-request.json",
            "/admin/zones",
            400,
            null
        )
        val content = response.getContentAsString(StandardCharsets.UTF_8)
        assertThat(content).contains("value failed for JSON property content due to missing")
    }

    @Test
    @Throws(Exception::class)
    fun processEmptyZones() {
        val response = callApiWithStatus(
            "controller/conveyor-admin/zoneconfig/empty-zones-request.json",
            "/admin/zones",
            400,
            null
        )
        val content = response.getContentAsString(StandardCharsets.UTF_8)
        assertThat(content).contains(
            "Field error in object 'updateZoneConfigsRequest' on field 'content': rejected value [[]]"
        )
    }

    @Test
    @Throws(Exception::class)
    fun processNullRequest() {
        val response = callApiWithStatus(
            "controller/conveyor-admin/zoneconfig/null-field-request.json",
            "/admin/zones",
            400,
            null
        )
        val content = response.getContentAsString(StandardCharsets.UTF_8)
        assertThat(content).contains("Missing required creator property 'enabled'")
    }

    @Test
    @Throws(Exception::class)
    fun wrongTypeRequest() {
        val response = callApiWithStatus(
            "controller/conveyor-admin/zoneconfig/wrong-type-request.json",
            "/admin/zones",
            400,
            null
        )
        val content = response.getContentAsString(StandardCharsets.UTF_8)
        assertThat(content).contains(
            "String \"FAKE\": not one of the values accepted for Enum class"
        )
    }

    @Test
    @Throws(Exception::class)
    fun duplicateNamesRequest() {
        val response = callApiWithStatus(
            "controller/conveyor-admin/zoneconfig/duplicate-names-request.json",
            "/admin/zones",
            400,
            null
        )
        val content = response.getContentAsString(StandardCharsets.UTF_8)
        assertThat(content).contains(
            "Зоны указаны более одного раза: zone-0-duplicated"
        )
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/get-zones.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/get-zones.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun findAllWithCursor() {
        val limit = 3

        var response = mockMvc.perform(
            // умышленно пустые параметры, так как реализация на фронте делает такой запрос. бэк не должен падать
            get("/admin/zones?cursor=&filter=")
                .param("limit", "$limit")
        )
            .andExpect(
                content().json(
                    getFileContent("controller/conveyor-admin/zoneconfig/find-all-1-response.json"), true
                )
            )
            .andReturn().response.contentAsString.fromJson<CursorPage<ZoneConfigDto>>()
        var cursor = response.cursor

        response = mockMvc.perform(
            get("/admin/zones")
                .param("cursor", cursor)
                .param("limit", "$limit")
        )
            .andExpect(
                content().json(
                    getFileContent("controller/conveyor-admin/zoneconfig/find-all-2-response.json"), true
                )
            )
            .andReturn().response.contentAsString.fromJson()
        cursor = response.cursor

        mockMvc.perform(
            get("/admin/zones")
                .param("cursor", cursor)
                .param("limit", "$limit")
        )
            .andExpect(
                content().json(
                    getFileContent("controller/conveyor-admin/zoneconfig/find-all-3-response.json"), true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/get-zones.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/get-zones.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun findOnlyEnabled() {
        mockMvc.perform(
            get("/admin/zones")
                .param("limit", "5")
                .param("filter", "enabled==true")
        )
            .andExpect(
                content().json(
                    getFileContent("controller/conveyor-admin/zoneconfig/find-enabled-response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/get-zones.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/get-zones.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    @Disabled("Фильтр по типу зоны не поддерживаетсян а данный момент")
    fun findOnlyExpensive() {
        mockMvc.perform(
            get("/admin/zones")
                .param("filter", "expensive==true")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                content().json(
                    getFileContent("controller/conveyor-admin/zoneconfig/find-expensive-response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/get-zones.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/get-zones.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun findZoneByText() {
        mockMvc.perform(
            get("/admin/zones")
                .param("filter", "zone==%def%")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                content().json(
                    getFileContent("controller/conveyor-admin/zoneconfig/find-zone-by-text-response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/conveyor-admin/zoneconfig/get-zones.xml")
    @ExpectedDatabase(
        value = "/controller/conveyor-admin/zoneconfig/get-zones.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Throws(Exception::class)
    fun findNothing() {
        mockMvc.perform(
            get("/admin/zones")
                .param("filter", "zone==somearea")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                content().json(
                    getFileContent("controller/conveyor-admin/zoneconfig/find-nothing-response.json"),
                    true
                )
            )
    }

    @Throws(Exception::class)
    private fun callApi(filePath: String, apiUrl: String, responseFile: String?): MockHttpServletResponse {
        return callApiWithStatus(filePath, apiUrl, 200, responseFile)
    }

    @Throws(Exception::class)
    private fun callApiWithStatus(
        filePath: String,
        apiUrl: String,
        status: Int,
        responseFile: String?
    ): MockHttpServletResponse {
        val mvcResult = mockMvc.perform(
            post(apiUrl)
                .cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(filePath).replace("\n", ""))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(status))
            .andReturn()
        val response = mvcResult.response
        if (responseFile != null) {
            JsonAssertUtils.assertFileEquals(
                responseFile,
                response.getContentAsString(StandardCharsets.UTF_8),
                JSONCompareMode.NON_EXTENSIBLE
            )
        }
        return response
    }
}

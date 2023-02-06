package ru.yandex.market.wms.core.controller.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.HttpAssert
import ru.yandex.market.wms.core.service.admin.AdminLocTemplateService

class AdminLocTemplateControllerTest: IntegrationTest() {
    private val httpAssert = HttpAssert { mockMvc }

    @Autowired
    @SpyBean
    private val service : AdminLocTemplateService? = null

    @Test
    fun getLocsEmptyTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/template"),
            responseFile = "controller/admin/templates/get-template-empty.json"
        )
    }

    @DatabaseSetup("/controller/admin/templates/before_get_templates.xml")
    @Test
    fun getLocTemplatesTableTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/template"),
            responseFile = "controller/admin/templates/get-templates.json"
        )
    }

    @DatabaseSetup("/controller/admin/templates/before_get_templates.xml")
    @Test
    fun getLocTemplatesTableWithOffsetTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/template?offset=2&order=DESC"),
            responseFile = "controller/admin/templates/get-templates-with-offset.json"
        )
    }

    @DatabaseSetup("/controller/admin/templates/before_get_templates.xml")
    @Test
    fun getLocTemplatesTableWithFilterTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/admin-locs/template?filter=template==template1"),
            responseFile = "controller/admin/templates/get-templates-with-filter.json"
        )
    }

    @DatabaseSetup("/controller/admin/templates/before_get_templates.xml")
    @ExpectedDatabase(
        value = "/controller/admin/templates/after_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun deleteTemplateTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.delete("/admin-locs/template/template1"),
        )
    }

    @DatabaseSetup("/controller/admin/templates/before_get_templates.xml")
    @ExpectedDatabase(
        value = "/controller/admin/templates/after_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun createTemplateTest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/admin-locs/template"),
            requestFile = "controller/admin/templates/create-template-request.json",
            status = MockMvcResultMatchers.status().isCreated
        )
    }
}

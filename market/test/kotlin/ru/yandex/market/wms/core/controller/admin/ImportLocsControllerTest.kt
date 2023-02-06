package ru.yandex.market.wms.core.controller.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.Assert
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.common.spring.utils.columnFilters.LocImportFilter
import ru.yandex.market.wms.core.HttpAssert

class ImportLocsControllerTest: IntegrationTest() {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/admin/import/before_get_import_locs.xml")
    fun getLocsImportHistoryTable() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/import-locs/history"),
            responseFile = "controller/admin/import/get-import-locs-history.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/import/before_get_import_locs.xml")
    fun getLocsImportHistoryStatus() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/import-locs/history/locs"),
            responseFile = "controller/admin/import/get-import-locs-status.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/import/before_get_import_locs.xml")
    @ExpectedDatabase(value = "/controller/admin/import/after_import_locs.xml",
        assertionMode= DatabaseAssertionMode.NON_STRICT_UNORDERED,
        columnFilters  = [LocImportFilter::class],
    )
    fun importFile() {
        val request = FileContentUtils.getFileContent("controller/admin/import/MEZONIN.txt")
        val file = MockMultipartFile(
            "file",
            "MEZONIN_5.csv",
            MediaType.TEXT_PLAIN_VALUE,
            request.toByteArray()
        )

        val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val requestBuilder = MockMvcRequestBuilders.multipart("/admin-locs/upload?descr=qwer").file(file)

        val result = mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()

        Assert.assertEquals(FileContentUtils.getFileContent("controller/admin/import/import-response.json"),
            result.response.contentAsString)
    }
}

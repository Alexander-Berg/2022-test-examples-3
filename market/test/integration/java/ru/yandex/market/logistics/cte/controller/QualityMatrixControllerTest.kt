package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest
import ru.yandex.market.logistics.cte.repo.QualityMatrixGroupAttrInclusionEntityRepository
import java.io.File
import java.io.IOException
import java.util.Objects.requireNonNull

class QualityMatrixControllerTest(
    @Autowired private val repository: QualityMatrixGroupAttrInclusionEntityRepository
) : MvcIntegrationTest() {

    @Test
    fun badRequestTest() {
        upload(
            getFile("controller/bad-request/file.csv"),
            "/quality_matrix/upload-group/10/matrix-type/FULFILLMENT", HttpStatus.BAD_REQUEST
        )

        upload(
            getFile("controller/bad-request/wrong_format.myext"),
            "/quality_matrix/upload-group/10/matrix-type/FULFILLMENT", HttpStatus.BAD_REQUEST
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/multiple-upload/before.xml")
    )
    fun multipleUploadWithDifferentAssessment() {
        upload(
            getFile("controller/multiple-upload/first.csv"),
            "/quality_matrix/upload-group/209/matrix-type/FULFILLMENT", HttpStatus.OK
        )

        upload(
            getFile("controller/multiple-upload/second.csv"),
            "/quality_matrix/upload-group/209/matrix-type/FULFILLMENT", HttpStatus.BAD_REQUEST
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:controller/upload-quality-group/categories_before.xml")
    )
    fun createSupplyItemWithNullValues() {
        upload(
            getFile("controller/upload-quality-group/file.csv"),
            "/quality_matrix/upload-group/0/matrix-type/FULFILLMENT", HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-quality-group-default-attributes/categories.xml")
    @ExpectedDatabase(
        value = "classpath:controller/upload-quality-group-default-attributes/import_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun uploadQualityGroupMatrixWithSupplyItemAttributesTypes() {
        upload(
            getFile("controller/upload-quality-group-default-attributes/file.csv"),
            "/quality_matrix/upload-group/10/matrix-type/FULFILLMENT", HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup(
            "classpath:controller/quality-matrix/create-all-enabled-matrix-for-entire-group/quality_group.xml"
        ),
        DatabaseSetup(
            "classpath:controller/quality-matrix/create-all-enabled-matrix-for-entire-group/qattribute.xml"
        ),
        DatabaseSetup(
            "classpath:controller/quality-matrix/create-all-enabled-matrix-for-entire-group/group_attribute.xml"
        ),
    )
    @ExpectedDatabase(
        value =
        "classpath:controller/quality-matrix/create-all-enabled-matrix-for-entire-group/result/qmatrix_group.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createAllEnabledMatrixForEntireGroup() {
        val requestBuilder = MockMvcRequestBuilders.request(
            HttpMethod.POST,
            "/quality_matrix/create"
        )
            .param("groupId", "1000000")
            .param("matrixType", "FULFILLMENT")
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
    }

    @DatabaseSetup("classpath:service/empty.xml")
    @Test
    fun createAllEnabledMatrixForEntireGroupNoSuchGroup() {
        val requestBuilder = MockMvcRequestBuilders.request(
            HttpMethod.POST,
            "/quality_matrix/create"
        )
            .param("groupId", "1000000")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.INTERNAL_SERVER_ERROR.value()))
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/qmatrix_group.xml"),
        DatabaseSetup("classpath:repository/quality_attribute_inclusion.xml"),
    )
    fun listMatrix() {

        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "attributeName")
            add("offset", "0")
            add("limit", "20")
        }

        testGetEndpoint(
            "/quality_matrix/list",
            params,
            "controller/quality-matrix/matrix-list/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/qmatrix_group.xml"),
        DatabaseSetup("classpath:repository/quality_attribute_inclusion.xml"),
    )
    fun listMatrixWithFilter() {

        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "matrixId")
            add("offset", "0")
            add("limit", "20")
            add("filter", "(attributeName=='PACKAGE_HOLES')")
        }

        testGetEndpoint(
            "/quality_matrix/list",
            params,
            "controller/quality-matrix/matrix-list-with-filter/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:repository/qmatrix_group.xml"),
        DatabaseSetup("classpath:repository/quality_attribute_inclusion.xml"),
    )
    fun listMatrixWithDateFilter() {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "matrixId")
            add("offset", "0")
            add("limit", "20")
            add("filter", "(updatedAt=='2021-12-08 10:55:45')")
        }

        testGetEndpoint(
            "/quality_matrix/list",
            params,
            "controller/quality-matrix/matrix-list-with-date-filter/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:controller/quality-matrix/active-matrix-list/qmatrix_group.xml"),
        DatabaseSetup("classpath:controller/quality-matrix/active-matrix-list/quality_attribute_inclusion.xml"),
    )
    fun listActiveMatrix() {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "matrixId")
            add("offset", "0")
            add("limit", "20")
            add("filter", "")
            add("showActiveMatrix", "true")
        }

        testGetEndpoint(
            "/quality_matrix/list",
            params,
            "controller/quality-matrix/active-matrix-list/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
            DatabaseSetup("classpath:repository/qattribute.xml"),
            DatabaseSetup("classpath:repository/group.xml"),
            DatabaseSetup("classpath:controller/quality-matrix/active-matrix-list/qmatrix_group.xml"),
            DatabaseSetup("classpath:controller/quality-matrix/active-matrix-list/quality_attribute_inclusion.xml"),
    )
    fun listActiveMatrixExel() {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "matrixId")
            add("filter", "")
        }

        testGetEndpointWithExcelFile(
                "/quality_matrix/list/excel",
                params,
                HttpStatus.OK,
                "quality_matrix_table.xlsx"
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:controller/quality-matrix/active-matrix-list-large/qmatrix_group.xml"),
        DatabaseSetup("classpath:controller/quality-matrix/active-matrix-list-large/quality_attribute_inclusion.xml"),
    )
    fun listLargeActiveMatrixExcel() {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "matrixId")
            add("filter", "")
        }

        testGetEndpointWithExcelFile(
            "/quality_matrix/list/excel",
            params,
            HttpStatus.OK,
            "quality_matrix_table.csv"
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml"),
        DatabaseSetup("classpath:repository/group.xml"),
        DatabaseSetup("classpath:controller/quality-matrix/active-matrix-list/qmatrix_group.xml"),
        DatabaseSetup("classpath:controller/quality-matrix/active-matrix-list/quality_attribute_inclusion.xml"),
    )
    fun listActiveMatrixWithFilter() {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("sort", "matrixId")
            add("offset", "0")
            add("limit", "20")
            add("filter", "(attributeName=='PACKAGE_HOLES')")
            add("showActiveMatrix", "true")
        }

        testGetEndpoint(
            "/quality_matrix/list",
            params,
            "controller/quality-matrix/active-matrix-list-with-filter/response.json",
            HttpStatus.OK)
    }

    private fun getFile(url: String): MockMultipartFile {
        val input = this.javaClass.classLoader.getResource(url)
        val file = File(requireNonNull(input).toURI())

        var content: ByteArray? = null
        try {
            content = file.readBytes()
        } catch (e: IOException) {
        }

        return MockMultipartFile(url, url, "text/plan", content)
    }

    private fun upload(file: MockMultipartFile, url: String, expectedStatus: HttpStatus): ResultActions? {
        val upload: MockHttpServletRequestBuilder = MockMvcRequestBuilders.multipart(url)
            .file("file", file.bytes)
            .param("qualityGroupName", "GROUP 10")

        try {
            val responseBody = mockMvc.perform(upload)

            responseBody.andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value()))
                .andReturn().response.contentAsString

            return responseBody
        } catch (e: java.lang.Exception) {
            println(e)
            throw RuntimeException(e)
        }
    }
}

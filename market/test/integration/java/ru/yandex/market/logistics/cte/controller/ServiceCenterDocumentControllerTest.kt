package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest

class ServiceCenterDocumentControllerTest : MvcIntegrationTest() {


    @Test
    @DatabaseSetup("classpath:controller/service-center/upload-file/1/before.xml")
    @ExpectedDatabase(
        "classpath:controller/service-center/upload-file/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun uploadDocumentTest() {
        val file = MockMultipartFile("file", "some text".toByteArray())
        val params = LinkedMultiValueMap<String, String>().apply {
            add("itemId", "1")
            add("type", "NRP_PROTOCOL")
        }

        testEndpointPostStatusWithMultiPartFile(
            "/service_center_documents",
            Pair("document", file), params, HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/service-center/upload-file/1/before.xml")
    fun uploadDocumentNonExistentTest() {
        val file = MockMultipartFile("file", "some text".toByteArray())
        val params = LinkedMultiValueMap<String, String>().apply {
            add("itemId", "2")
            add("type", "NRP_PROTOCOL")
        }

        testEndpointPostStatusWithMultiPartFile(
            "/service_center_documents",
            Pair("document", file), params, HttpStatus.NOT_FOUND
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/service-center/upload-file/1/before.xml")
    fun uploadDocumentBanTypeTest() {
        val file = MockMultipartFile("file", "some text".toByteArray())
        val params = LinkedMultiValueMap<String, String>().apply {
            add("itemId", "1")
            add("type", "REFUND_RECEIPT")
        }

        testEndpointPostStatusWithMultiPartFile(
            "/service_center_documents",
            Pair("document", file), params, HttpStatus.BAD_REQUEST
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/service-center/upload-file/2/before.xml")
    @ExpectedDatabase(
        "classpath:controller/service-center/upload-file/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun deleteDocumentTest() {
        testEndpointDeleteStatusWithMultiPartFile(
            "/service_center_documents/1", LinkedMultiValueMap(), HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/service-center/upload-file/2/before.xml")
    @ExpectedDatabase(
        "classpath:controller/service-center/upload-file/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun deleteDocumentNonExistentTest() {
        testEndpointDeleteStatusWithMultiPartFile(
            "/service_center_documents/2", LinkedMultiValueMap(), HttpStatus.NOT_FOUND
        )
    }
}

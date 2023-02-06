package ru.yandex.market.logistics.cte.base

import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.util.MultiValueMap
import org.springframework.web.context.WebApplicationContext

open class MvcIntegrationTest(): IntegrationTest(){

    protected lateinit var mockMvc: MockMvc

    @Autowired
    private val wac: WebApplicationContext? = null

    @BeforeEach
    fun setupMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac!!)
                .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
                .build()
    }

    @Throws(Exception::class)
    protected fun testEndpointPut(url: String,
                                  requestFile: String,
                                  responseFile: String,
                                  expectedStatus: HttpStatus) {
        val responseBody = mockMvc.perform(MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(readFromFile(requestFile))
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value())).andReturn().response.contentAsString
        JSONAssert.assertEquals(readFromFile(responseFile), responseBody, JSONCompareMode.LENIENT)
    }

    @Throws(Exception::class)
    protected fun testEndpointPut(url: String,
                                  responseFile: String,
                                  expectedStatus: HttpStatus) {
        val responseBody = mockMvc.perform(MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value())).andReturn().response.contentAsString
        JSONAssert.assertEquals(readFromFile(responseFile), responseBody, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Throws(Exception::class)
    protected fun testEndpointPutStatus(url: String,
                                        requestFile: String,
                                        expectedStatus: HttpStatus) {
        mockMvc.perform(MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(readFromFile(requestFile))
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value()))
    }

    @Throws(Exception::class)
    protected fun testEndpointPutStatus(url: String,
                                        expectedStatus: HttpStatus) {
        mockMvc.perform(MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value()))
    }

    @Throws(Exception::class)
    protected fun testEndpointPost(url: String,
                                   requestFile: String,
                                   responseFile: String,
                                   expectedStatus: HttpStatus) {
        val responseBody = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFromFile(requestFile))
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value())).andReturn().response.contentAsString
        JSONAssert.assertEquals(readFromFile(responseFile), responseBody, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Throws(Exception::class)
    protected fun testEndpointPostWithParams(url: String,
                                             params: MultiValueMap<String, String>,
                                             responseFile: String,
                                             expectedStatus: HttpStatus) {
        val responseBody = mockMvc.perform(MockMvcRequestBuilders.post(url)
            .params(params)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value())).andReturn().response.contentAsString
        JSONAssert.assertEquals(readFromFile(responseFile), responseBody, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Throws(Exception::class)
    protected fun testEndpointPostWithLenientCompareMode(url: String,
                                   requestFile: String,
                                   responseFile: String,
                                   expectedStatus: HttpStatus) {
        val responseBody = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(readFromFile(requestFile))
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value())).andReturn().response.contentAsString
        JSONAssert.assertEquals(readFromFile(responseFile), responseBody, JSONCompareMode.LENIENT)
    }

    @Throws(Exception::class)
    protected fun testEndpointPostStatus(url: String,
                                         requestFile: String,
                                         expectedStatus: HttpStatus) {
        mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(readFromFile(requestFile))
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value()))
    }

    @Throws(Exception::class)
    protected fun testEndpointPostStatusWithMultiPartFile(url: String,
                                                          file: Pair<String, MockMultipartFile>,
                                                          params: MultiValueMap<String, String>,
                                                          expectedStatus: HttpStatus) {
        mockMvc.perform(MockMvcRequestBuilders.multipart(url)
            .file(file.first, file.second.bytes)
            .contentType(MediaType.APPLICATION_JSON)
            .params(params)
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value()))
    }

    @Throws(Exception::class)
    protected fun testEndpointDeleteStatusWithMultiPartFile(url: String,
                                                            params: MultiValueMap<String, String>,
                                                            expectedStatus: HttpStatus) {
        mockMvc.perform(MockMvcRequestBuilders.delete(url)
               .contentType(MediaType.APPLICATION_JSON)
               .params(params)
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value()))
    }

    @Throws(Exception::class)
    protected fun testEndpointPostStatusWithoutRequestFile(url: String,
                                         expectedStatus: HttpStatus) {
        mockMvc.perform(MockMvcRequestBuilders.post(url)
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value()))
    }

    protected fun testGetEndpoint(
            url: String, params: MultiValueMap<String, String>, responseFile: String?, expectedStatus: HttpStatus
    ) {
        val responseBody = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                        .params(params)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value())).andReturn().response.contentAsString

        JSONAssert.assertEquals(readFromFile(responseFile), responseBody, JSONCompareMode.LENIENT)
    }

    protected fun testGetEndpointWithExcelFile(
            url: String, params: MultiValueMap<String, String>, expectedStatus: HttpStatus, tableName: String
    ) {
        val response = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                        .params(params)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value())).andReturn().response

        assertions.assertThat(response.contentAsByteArray.size).isNotEqualTo(0);
        Assert.assertEquals(response.getHeader("Content-disposition"),
                "attachment; filename=$tableName")
        Assert.assertEquals(response.getHeader("Content-Type"), "application/octet-stream")
    }

    protected fun testGetEndpointWithException(
            url: String, params: MultiValueMap<String, String>, responseFile: String?, expectedStatus: HttpStatus, exception: Exception
    ) {
        val responseBody = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                        .params(params)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value())).andExpect { exception }
    }
}

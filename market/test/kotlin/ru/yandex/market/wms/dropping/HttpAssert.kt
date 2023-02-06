package ru.yandex.market.wms.dropping

import org.assertj.core.api.Assertions
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.model.enums.AuthenticationParam
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils
import java.nio.charset.StandardCharsets
import javax.servlet.http.Cookie

class HttpAssert(private val mockMvc: () -> MockMvc) {

    fun assertApiCallOk(
        request: MockHttpServletRequestBuilder,
        requestFile: String? = null,
        responseFile: String? = null
    ) {
        initRequest(request, requestFile)
        val mvcResult = mockMvc().perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        responseFile?.let {
            JsonAssertUtils.assertFileNonExtensibleEquals(
                it,
                mvcResult.response.getContentAsString(StandardCharsets.UTF_8)
            )
        }
    }

    fun assertApiCallError(
        request: MockHttpServletRequestBuilder,
        requestFile: String? = null,
        errorFragment: String? = null,
        resultMatcher: ResultMatcher
    ) {
        initRequest(request, requestFile)
        val mvcResult = mockMvc().perform(request)
            .andExpect(resultMatcher)
            .andReturn()
        errorFragment?.let {
            Assertions.assertThat(mvcResult.response.getContentAsString(StandardCharsets.UTF_8)).contains(it)
        }
    }

    private fun initRequest(request: MockHttpServletRequestBuilder, requestFile: String?) {
        request.cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
            .contentType(MediaType.APPLICATION_JSON)
        requestFile?.let { request.content(FileContentUtils.getFileContent(it)) }
    }
}

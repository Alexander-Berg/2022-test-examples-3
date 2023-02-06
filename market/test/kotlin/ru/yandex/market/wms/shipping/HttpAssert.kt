package ru.yandex.market.wms.shipping

import org.skyscreamer.jsonassert.JSONCompareMode
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

    fun assertApiCall(
        request: MockHttpServletRequestBuilder,
        status: ResultMatcher = MockMvcResultMatchers.status().isOk,
        requestFile: String? = null,
        responseFile: String? = null,
        compareMode: JSONCompareMode = JSONCompareMode.NON_EXTENSIBLE
    ) {
        initRequest(request, requestFile)
        val mvcResult = mockMvc().perform(request)
            .andExpect(status)
            .andReturn()
        responseFile?.let {
            JsonAssertUtils.assertFileNonExtensibleEquals(
                it,
                mvcResult.response.getContentAsString(StandardCharsets.UTF_8),
                compareMode,
            )
        }
    }

    private fun initRequest(request: MockHttpServletRequestBuilder, requestFile: String?) {
        request.cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
            .contentType(MediaType.APPLICATION_JSON)
        requestFile?.let { request.content(FileContentUtils.getFileContent(it)) }
    }
}

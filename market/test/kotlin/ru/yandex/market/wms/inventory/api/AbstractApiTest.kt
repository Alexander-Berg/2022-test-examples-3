package ru.yandex.market.wms.inventory.api

import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.junit.jupiter.api.TestInstance
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.support.DirtiesContextTestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.wms.inventory.AbstractFunctionalTest
import ru.yandex.market.wms.inventory.helper.NullableColumnsDataSetLoader
import java.nio.charset.StandardCharsets
import javax.annotation.Nonnull
import javax.servlet.http.Cookie

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@Transactional
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    DirtiesContextTestExecutionListener::class,
    TransactionalTestExecutionListener::class,
    DbUnitTestExecutionListener::class
)
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader::class)
abstract class AbstractApiTest : AbstractFunctionalTest() {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    protected fun resourceAsString(path: String): String = String(ClassPathResource(path).file.readBytes())

    @Throws(Exception::class)
    protected fun assertApiCall(
        requestFile: String, responseFile: String?,
        request: MockHttpServletRequestBuilder, status: ResultMatcher,
        mode: JSONCompareMode
    ) {
        val mvcResult = mockMvc.perform(
            request
                .cookie(Cookie("Username", "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceAsString(requestFile))
        )
            .andExpect(status)
            .andReturn()
        val res = mvcResult.response.contentAsString
        if (responseFile != null) {
            assertFileNonExtensibleEquals(
                responseFile,
                mvcResult.response.getContentAsString(StandardCharsets.UTF_8),
                mode
            )
        }
    }

    private fun assertFileNonExtensibleEquals(
        @Nonnull expectedJsonFileName: String?, @Nonnull actualJson: String?,
        compareMode: JSONCompareMode?
    ) {
        try {
            JSONAssert.assertEquals(expectedJsonFileName?.let { resourceAsString(it) }, actualJson, compareMode)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    @Throws(java.lang.Exception::class)
    fun assertApiCallClientError(
        requestFile: String,
        request: MockHttpServletRequestBuilder,
        errorInfo: String
    ) {
        val mvcResult = mockMvc.perform(
            request
                .cookie(Cookie("Username", "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceAsString(requestFile))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()

        assertThat(mvcResult.response.getContentAsString(StandardCharsets.UTF_8)).contains(errorInfo)
    }
}

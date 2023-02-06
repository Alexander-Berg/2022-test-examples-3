package ru.yandex.market.pers.tvm.spring

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import ru.yandex.market.pers.test.common.AbstractMvcMocks
import ru.yandex.market.pers.tvm.TvmUtils
import ru.yandex.market.util.ExecUtils

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.03.2021
 */
class TvmCheckMvcMock : AbstractMvcMocks() {
    fun methodWithTvm(ticket: String?, resultMatcher: ResultMatcher): String {
        return invokeAndRetrieveResponse(
            addTvm(MockMvcRequestBuilders.get("/test/tvm/method"), ticket)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        )
    }

    fun methodWithTvmTrusted(ticket: String?, resultMatcher: ResultMatcher): String {
        return invokeAndRetrieveResponse(
            addTvm(MockMvcRequestBuilders.get("/test/tvm/restricted/method"), ticket)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        )
    }

    fun methodWithTvmTrusted2(ticket: String?, resultMatcher: ResultMatcher): String {
        return invokeAndRetrieveResponse(
            addTvm(MockMvcRequestBuilders.get("/test/tvm/restricted/method2"), ticket)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        )
    }

    fun methodWithTvmHeader(ticket: String?, resultMatcher: ResultMatcher): String? {
        return try {
            mockMvc.perform(
                addTvm(MockMvcRequestBuilders.get("/test/tvm/method"), ticket)
                    .accept(MediaType.APPLICATION_JSON)
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(resultMatcher)
                .andReturn().response.getHeader("TVM_ERROR_MSG")
        } catch (e: Exception) {
            throw ExecUtils.silentError(e, "Failed mvcMock call")
        }
    }

    fun methodWithTvmError(ticket: String?, resultMatcher: ResultMatcher): String {
        return invokeAndRetrieveResponse(
            addTvm(MockMvcRequestBuilders.get("/test/tvm/methodErr"), ticket)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        )
    }

    fun methodWithTvmDisabled(ticket: String?, resultMatcher: ResultMatcher): String {
        return invokeAndRetrieveResponse(
            addTvm(MockMvcRequestBuilders.get("/test/tvm/methodWithoutTvm"), ticket)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        )
    }

    fun methodWithTvmDisabled2(ticket: String?, resultMatcher: ResultMatcher): String {
        return invokeAndRetrieveResponse(
            addTvm(MockMvcRequestBuilders.get("/test/notvm/method"), ticket)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher
        )
    }

    private fun addTvm(builder: MockHttpServletRequestBuilder, ticket: String?): MockHttpServletRequestBuilder {
        return if (ticket != null) {
            builder.header(TvmUtils.SERVICE_TICKET_HEADER, ticket)
        } else builder
    }
}
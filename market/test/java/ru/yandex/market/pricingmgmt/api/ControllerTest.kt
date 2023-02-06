package ru.yandex.market.pricingmgmt.api

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationFilter
import java.io.IOException

@AutoConfigureMockMvc
@WithMockUser(username = PassportAuthenticationFilter.LOCAL_DEV, roles = ["PRICING_MGMT_ACCESS"])
abstract class ControllerTest : AbstractFunctionalTest() {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected var objectMapper: ObjectMapper? = null

    @Throws(IOException::class)
    protected fun <T> readJson(content: String?, cls: Class<T>?): T {
        return objectMapper!!.readValue(content, cls)
    }

    @Throws(IOException::class)
    protected fun <T> readJson(content: MvcResult, cls: Class<T>?): T {
        return objectMapper!!.readValue(content.response.contentAsString, cls)
    }

    @Throws(IOException::class)
    protected fun <T> readJson(content: String?, cls: TypeReference<T>?): T {
        return objectMapper!!.readValue(content, cls)
    }

    @Throws(IOException::class)
    protected fun <T> readJson(result: MvcResult, cls: TypeReference<T>?): T {
        return readJson(result.response.contentAsString, cls)
    }

    @Throws(JsonProcessingException::class)
    protected fun dtoToString(dto: Any?): String {
        // objectMapper!!.configure(SerializationFeature.WRAP_ROOT_VALUE, false)
        val ow = objectMapper!!.writer().withDefaultPrettyPrinter()
        return ow.writeValueAsString(dto)
    }

    protected inline fun <reified T : Any> ObjectMapper.readValue(content: String): T =
        readValue(content, object : TypeReference<T>() {})
}

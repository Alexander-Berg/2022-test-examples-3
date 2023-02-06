package ru.yandex.market.wms.core.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

private const val DB_PATH = "/controller/nsqlconfig/db.xml"

class NSqlConfigControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getMultipleNSqlValues() {
        assertGetNSqlValues(
            "controller/nsqlconfig/get-values/response/multiple-values-response.json",
            MockMvcResultMatchers.status().is2xxSuccessful,
            "KEY2", "KEY3"
        )
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getNSqlValue() {
        assertGetNSqlValues(
            "controller/nsqlconfig/get-values/response/one-value-response.json",
            MockMvcResultMatchers.status().is2xxSuccessful,
            "KEY2"
        )
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getManyNSqlValues() {
        assertGetNSqlValues(
            "controller/nsqlconfig/get-values/response/many-values-response.json",
            MockMvcResultMatchers.status().is2xxSuccessful,
            "KEY1", "KEY2", "KEY3"
        )
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getNonexistentNSqlValue() {
        assertGetNSqlValues(
            "controller/nsqlconfig/get-values/response/empty-response.json",
            MockMvcResultMatchers.status().is2xxSuccessful,
            "KEY5", "KEY6"
        )
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getSameNSqlValues() {
        assertGetNSqlValues(
            "controller/nsqlconfig/get-values/response/one-value-response.json",
            MockMvcResultMatchers.status().is2xxSuccessful,
            "KEY2", "KEY2"
        )
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getExistentAndNonNSqlValues() {
        assertGetNSqlValues(
            "controller/nsqlconfig/get-values/response/one-value-response.json",
            MockMvcResultMatchers.status().is2xxSuccessful,
            "KEY2", "KEY6"
        )
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getNoNSqlValues() {
        assertGetNSqlValues(
            "controller/nsqlconfig/get-values/response/empty-parameter-response.json",
            MockMvcResultMatchers.status().is4xxClientError,
            ""
        )
    }

    private fun assertGetNSqlValues(responseFile: String, status: ResultMatcher, vararg configkeys: String) {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/nsqlconfig/values")
                .param("configkeys", *configkeys)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent(responseFile)))
            .andReturn()
    }
}

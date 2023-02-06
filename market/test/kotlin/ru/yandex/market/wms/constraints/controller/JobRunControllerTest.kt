package ru.yandex.market.wms.constraints.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest

class JobRunControllerTest : ConstraintsIntegrationTest() {

    @Test
    @DatabaseSetup("/controller/job-run/problem-cargo-types/ok/before.xml")
    @ExpectedDatabase(
        value = "/controller/job-run/problem-cargo-types/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `run problemCargoTypes job`() {
        postEmptyRes("/job/run/problemCargoTypes")
    }

    private fun postEmptyRes(url: String) {
        mockMvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}

package ru.yandex.market.logistics.calendaring.tvm

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.config.SecurityTestConfig

class TvmSecurityTestWithDisabledTvm : AbstractContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/get-slot/slot-exists/before.xml")
    fun testRequestIsSuccessfulForUnsecuredHealthMethod() {
        mockMvc!!.perform(MockMvcRequestBuilders.get("/booking/1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}

package ru.yandex.market.logistics.calendaring.tvm

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.Matchers.emptyOrNullString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.config.SecurityTestConfig
import ru.yandex.market.logistics.calendaring.tvm.TvmTestUtils.Companion.baseRequestBuilder
import ru.yandex.market.logistics.calendaring.tvm.TvmTestUtils.Companion.builderWithEmptyServiceTicket
import ru.yandex.market.logistics.calendaring.tvm.TvmTestUtils.Companion.builderWithServiceTicket
import ru.yandex.market.logistics.calendaring.tvm.TvmTestUtils.Companion.builderWithUserTicket
import ru.yandex.market.logistics.calendaring.tvm.TvmTestUtils.Companion.mockServiceTicketCheck
import ru.yandex.market.logistics.calendaring.tvm.TvmTestUtils.Companion.mockUserTicketCheck
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi


@TestPropertySource(properties = ["tvm.unsecured-methods=/ping", "tvm.internal.check-user-ticket=true"])
@Import(SecurityTestConfig::class)
class TvmSecurityTest(
    @Autowired
    private val tvmClientApi: TvmClientApi
) : AbstractContextualTest() {

    @Test
    @Throws(Exception::class)
    fun testRequestIsAbortedWithoutTicket() {
        mockMvc!!.perform(baseRequestBuilder())
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(
                MockMvcResultMatchers.content().string(CoreMatchers.containsString("SERVICE_TICKET_NOT_PRESENT"))
            )
    }

    @Test
    @Throws(Exception::class)
    fun testRequestIsAbortedWithEmptyServiceTicket() {
        mockMvc!!.perform(builderWithEmptyServiceTicket())
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(
                MockMvcResultMatchers.content().string(CoreMatchers.containsString("SERVICE_TICKET_NOT_PRESENT"))
            )
    }

    @Test
    @Throws(Exception::class)
    fun testRequestIsAbortedWithNotValidServiceTicket() {
        mockServiceTicketCheck(false, tvmClientApi)
        mockMvc!!.perform(builderWithServiceTicket())
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(MockMvcResultMatchers.content().string(CoreMatchers.containsString("BAD_SERVICE_TICKET")))
    }

    @Test
    @Throws(Exception::class)
    @DatabaseSetup("classpath:fixtures/controller/booking/get-slot/slot-exists/before.xml")
    fun testSuccessfulRequestWithUserTicket() {
        mockServiceTicketCheck(true, tvmClientApi)
        mockUserTicketCheck(true, tvmClientApi)
        mockMvc!!.perform(builderWithUserTicket())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().string(Matchers.not(Matchers.`is`(emptyOrNullString()))))
    }

    @Test
    @Throws(Exception::class)
    fun testRequestIsAbortedWithNotValidUserTicket() {
        mockServiceTicketCheck(true, tvmClientApi)
        mockUserTicketCheck(false, tvmClientApi)
        mockMvc!!.perform(builderWithUserTicket())
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(MockMvcResultMatchers.content().string(CoreMatchers.containsString("BAD_USER_TICKET")))
    }
}

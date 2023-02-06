package ru.yandex.market.logistics.calendaring.tvm

import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi
import ru.yandex.market.logistics.util.client.tvm.client.TvmServiceTicket
import ru.yandex.market.logistics.util.client.tvm.client.TvmTicketStatus
import ru.yandex.market.logistics.util.client.tvm.client.TvmUserTicket

internal class TvmTestUtils private constructor() {
    companion object {
        const val SERVICE_TICKET_HEADER = "X-Ya-Service-Ticket"
        const val USER_SERVICE_TICKET_HEADER = "X-Ya-User-Ticket"
        private const val TEST_URL = "/booking/1"
        private const val SERVICE_TICKET_VALUE = "ServiceTicket"
        private const val USER_TICKET_VALUE = "UserTicket"
        fun mockUserTicketCheck(isValid: Boolean, tvmClientApi: TvmClientApi) {
            Mockito.doReturn(
                TvmUserTicket(
                    null,
                    if (isValid) TvmTicketStatus.OK else TvmTicketStatus.INVALID_DESTINATION
                )
            )
                .`when`(tvmClientApi)
                .checkUserTicket(ArgumentMatchers.eq(USER_TICKET_VALUE))
        }

        fun mockServiceTicketCheck(isValid: Boolean, tvmClientApi: TvmClientApi) {
            Mockito.doReturn(
                TvmServiceTicket(
                    1,
                    if (isValid) TvmTicketStatus.OK else TvmTicketStatus.INVALID_DESTINATION,
                    ""
                )
            )
                .`when`(tvmClientApi)
                .checkServiceTicket(ArgumentMatchers.eq(SERVICE_TICKET_VALUE))
        }

        fun baseRequestBuilder(): MockHttpServletRequestBuilder {
            return MockMvcRequestBuilders.get(TEST_URL)
        }

        fun builderWithServiceTicket(): MockHttpServletRequestBuilder {
            return baseRequestBuilder()
                .header(SERVICE_TICKET_HEADER, SERVICE_TICKET_VALUE)
        }

        fun builderWithEmptyServiceTicket(): MockHttpServletRequestBuilder {
            return baseRequestBuilder()
                .header(SERVICE_TICKET_HEADER, "")
        }

        fun builderWithUserTicket(): MockHttpServletRequestBuilder {
            return builderWithServiceTicket()
                .header(USER_SERVICE_TICKET_HEADER, USER_TICKET_VALUE)
        }
    }

    init {
        throw AssertionError()
    }
}

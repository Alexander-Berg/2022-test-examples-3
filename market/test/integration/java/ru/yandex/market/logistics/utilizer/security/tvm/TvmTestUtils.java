package ru.yandex.market.logistics.utilizer.security.tvm;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.util.client.tvm.client.TvmServiceTicket;
import ru.yandex.market.logistics.util.client.tvm.client.TvmTicketStatus;
import ru.yandex.market.logistics.util.client.tvm.client.TvmUserTicket;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class TvmTestUtils {
    public static final String SERVICE_TICKET_HEADER = "X-Ya-Service-Ticket";
    public static final String USER_SERVICE_TICKET_HEADER = "X-Ya-User-Ticket";
    private static final String TEST_URL = "/utilization-cycle/123/warehouse-stock-types";
    private static final String SERVICE_TICKET_VALUE = "ServiceTicket";
    private static final String USER_TICKET_VALUE = "UserTicket";

    private TvmTestUtils() {
        throw new AssertionError();
    }

    static void mockUserTicketCheck(boolean isValid, TvmClientApi tvmClientApi) {
        doReturn(new TvmUserTicket(null, isValid ? TvmTicketStatus.OK : TvmTicketStatus.INVALID_DESTINATION))
                .when(tvmClientApi)
                .checkUserTicket(eq(USER_TICKET_VALUE));
    }

    static void mockServiceTicketCheck(boolean isValid, TvmClientApi tvmClientApi) {
        doReturn(new TvmServiceTicket(1, isValid ? TvmTicketStatus.OK : TvmTicketStatus.INVALID_DESTINATION, ""))
                .when(tvmClientApi)
                .checkServiceTicket(eq(SERVICE_TICKET_VALUE));
    }

    static MockHttpServletRequestBuilder baseRequestBuilder() {
        return get(TEST_URL);
    }

    static MockHttpServletRequestBuilder builderWithServiceTicket() {
        return baseRequestBuilder()
                .header(SERVICE_TICKET_HEADER, SERVICE_TICKET_VALUE);
    }

    static MockHttpServletRequestBuilder builderWithEmptyServiceTicket() {
        return baseRequestBuilder()
                .header(SERVICE_TICKET_HEADER, "");
    }

    static MockHttpServletRequestBuilder builderWithUserTicket() {
        return builderWithServiceTicket()
                .header(USER_SERVICE_TICKET_HEADER, USER_TICKET_VALUE);
    }

}

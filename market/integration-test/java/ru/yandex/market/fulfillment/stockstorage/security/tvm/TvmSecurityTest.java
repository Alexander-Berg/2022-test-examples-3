package ru.yandex.market.fulfillment.stockstorage.security.tvm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.security.tvm.TvmTestUtils.baseRequestBuilder;
import static ru.yandex.market.fulfillment.stockstorage.security.tvm.TvmTestUtils.builderWithEmptyServiceTicket;
import static ru.yandex.market.fulfillment.stockstorage.security.tvm.TvmTestUtils.builderWithServiceTicket;
import static ru.yandex.market.fulfillment.stockstorage.security.tvm.TvmTestUtils.builderWithUserTicket;
import static ru.yandex.market.fulfillment.stockstorage.security.tvm.TvmTestUtils.mockServiceTicketCheck;
import static ru.yandex.market.fulfillment.stockstorage.security.tvm.TvmTestUtils.mockUserTicketCheck;

@TestPropertySource(properties = {
        "tvm.unsecured-methods=/ping,/health/**",
        "stockstorage.tvm.check-user-ticket=true",
})
@AutoConfigureMockMvc(addFilters = true)
public class TvmSecurityTest extends AbstractContextualTest {

    @Autowired
    private TvmClientApi tvmClientApi;

    @Test
    public void testRequestIsAbortedWithoutTicket() throws Exception {
        mockMvc.perform(baseRequestBuilder())
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(containsString("SERVICE_TICKET_NOT_PRESENT")));
    }

    @Test
    public void testRequestIsSuccessfulForUnsecuredHealthMethod() throws Exception {
        String contentAsString = mockMvc.perform(get("/health/hangingJobs"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .isEqualTo("");
    }

    @Test
    public void testRequestIsSuccessfulForUnsecuredPingMethod() throws Exception {

        String contentAsString = mockMvc.perform(get("/ping"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .isEqualTo("0;OK");
    }

    @Test
    public void testRequestIsAbortedWithEmptyServiceTicket() throws Exception {
        mockMvc.perform(builderWithEmptyServiceTicket())
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(containsString("SERVICE_TICKET_NOT_PRESENT")));
    }

    @Test
    public void testRequestIsAbortedWithNotValidServiceTicket() throws Exception {

        mockServiceTicketCheck(false, tvmClientApi);

        mockMvc.perform(builderWithServiceTicket())
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(containsString("BAD_SERVICE_TICKET")));
    }

    @Test
    public void testSuccessfulRequestWithUserTicket() throws Exception {

        mockServiceTicketCheck(true, tvmClientApi);
        mockUserTicketCheck(true, tvmClientApi);

        mockMvc.perform(builderWithUserTicket())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(not(isEmptyOrNullString())));
    }

    @Test
    public void testRequestIsAbortedWithNotValidUserTicket() throws Exception {

        mockServiceTicketCheck(true, tvmClientApi);
        mockUserTicketCheck(false, tvmClientApi);

        mockMvc.perform(builderWithUserTicket())
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(containsString("BAD_USER_TICKET")));

    }
}

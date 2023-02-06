package ru.yandex.market.logistics.utilizer.security.tvm;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.config.SecurityTestConfig;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.utilizer.security.tvm.TvmTestUtils.baseRequestBuilder;
import static ru.yandex.market.logistics.utilizer.security.tvm.TvmTestUtils.builderWithEmptyServiceTicket;
import static ru.yandex.market.logistics.utilizer.security.tvm.TvmTestUtils.builderWithServiceTicket;
import static ru.yandex.market.logistics.utilizer.security.tvm.TvmTestUtils.builderWithUserTicket;
import static ru.yandex.market.logistics.utilizer.security.tvm.TvmTestUtils.mockServiceTicketCheck;
import static ru.yandex.market.logistics.utilizer.security.tvm.TvmTestUtils.mockUserTicketCheck;


@DatabaseSetup("classpath:fixtures/controller/utilization-cycle/warehouse-stock-types/3/before.xml")
@TestPropertySource(properties = {
        "tvm.unsecured-methods=/actuator/health",
        "tvm.utilizer.check-user-ticket=true",
})
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
    public void testRequestIsSuccessfulForUnsecuredActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\": \"UP\"}"));
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

package ru.yandex.market.hrms.api.external.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.starter.tvm.factory.TvmClientSettings.LOCAL_TVM_ID;

@TestPropertySource(properties = {
        "mj.tvm.checkServiceTicket=true",
        "mj.tvm.checkUserTicket=true"
})
@DbUnitDataSet(before = "HrmsAgentApiTest.common.before.csv")
class HrmsAgentFinancesApiServiceTest extends AbstractApiTest {

    private final static String SERVICE_TICKET = "3:serv:TEST";
    private final static String USER_TICKET = "3:user:TEST";
    private final static long USER_UID = -1;

    private final static String DATE = "2022-04-13";


    @MockBean
    private TvmClient tvmClient;

    @BeforeEach
    public void initTvm() {
        when(tvmClient.checkServiceTicket(SERVICE_TICKET))
                .thenReturn(new CheckedServiceTicket(TicketStatus.OK, null, LOCAL_TVM_ID, USER_UID));

        when(tvmClient.checkUserTicket(USER_TICKET))
                .thenReturn(new CheckedUserTicket(TicketStatus.OK, null, null, USER_UID, null));
    }


    @Test
    @DbUnitDataSet(before = "HrmsAgentFinancesApiTest.before.csv")
    public void happyPath() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/external/api/agent/v1/finances/bonus")
                        .queryParam("date", DATE)
                        .header("X-Ya-Service-Ticket", SERVICE_TICKET)
                        .header("X-Ya-User-Ticket", USER_TICKET))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("json/finance.happyPath.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "HrmsAgentFinancesApiTest.before.csv")
    @DbUnitDataSet(before = "HrmsAgentFinancesApiTest.withViolations.csv")
    public void addQualityFactorReductionComment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/external/api/agent/v1/finances/bonus")
                        .queryParam("date", "2022-01-01")
                        .header("X-Ya-Service-Ticket", SERVICE_TICKET)
                        .header("X-Ya-User-Ticket", USER_TICKET))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(loadFromFile("json/finance.withViolations.json"), true));
    }
}
package ru.yandex.market.billing.api.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.billing.api.config.MvcConfig;
import ru.yandex.market.billing.api.model.ContractFrequencyPair;
import ru.yandex.market.billing.api.model.CurrentAndNextMonthPayoutFrequency;
import ru.yandex.market.billing.api.service.ContractPayoutFrequencyService;
import ru.yandex.market.billing.core.factoring.PayoutFrequency;
import ru.yandex.market.billing.security.config.PassportConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PayoutFrequenciesController.class)
@AutoConfigureMockMvc(secure = false)
@ActiveProfiles("functionalTest")
class PayoutFrequenciesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PassportConfig passportConfig;
    @MockBean
    MvcConfig mvcConfig;

    @MockBean
    ContractPayoutFrequencyService service;

    @Test
    void createContractPayoutFrequencyOK() throws Exception {
        when(service.addCurrentMonthFrequencyForNewContract(anyLong(), any())).thenReturn(false);

        mockMvc.perform(post("/payout/frequencies/485739")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("{\"frequency\":\"DAILY\"}")
        ).andExpect(status().isOk());

        verify(service).addCurrentMonthFrequencyForNewContract(485739L, PayoutFrequency.DAILY);
    }

    @Test
    void createContractPayoutFrequencyCreated() throws Exception {
        when(service.addCurrentMonthFrequencyForNewContract(anyLong(), any())).thenReturn(true);

        mockMvc.perform(post("/payout/frequencies/9879870")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("{\"frequency\":\"BIWEEKLY\"}")
        ).andExpect(status().isCreated());

        verify(service).addCurrentMonthFrequencyForNewContract(9879870L, PayoutFrequency.BI_WEEKLY);
    }

    @Test
    void createContractPayoutFrequencyConflict() throws Exception {
        when(service.addCurrentMonthFrequencyForNewContract(anyLong(), any())).thenThrow(new IllegalStateException());

        mockMvc.perform(post("/payout/frequencies/92385")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("{\"frequency\":\"WEEKLY\"}")
        ).andExpect(status().isConflict());

        verify(service).addCurrentMonthFrequencyForNewContract(92385L, PayoutFrequency.WEEKLY);
    }

    @Test
    public void getCurrentAndNextMonthPayoutFrequencies() throws Exception {
        when(service.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        CurrentAndNextMonthPayoutFrequency.builder()
                                .setContractId(777)
                                .setCurrentMonthFrequency(PayoutFrequency.DAILY)
                                .setIsDefaultCurrentMonthFrequency(true)
                                .setNextMonthFrequency(PayoutFrequency.WEEKLY)
                                .build(),
                        CurrentAndNextMonthPayoutFrequency.builder()
                                .setContractId(888)
                                .setCurrentMonthFrequency(PayoutFrequency.BI_WEEKLY)
                                .setIsDefaultCurrentMonthFrequency(false)
                                .setNextMonthFrequency(PayoutFrequency.DAILY)
                                .build(),
                        CurrentAndNextMonthPayoutFrequency.builder()
                                .setContractId(999)
                                .setCurrentMonthFrequency(PayoutFrequency.BI_WEEKLY)
                                .setIsDefaultCurrentMonthFrequency(false)
                                .setNextMonthFrequency(PayoutFrequency.MONTHLY)
                                .build()
                ));

        mockMvc.perform(get("/payout/frequencies/current-next-month?contractIds=777,888"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json("{\n" +
                        "  \"frequencies\": [\n" +
                        "    {\n" +
                        "      \"contractId\": 777,\n" +
                        "      \"currentMonthFrequency\": \"DAILY\",\n" +
                        "      \"isDefaultCurrentMonthFrequency\": true,\n" +
                        "      \"nextMonthFrequency\": \"WEEKLY\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"contractId\": 888,\n" +
                        "      \"currentMonthFrequency\": \"BIWEEKLY\",\n" +
                        "      \"isDefaultCurrentMonthFrequency\": false,\n" +
                        "      \"nextMonthFrequency\": \"DAILY\"\n" +
                        "    },\n" +
                        "    {\n" +
                                "      \"contractId\": 999,\n" +
                        "      \"currentMonthFrequency\": \"BIWEEKLY\",\n" +
                        "      \"isDefaultCurrentMonthFrequency\": false,\n" +
                        "      \"nextMonthFrequency\": \"MONTHLY\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"));
    }

    @Test
    void setNextMonthPayoutFrequencies() throws Exception {
        mockMvc.perform(put("/payout/frequencies/next-month")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("{\n" +
                        "  \"frequencies\": [\n" +
                        "    {\n" +
                        "      \"contractId\": 12345,\n" +
                        "      \"frequency\": \"DAILY\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"contractId\": 23456,\n" +
                        "      \"frequency\": \"WEEKLY\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"contractId\": 34567,\n" +
                        "      \"frequency\": \"BIWEEKLY\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"contractId\": 34568,\n" +
                        "      \"frequency\": \"MONTHLY\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}")
        ).andExpect(status().isOk());

        verify(service).setNextMonthPayoutFrequencies(List.of(
                ContractFrequencyPair.of(12345L, PayoutFrequency.DAILY),
                ContractFrequencyPair.of(23456L, PayoutFrequency.WEEKLY),
                ContractFrequencyPair.of(34567L, PayoutFrequency.BI_WEEKLY),
                ContractFrequencyPair.of(34568L, PayoutFrequency.MONTHLY)
        ));
    }
}

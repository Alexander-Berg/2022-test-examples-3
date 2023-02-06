package ru.yandex.market.mbi.oebs.service.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.mbi.oebs.service.FunctionalTest;
import ru.yandex.market.mbi.oebs.service.model.ContractProblemDTO;
import ru.yandex.market.mbi.oebs.service.service.ContractProblemService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(secure = false)
public class ContractProblemControllerTest extends FunctionalTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ContractProblemService service;

    @Test
    void getContractProblems() throws Exception {
        List<ContractProblemDTO> problems = List.of(
                new ContractProblemDTO()
                        .partnerId(601017L)
                        .contractId("ОФ-489135")
                        .docOrigNotPresent(true)
                        .unpaidAmount(new BigDecimal("93977.25"))
                        .currency("RUR"),
                new ContractProblemDTO()
                        .partnerId(2383387L)
                        .contractId("ОФ-489135")
                        .docOrigNotPresent(true)
                        .unpaidAmount(new BigDecimal("93977.25"))
                        .currency("RUR")
        );

        when(service.getContractProblems(any(), any(), any())).thenReturn(problems);

        mockMvc.perform(get("/contract/problem/?business_id=850803")).andExpect(status().isOk());
    }
}

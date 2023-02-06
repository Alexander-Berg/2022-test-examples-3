package ru.yandex.market.wms.api.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class OutboundControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getInterWarehouseFactOutboundRegister() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-1243455")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("outbound/get-registry-{externalOrderKey}/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/auto-outbound/db.xml")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/auto-outbound/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getAutoOutboundRegistry() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "outbound/get-registry-{externalOrderKey}/auto-outbound/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/bbxd-withdrawal/immutable")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/bbxd-withdrawal/immutable",
            assertionMode = NON_STRICT_UNORDERED)
    void getBbxdOutboundRegistry() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-6773416")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "outbound/get-registry-{externalOrderKey}/bbxd-withdrawal/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/initial-state-non-68-state.xml")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/initial-state-non-68-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getInterWarehouseFactOutboundRegisterNon68State() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-1243455")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("outbound/get-registry-{externalOrderKey}/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/identity-uit/1/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/identity-uit/1/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getInterWarehouseFactOutboundRegisterWithCIS() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-1243455")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("outbound/get-registry-{externalOrderKey}/identity-uit/1/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/identity-uit/2/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/identity-uit/2/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cisInfoNotFoundForItem() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-1243455")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("outbound/get-registry-{externalOrderKey}/identity-uit/2/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/identity-uit/3/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/identity-uit/3/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getInterWarehouseFactOutboundRegisterWithIdentities() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-1243455")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("outbound/get-registry-{externalOrderKey}/identity-uit/3/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/identity-uit/4/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/identity-uit/4/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void identityRequirementsShouldBeReturned() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-1243455")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("outbound/get-registry-{externalOrderKey}/identity-uit/4/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/anomaly-withdrawal/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/anomaly-withdrawal/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getAnomalyWithdrawalFactRegister() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-1243455")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("outbound/get-registry-{externalOrderKey}/anomaly-withdrawal/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/get-registry-{externalOrderKey}/anomaly-withdrawal/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/get-registry-{externalOrderKey}/anomaly-withdrawal/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getAnomalyWithdrawalWithoutArticle() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/registry/outbound-1243456")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("outbound/get-registry-{externalOrderKey}/anomaly-withdrawal" +
                                "/response-no-article.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/details/ok/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/details/ok/initial-state.xml", assertionMode = NON_STRICT_UNORDERED)
    void getOutboundDetails() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/details/0011230310")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("outbound/details/ok/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/details/already_transferred/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/details/already_transferred/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getOutboundDetailsAlreadyTransferred() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/details/0011230310")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("outbound/details/already_transferred/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/outbound/details/not_ready/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/details/not_ready/initial-state.xml", assertionMode = NON_STRICT_UNORDERED)
    void getOutboundDetailsNotReady() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/details/0011230310")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent("outbound/details/not_ready/response.json")))
                .andReturn();

    }

    @Test
    @DatabaseSetup("/outbound/details/invalid_detail_status/initial-state.xml")
    @ExpectedDatabase(value = "/outbound/details/invalid_detail_status/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getOutboundDetailsInvalidDetailStatus() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound/details/0011230310")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent("outbound/details/invalid_detail_status/response.json")))
                .andReturn();

    }
}

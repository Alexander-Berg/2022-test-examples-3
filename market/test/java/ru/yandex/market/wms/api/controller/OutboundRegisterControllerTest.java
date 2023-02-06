package ru.yandex.market.wms.api.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(classes = IntegrationTestConfig.class)
public class OutboundRegisterControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/outbound-registers/interwarehouse/initial-state.xml")
    @ExpectedDatabase(value = "/outbound-registers/interwarehouse/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getInterWarehouseFactOutboundRegister() throws Exception {
        assertGetRegisters(
                "outbound-registers/interwarehouse/response.json",
                status().is2xxSuccessful(),
                "outbound-1243455");
    }

    @Test
    @DatabaseSetup("/outbound-registers/before/getoutbound-registers.xml")
    @ExpectedDatabase(value = "/outbound-registers/before/getoutbound-registers.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getOutboundRegisters() throws Exception {
        assertGetRegisters(
                "outbound-registers/response/getoutbound-registers-response.json",
                status().is2xxSuccessful(),
                "outbound-100200");
    }

    @Test
    @DatabaseSetup("/outbound-registers/before/getoutbound-registers.xml")
    @ExpectedDatabase(value = "/outbound-registers/before/getoutbound-registers.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getNoOutboundRegisters() throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound-registers/outbound-090909")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("[]"))
                .andReturn();
    }

    private void assertGetRegisters(String responseFile, ResultMatcher status, String externOrderKey) throws Exception {
        mockMvc.perform(get("/INFOR_SCPRD_wmwhse1/outbound-registers/" + externOrderKey)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status)
                .andExpect(content().json(getFileContent(responseFile)))
                .andReturn();
    }
}

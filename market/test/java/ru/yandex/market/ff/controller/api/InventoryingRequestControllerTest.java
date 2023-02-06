package ru.yandex.market.ff.controller.api;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональные тесты для {@link InventoryingRequestController}
 */
class InventoryingRequestControllerTest extends MvcIntegrationTest {

    @Test
    @DatabaseSetup("classpath:controller/inventorying/before-create.xml")
    @ExpectedDatabase(value = "classpath:controller/inventorying/after-create-empty-supply.xml",
            assertionMode = NON_STRICT
    )
    void createInventoryingSupply() throws Exception {
        MvcResult mvcResult = mockMvc.perform(put("/inventorying/supply/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("create-supply.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/inventorying/before-create.xml")
    void operLostEnabled() throws Exception {
        mockMvc.perform(put("/inventorying/withdraw/1")
                        .param("type", "OPER_LOST")
                        .param("cycleStartDate", "2021-09-20T10:30:25+03:00")
                        .param("supplierId", "465852")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("classpath:controller/inventorying/oper-lost-disabled.xml")
    void operLostDisabled() throws Exception {
        String content = mockMvc.perform(put("/inventorying/withdraw/1")
                .param("type", "OPER_LOST"))
                .andDo(print())
                .andExpect(status().is5xxServerError())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertions.assertThat(content).contains("Only FIX_LOST type is supported");
    }

    @Test
    @DatabaseSetup("classpath:controller/inventorying/before-create.xml")
    @ExpectedDatabase(value = "classpath:controller/inventorying/after-create-empty-withdraw-fix-lost-1P.xml",
            assertionMode = NON_STRICT
    )
    void createInventoryingWithdrawFixLost1P() throws Exception {
        MvcResult mvcResult = mockMvc.perform(put("/inventorying/withdraw/1")
                .param("type", "FIX_LOST")
                .param("cycleStartDate", "2021-09-20T10:30:25+03:00")
                .param("supplierId", "465852")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("create-withdraw-fix-lost-1P.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/inventorying/before-create.xml")
    @ExpectedDatabase(value = "classpath:controller/inventorying/after-create-empty-withdraw-fix-lost-3P.xml",
            assertionMode = NON_STRICT
    )
    void createInventoryingWithdrawFixLost3P() throws Exception {
        MvcResult mvcResult = mockMvc.perform(put("/inventorying/withdraw/1")
                .param("type", "FIX_LOST")
                .param("cycleStartDate", "2021-09-20T10:30:25+03:00")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("create-withdraw-fix-lost-3P.json", mvcResult);
    }

    @Test
    void createInventoryingWithdrawFixLostWithoutTypeParam() throws Exception {
        mockMvc.perform(put("/inventorying/withdraw/1"))
                .andDo(print())
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    void createInventoryingWithdrawFixLostWithoutcycleStartDateKeyParam() throws Exception {
        mockMvc.perform(put("/inventorying/withdraw/1")
                        .param("type", "FIX_LOST")
                )
                .andDo(print())
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(FileContentUtils.getFileContent("controller/inventorying/" + filename),
                response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}

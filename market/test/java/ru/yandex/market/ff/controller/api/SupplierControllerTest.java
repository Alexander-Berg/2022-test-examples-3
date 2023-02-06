package ru.yandex.market.ff.controller.api;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SupplierControllerTest extends MvcIntegrationTest {

    @Test
    @DatabaseSetup("classpath:controller/supplier/before-suppliers-with-first-finished-inbound-info.xml")
    @ExpectedDatabase(value = "classpath:controller/supplier/before-suppliers-with-first-finished-inbound-info.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void getSuppliersWithFirstFinishedInboundInfo() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get("/suppliers/suppliers-with-first-finished-inbound-info")
        ).andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("suppliers-with-finished-inbound-info-response.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/supplier/before-suppliers-with-first-finished-inbound-info.xml")
    @ExpectedDatabase(value = "classpath:controller/supplier/before-suppliers-with-first-finished-inbound-info.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void getSuppliersHavingAtLeastOneSupply() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get("/suppliers/suppliers-having-at-least-one-supply")
        ).andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("suppliers-having-at-least-one-supply.json", mvcResult);
    }


    @Test
    @DatabaseSetup("classpath:controller/supplier/has-validated-shadow-withdraw/before.xml")
    public void hasValidatedShadowWithdrawTrue() throws Exception {
        mockMvc.perform(
                get("/suppliers/1/has-validated-shadow-withdraw")
        )
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andReturn();
    }

    @Test
    @DatabaseSetup("classpath:controller/supplier/has-validated-shadow-withdraw/before.xml")
    public void hasValidatedShadowWithdrawFalse() throws Exception {
        mockMvc.perform(
                get("/suppliers/2/has-validated-shadow-withdraw")
        )
                .andExpect(status().isOk())
                .andExpect(content().string("false"))
                .andReturn();
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(FileContentUtils.getFileContent("controller/supplier/" + filename),
                response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}

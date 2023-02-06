package ru.yandex.market.wms.receiving.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UITControllerTest extends ReceivingIntegrationTest {

    @Test
    @DatabaseSetup("/controller/cancel-uit-controller/before-cancel-uit.xml")
    @ExpectedDatabase(value = "/controller/cancel-uit-controller/after-cancel-uit.xml", assertionMode = NON_STRICT)
    public void cancelUIT() throws Exception {
        mockMvc.perform(delete("/uit/delete/0120000097"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup("/controller/cancel-uit-controller/before-cancel-uit.xml")
    @ExpectedDatabase(value = "/controller/cancel-uit-controller/after-cancel-uit-no-change.xml",
            assertionMode = NON_STRICT)
    public void cancelNotExistingUIT() throws Exception {
        mockMvc.perform(delete("/uit/delete/0120000099"))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup("/controller/cancel-uit-controller/before-cancel-uit-receipt-status-15.xml")
    public void cancelUITWithReceiptStatusNotInReceiving() throws Exception {
        mockMvc.perform(delete("/uit/delete/0120000097"))
                .andExpect(status().is4xxClientError())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup("/controller/cancel-uit-controller/before-cancel-uit-single.xml")
    @ExpectedDatabase(value = "/controller/cancel-uit-controller/after-cancel-uit-single.xml",
            assertionMode = NON_STRICT)
    public void cancelSingleUIT() throws Exception {
        mockMvc.perform(delete("/uit/delete/0120000097"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}

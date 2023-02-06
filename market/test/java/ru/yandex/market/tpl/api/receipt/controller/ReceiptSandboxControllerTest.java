package ru.yandex.market.tpl.api.receipt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ru.yandex.market.tpl.api.ReceiptServiceShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author valter
 */
@WebLayerTest(ReceiptSandboxController.class)
class ReceiptSandboxControllerTest extends ReceiptServiceShallowTest {

    @Test
    void hiSandbox() throws Exception {
        mockMvc.perform(
                get("/api/receipt-service/sandbox/hi")
                        .with(httpBasic("111", "P5u7~n38"))
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("" +
                        "hi, 111 you're in sandbox"
                ));
    }

    @Test
    void hiSandboxWrongUser() throws Exception {
        mockMvc.perform(
                get("/api/receipt-service/sandbox/hi")
                        .with(httpBasic("1112", "P5u7~n38"))
        )
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

}

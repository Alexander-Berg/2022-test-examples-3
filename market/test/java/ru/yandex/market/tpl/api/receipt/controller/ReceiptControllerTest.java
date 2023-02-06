package ru.yandex.market.tpl.api.receipt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import ru.yandex.market.tpl.api.ReceiptServiceShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author valter
 */
@WebLayerTest(ReceiptController.class)
class ReceiptControllerTest extends ReceiptServiceShallowTest {

    @MockBean
    private ReceiptService receiptService;

    @Test
    void hi() throws Exception {
        mockMvc.perform(
                get("/api/receipt-service/hi")
                        .with(httpBasic("1", "1"))
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("" +
                        "hi, 1"
                ));
    }

    @Test
    void hiWrongUser() throws Exception {
        mockMvc.perform(
                get("/api/receipt-service/hi")
                        .with(httpBasic("111", "P5u7~n38"))
        )
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

}

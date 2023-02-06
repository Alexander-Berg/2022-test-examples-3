package ru.yandex.market.fulfillment.stockstorage.security.tvm;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.configuration.SecurityTestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityTestConfig.class)
public class TvmSecurityTestWithDisabledTvm extends AbstractContextualTest {

    @Test
    public void testRequestIsSuccessfulForUnsecuredHealthMethod() throws Exception {
        mockMvc.perform(get("/pageMatch"))
                .andExpect(status().is2xxSuccessful());
    }
}

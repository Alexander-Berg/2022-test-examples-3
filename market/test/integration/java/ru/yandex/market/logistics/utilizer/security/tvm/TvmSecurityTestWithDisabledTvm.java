package ru.yandex.market.logistics.utilizer.security.tvm;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.config.SecurityTestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityTestConfig.class)
public class TvmSecurityTestWithDisabledTvm extends AbstractContextualTest {

    @Test
    @DatabaseSetup(value = "classpath:fixtures/controller/support/finalize-cycle/before.xml")
    public void testRequestIsSuccessfulForUnsecuredHealthMethod() throws Exception {
        mockMvc.perform(put("/support/finalize-cycle/100500"))
                .andExpect(status().isOk());
    }
}

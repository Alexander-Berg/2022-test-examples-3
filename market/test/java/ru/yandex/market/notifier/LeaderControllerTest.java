package ru.yandex.market.notifier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.yandex.market.notifier.application.AbstractWebTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
public class LeaderControllerTest extends AbstractWebTestBase {

    @Test
    public void shouldReturnLeaderId() throws Exception {
        mockMvc.perform(get("/leader-info/1"))
                .andDo(log())
                .andExpect(status().isOk());
    }
}

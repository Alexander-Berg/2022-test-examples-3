package ru.yandex.market.wms.datacreator.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.datacreator.config.DataCreatorIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UitControllerTest extends DataCreatorIntegrationTest {

    @Test
    @DatabaseSetup(value = "/dao/uit/before.xml", connection = "wmwhse1Connection")
    public void getUitByLocAndLotTest() throws Exception {

        String body = mockMvc.perform(
                get("/uit?loc=loc1&lot=1111")
                        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Assertions.assertEquals("3333", body);
    }
}

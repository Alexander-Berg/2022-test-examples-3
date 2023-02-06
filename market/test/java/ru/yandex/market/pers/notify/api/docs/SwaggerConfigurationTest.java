package ru.yandex.market.pers.notify.api.docs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by dinyat on 15/03/2017.
 */
public class SwaggerConfigurationTest extends MarketUtilsMockedDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testV2ApiDocs() throws Exception {
        String result = this.mockMvc.perform(get("/v2/api-docs")
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Assertions.assertTrue(result.startsWith("{\"swagger\":"));
    }

    @Test
    public void testSwaggerUi() throws Exception {
        this.mockMvc.perform(get("/swagger-ui.html")
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());
    }

}

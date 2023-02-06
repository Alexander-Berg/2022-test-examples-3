package ru.yandex.market.checkout.pushapi.pumkin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.pushapi.pumpkin.config.PumpkinConfig;
import ru.yandex.market.checkout.pushapi.pumpkin.controllers.PumpkinController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author: aproskriakov
 */
@WebMvcTest
@ContextConfiguration(classes = {
        PumpkinConfig.class,
        PumpkinController.class
})
public class PumpkinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testPumpkinCart() throws Exception {
        String reqBody = "<cart currency=\"RUR\">\n" +
                "    <items>\n" +
                "        <item feed-id=\"200305173\" offer-id=\"4\" count=\"1\"/>\n" +
                "    </items>\n" +
                "    <delivery region-id=\"2\">\n" +
                "        <address country=\"Русь\" postcode=\"131488\" city=\"Питер\" " +
                "subway=\"Петровско-Разумовская\" street=\"Победы\" house=\"13\" block=\"666\" floor=\"8\"/>\n" +
                "    </delivery>" +
                "</cart>";
        String resBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<cart shop-admin=\"false\" delivery-currency=\"RUR\">\n" +
                "    <items>\n" +
                "        <item feed-id=\"200305173\" offer-id=\"4\" subsidy=\"0\" delivery=\"false\" count=\"1\"/>\n" +
                "    </items>\n" +
                "</cart>";

        mockMvc.perform(
                post("/pumpkin/shops/12/cart")
                        .content(reqBody)
                        .contentType(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().xml(resBody));
    }
}

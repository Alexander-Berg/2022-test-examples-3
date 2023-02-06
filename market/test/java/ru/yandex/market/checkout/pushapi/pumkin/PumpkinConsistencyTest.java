package ru.yandex.market.checkout.pushapi.pumkin;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.CartXmlDeserializer;
import ru.yandex.market.checkout.pushapi.client.xml.DeliveryXmlDeserializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.CartItemXmlDeserializer;
import ru.yandex.market.checkout.pushapi.pumpkin.controllers.PumpkinController;
import ru.yandex.market.checkout.pushapi.service.shop.ApiService;
import ru.yandex.market.checkout.pushapi.service.shop.ValidateService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * @author: aproskriakov
 */
@ContextConfiguration(classes = {
        PumpkinController.class
})
public class PumpkinConsistencyTest extends AbstractWebTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiService apiService;

    @MockBean
    private ValidateService validateService;

    @Test
    public void testCartConsistency() throws Exception {
        CartXmlDeserializer deserializer = new CartXmlDeserializer();
        deserializer.setCartItemXmlDeserializer(new CartItemXmlDeserializer());
        deserializer.setDeliveryXmlDeserializer(new DeliveryXmlDeserializer());
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
        String expResult = resBody.replaceAll("[\\n\\t ]", "");
        when(apiService.cart(any(), anyLong(), any(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(
                        CartResponse.pumpkin(XmlTestUtil.deserialize(deserializer, reqBody))));
        String endpoint = "/shops/12/cart";

        String pumpkinResult = mockMvc.perform(
                        post("/pumpkin" + endpoint)
                                .content(reqBody)
                                .contentType(MediaType.APPLICATION_XML)
                                .accept(MediaType.APPLICATION_XML))
                .andReturn().getResponse().getContentAsString().replaceAll("[\\n\\t ]", "");

        var result = mockMvc.perform(
                        post(endpoint)
                                .content(reqBody)
                                .contentType(MediaType.APPLICATION_XML)
                                .accept(MediaType.APPLICATION_XML))
                .andExpect(request().asyncStarted())
                .andReturn();
        String mainAppResult = mockMvc.perform(asyncDispatch(result))
                .andReturn().getResponse().getContentAsString().replaceAll("[\\n\\t ]", "");

        assertEquals(pumpkinResult, expResult);
        assertEquals(mainAppResult, expResult);
    }
}

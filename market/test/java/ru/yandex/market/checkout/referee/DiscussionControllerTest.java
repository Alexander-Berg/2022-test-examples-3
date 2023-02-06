package ru.yandex.market.checkout.referee;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.checkout.common.xml.ClassMappingXmlSerializer;
import ru.yandex.market.checkout.common.xml.SimpleXmlWriter;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.entity.ShopStatistic;
import ru.yandex.market.checkout.referee.controller.DiscussionController;
import ru.yandex.market.checkout.referee.test.BaseConversationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.referee.WebParams.ORDER;
import static ru.yandex.market.checkout.referee.WebParams.ROLE;
import static ru.yandex.market.checkout.referee.WebParams.SHOP;
import static ru.yandex.market.checkout.referee.WebParams.TITLE;
import static ru.yandex.market.checkout.referee.WebParams.UID;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertConv;
import static ru.yandex.market.checkout.referee.test.BaseTest.newOrderId;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;

/**
 * @author kukabara
 * @see DiscussionController
 */
public class DiscussionControllerTest extends BaseConversationTest {
    private static final Long TEST_UID = 23412435L;
    @Autowired
    protected HttpMessageConverter checkoutRefereeAnnotationJsonMessageConverter;

    @Autowired
    private ClassMappingXmlSerializer checkoutRefereeXmlSerializer;

    @Autowired
    @Qualifier("checkoutRefereeAnnotationJsonMessageConverter")
    MappingJackson2HttpMessageConverter converter;

    /**
     * {@link ru.yandex.market.checkout.referee.controller.HealthController#ping}
     */
    @Test
    public void testPing() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain; charset=UTF-8"))
                .andExpect(content().string("0;OK\n"));
    }

    private String serializeXml(Object object) throws IOException {
        final StringWriter stringWriter = new StringWriter();
        checkoutRefereeXmlSerializer.serializeXml(object, new SimpleXmlWriter(stringWriter));
        String xml = stringWriter.toString();
        System.out.println("XML = " + xml);
        return xml;
    }

    private String writeJson(Object object) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        checkoutRefereeAnnotationJsonMessageConverter.write(object, MediaType.APPLICATION_JSON_UTF8, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    /**
     * {@link DiscussionController#shopStatistic
     */
    @Test
    public void testShopStat() throws Exception {
        ShopStatistic shopStatistic = client.shopStat(TEST_UID, RefereeRole.SHOP, 774L);
        assertNotNull(shopStatistic);

        mockMvc.perform(
                get("/arbitrage/conversations/shop-stat.json")
                        .params(getParamsForShop())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(writeJson(shopStatistic)));

        mockMvc.perform(
                get("/arbitrage/conversations/shop-stat")
                        .params(getParamsForShop())
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().string(serializeXml(shopStatistic)));
    }

    private MultiValueMap<String, String> getParamsForShop() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(WebParams.SHOP, "774");
        params.add(WebParams.UID, String.valueOf(TEST_UID));
        params.add(WebParams.ROLE, RefereeRole.SHOP.name());
        return params;
    }

    private Conversation readConversation(MvcResult mvcResult) throws Exception {
        String content = mvcResult.getResponse().getContentAsString();
        return (Conversation) checkoutRefereeAnnotationJsonMessageConverter.read(Conversation.class,
                new MockHttpInputMessage(content.getBytes()));
    }

    /**
     * {@link DiscussionController#start}
     */
    @Test
    public void testStart() throws Exception {
        String title = "Title with spaces!";
        String text = "Text\nMultiline with spaces!";

        MvcResult mvcResult = mockMvc.perform(post("/arbitrage/conversations/start.json")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .param(ROLE, RefereeRole.USER.name())
                .param(SHOP, String.valueOf(newUID()))
                .param(UID, String.valueOf(newUID()))
                .param(ORDER, String.valueOf(newOrderId()))
                .param(TITLE, title)
                .content(text)
        ).andReturn();

        if (mvcResult.getResponse().getStatus() == HttpServletResponse.SC_OK) {
            Conversation conv1 = readConversation(mvcResult);
            assertConv(conv1);
            assertEquals(title, conv1.getTitle());
        } else if (mvcResult.getResponse().getStatus() != HttpServletResponse.SC_FORBIDDEN) {
            fail();
        }
    }

}

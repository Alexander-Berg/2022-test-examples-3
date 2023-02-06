package ru.yandex.market.logistic.gateway.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.utils.MockServerUtils;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PushReferenceItemsTest extends AbstractIntegrationTest {

    private static final String PUSH_REFERENCE_ITEMS = "/push-api/reference-items";

    @Autowired
    private HttpTemplateImpl irisHttpTemplate;

    private MockRestServiceServer mockIrisServer;

    @Before
    public void setup() {
        mockIrisServer = MockServerUtils.createMockRestServiceServer(irisHttpTemplate.getRestTemplate());
    }

    @After
    public void tearDown() {
        mockIrisServer.verify();
    }

    @Test
    public void positiveScenario() throws Exception {
        prepareMockServerJsonScenario(mockIrisServer,
            once(),
            irisHost + PUSH_REFERENCE_ITEMS,
            "fixtures/request/fulfillment/push_reference_items/iris_push_reference_items.json",
            null);

        String xmlResponse = mockMvc.perform(post("/fulfillment/query-gateway")
            .contentType(MediaType.TEXT_XML)
            .content(getFileContent("fixtures/request/fulfillment/push_reference_items/fulfillment_push_reference_items.xml")))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_reference_items/fulfillment_push_reference_items.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    @Test
    public void anyAcceptHeaderReturnsXml() throws Exception {
        mockMvc.perform(post("/fulfillment/query-gateway")
            .contentType(MediaType.TEXT_XML)
            .accept(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/fulfillment/push_reference_items/fulfillment_push_reference_items.xml")))
            .andExpect(content().contentType(MediaType.TEXT_XML));
    }
}

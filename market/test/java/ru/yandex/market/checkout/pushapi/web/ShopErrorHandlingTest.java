package ru.yandex.market.checkout.pushapi.web;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

public class ShopErrorHandlingTest extends AbstractShopWebTestBase {

    public ShopErrorHandlingTest() {
        super(DataType.JSON);
    }

    @Test
    public void shouldReturn422If404WasReturnedByTargetShop() throws Exception {

        performCart()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.xpath("/error/code").string("HTTP"));
    }

    @Test
    public void shouldReturn422If500WasReturnedByShop() throws Exception {
        shopadminStubMock.stubFor(
                stubCart()
                        .willReturn(responseDefinition()
                                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .withStatusMessage("FAILURE")
                        )
        );

        performCart()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.xpath("/error/code").string(ErrorSubCode.HTTP.name()));
    }

    @Test
    public void shouldReturn422IfInvalidResponse() throws Exception {
        shopadminStubMock.stubFor(stubCart().willReturn(responseDefinition()
                .withStatus(200)
                .withBody("asdasd")
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));

        performCart()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(xpath("/error/code").string(ErrorSubCode.CANT_PARSE_RESPONSE.name()));
    }

    @Test
    public void shouldReturn422IfResponseTimeout() throws Exception {
        shopadminStubMock.stubFor(stubCart().willReturn(responseDefinition()
                .withFixedDelay(20000)));

        performCartTimeout()
                .andDo(log())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(xpath("/error/code").string(ErrorSubCode.HTTP.name()))
                .andExpect(xpath("/error/message").string(containsString("422")));
    }

    @Test
    public void shouldFailIfReturnedMoreThan10MB() throws Exception {
        shopadminStubMock.stubFor(stubCart().willReturn(responseDefinition()
                .withStatus(HttpStatus.OK.value())
                .withBody(RandomStringUtils.randomAscii(10 * 1024 * 1024))
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));

        performCart()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(xpath("/error/code").string(ErrorSubCode.HTTP.name()));
    }

    private MappingBuilder stubCart() {
        return post(
                urlPathEqualTo("/svn-shop/" + SHOP_ID + "/cart")
        );
    }

    private ResultActions performCart() throws Exception {
        var result = performCart(SHOP_ID);
        return mockMvc.perform(asyncDispatch(result));
    }

    private ResultActions performCartTimeout() throws Exception {
        var result = performCart(SHOP_ID);
        result.getRequest().getAsyncContext().setTimeout(30000);
        result.getAsyncResult();
        return mockMvc.perform(asyncDispatch(result));
    }

    private MvcResult performCart(long shopId) throws Exception {
        RequestContextHolder.createNewContext();
        var settings = new Settings(
                "http://localhost:" + shopadminStubMock.port() + "/svn-shop/" + SHOP_ID,
                "asdasd", dataType, AuthType.HEADER, false
        );
        mockPostSettings(shopId, settings);
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/shops/{shopId}/cart", shopId)
                        .content("<cart currency=\"RUR\">\n" +
                                "  <items>\n" +
                                "    <item feed-id=\"200305173\" offer-id=\"4\" feed-category-id=\"{{feedcategory}}\"" +
                                " " +
                                "offer-name=\"{{offername}}\" count=\"1\"/>\n" +
                                "  </items>\n" +
                                "    <delivery region-id=\"2\">\n" +
                                "      <address country=\"Русь\" postcode=\"131488\" city=\"Питер\" " +
                                "subway=\"Петровско-Разумовская\" street=\"Победы\" house=\"13\" block=\"666\" " +
                                "floor=\"8\"/>\n" +
                                "  </delivery>\n" +
                                "</cart>")
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(request().asyncStarted())
                .andReturn();
        return result;
    }
}

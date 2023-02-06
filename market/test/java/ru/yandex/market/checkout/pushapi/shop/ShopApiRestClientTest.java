package ru.yandex.market.checkout.pushapi.shop;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.common.json.jackson.JacksonMessageConverter;
import ru.yandex.market.checkout.common.xml.NewClassMappingXmlMessageConverter;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.config.AsyncEndpointsExperimentService;
import ru.yandex.market.checkout.pushapi.config.AsyncRestConfig;
import ru.yandex.market.checkout.pushapi.config.async.CustomHttpComponentsAsyncClientHttpRequestFactory;
import ru.yandex.market.checkout.pushapi.service.shop.RequestContext;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class ShopApiRestClientTest extends AbstractWebTestBase {

    @Autowired
    private NewClassMappingXmlMessageConverter shopApiClassMappingXmlMessageConverter;
    @Autowired
    private CustomHttpComponentsAsyncClientHttpRequestFactory thinAsyncHttpRequestFactory;
    @Autowired
    private RestTemplateBuilder restTemplateBuilder;
    @Autowired
    private AsyncEndpointsExperimentService asyncEndpointsExperimentService;

    @Autowired
    private JacksonMessageConverter shopApiClassMappingJsonMessageConverter;

    @Autowired
    private ExecutorService asyncRestTemplateExecutorService;

    private MockRestServiceServer mockServer;
    private ShopApiAsyncRestClient thickJsonShopApiRestClient;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        AsyncRestConfig config = new AsyncRestConfig();
        this.thickJsonShopApiRestClient =
                (ShopApiAsyncRestClient) config.thinJsonShopApiAsyncRestClient(config.thinJsonAsyncRestTemplate(
                                shopApiClassMappingJsonMessageConverter,
                                thinAsyncHttpRequestFactory
                        ),
                        asyncRestTemplateExecutorService
                );
        this.mockServer = MockRestServiceServer.createServer(thickJsonShopApiRestClient.getAsyncRestTemplate());
    }

    @Test
    public void shouldDetect403Exception() throws ExecutionException, InterruptedException {
        mockServer.expect(requestTo("/null"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        var context = mock(RequestContext.class);
        when(context.getShopId()).thenReturn(1L);

        ShopApiResponse<CartResponse> response = thickJsonShopApiRestClient.cart(
                new Settings(null, null, null, null, true),
                new ExternalCart(),
                new ApiSelectorUtil.ApiSelection(null, null, DataType.JSON),
                context
        ).get();

        Assertions.assertTrue(response.isError());
        Assertions.assertEquals(ErrorSubCode.HTTP, response.getErrorSubCode());
        Assertions.assertTrue(response.getException() instanceof HttpClientErrorException);
        Assertions.assertEquals(HttpStatus.FORBIDDEN,
                ((HttpClientErrorException) response.getException()).getStatusCode());
        mockServer.verify();
    }

    @Test
    public void shouldOk() throws ExecutionException, InterruptedException {
        mockServer.expect(requestTo("https://super-mag.ru/cart"))
                .andRespond(withSuccess("{\"cart\":{}}", MediaType.APPLICATION_JSON));


        Settings settings = Settings.builder().urlPrefix("https://super-mag.ru").build();
        ApiSettings apiSettings = ApiSettings.PRODUCTION;
        ApiSelectorUtil.ApiSelection selection = new ApiSelectorUtil().getApiUrl(settings, "/cart", apiSettings
        );
        var context = mock(RequestContext.class);
        when(context.getShopId()).thenReturn(1L);

        ShopApiResponse<CartResponse> response = thickJsonShopApiRestClient.cart(settings,
                new ExternalCart(),
                selection,
                context).get();

        Assertions.assertFalse(response.isError());
        mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://my.yandex-team.ru", "https://yandex-team.ru", "https://yandex.tld", "https" +
            "://yandex.tld:443"})
    public void shouldDetectBlackListHost(String host) throws ExecutionException, InterruptedException {
        mockServer.expect(requestTo(host + "/cart"))
                .andRespond(withSuccess("<cart/>", MediaType.APPLICATION_XML));

        Settings settings = Settings.builder().urlPrefix(host).build();
        ApiSettings apiSettings = ApiSettings.PRODUCTION;
        ApiSelectorUtil.ApiSelection selection = new ApiSelectorUtil().getApiUrl(settings, "/cart", apiSettings
        );
        RequestContext context = mock(RequestContext.class);
        when(context.getShopId()).thenReturn(1L);

        try {
            var result = thickJsonShopApiRestClient.cart(settings,
                    new ExternalCart(),
                    selection,
                    context).get();
            Assertions.assertEquals(result, null);
        } catch (Exception exception) {
            Assertions.assertTrue(exception instanceof HttpClientErrorException);
            Assertions.assertSame(((HttpClientErrorException) exception).getStatusCode(), HttpStatus.FORBIDDEN);
        }
    }

    @Test
    public void shouldDetectWithRedirect() throws ExecutionException, InterruptedException {
        mockServer.expect(requestTo("https://super-mag.ru/cart"))
                .andRespond(request -> {
                    MockClientHttpResponse response = new MockClientHttpResponse(new byte[]{},
                            HttpStatus.MOVED_PERMANENTLY);
                    response.getHeaders().add(LOCATION, "https://super-mag.ru/api/cart");
                    return response;
                });

        Settings settings = Settings.builder().urlPrefix("https://super-mag.ru").build();
        ApiSettings apiSettings = ApiSettings.PRODUCTION;
        ApiSelectorUtil.ApiSelection selection = new ApiSelectorUtil().getApiUrl(settings, "/cart", apiSettings
        );
        RequestContext context = mock(RequestContext.class);
        when(context.getShopId()).thenReturn(1L);
        ShopApiResponse<CartResponse> response = thickJsonShopApiRestClient.cart(settings,
                new ExternalCart(),
                selection, context).get();


        Assertions.assertTrue(response.isError());
        Assertions.assertEquals(ErrorSubCode.HTTP, response.getErrorSubCode());
        Assertions.assertTrue(response.getException() instanceof HttpClientErrorException);
        Assertions.assertEquals(HttpStatus.MOVED_PERMANENTLY,
                ((HttpClientErrorException) response.getException()).getStatusCode());
    }
}

package ru.yandex.market.checkout.pushapi.shop;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.AsyncRestTemplate;

import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.config.async.AsyncRestTemplateHandlers;
import ru.yandex.market.checkout.pushapi.config.async.CustomHttpComponentsAsyncClientHttpRequestFactory;
import ru.yandex.market.checkout.pushapi.config.async.FutureContextAsyncClientInterceptor;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

public class CustomHttpComponentsAsyncClientHttpRequestFactoryTest {

    static {
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    private CustomHttpComponentsAsyncClientHttpRequestFactory httpRequestFactory;

    @BeforeEach
    public void setUp() throws Exception {
        httpRequestFactory = new CustomHttpComponentsAsyncClientHttpRequestFactory();
        httpRequestFactory.setTrustManager(new DynamicTrustManager());
        httpRequestFactory.afterPropertiesSet();
    }

    @Test
    @Disabled
    public void testSsl() throws Exception {
        Settings settings = Settings.builder()
                .partnerInterface(true)
                .fingerprint(
                        Hex.decodeHex("0102".toCharArray())
                ).build();
        ThreadLocalSettings.setSettings(settings);

        final String url2 = "https://api.clockshop.ru:31443";
        final String url1 = "https://compax.ru";

        // only IPv4 server
        final String url =
                "https://vsova.ru/bitrix/services/yandex.market/trading/marketplace/s2/order/status";

        final HttpPost httpGet = new HttpPost(url);
        httpRequestFactory.setConnectTimeout(1000);
        httpRequestFactory.setReadTimeout(1000);
        CompletableFuture<HttpResponse> responseCompletableFuture = new CompletableFuture<>();
        AsyncRestTemplateHandlers asyncRestTemplateHandlers = new AsyncRestTemplateHandlers();
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();
        asyncRestTemplate.setAsyncRequestFactory(httpRequestFactory);
        asyncRestTemplate.setInterceptors(List.of(new FutureContextAsyncClientInterceptor()));

        final HttpBodies httpBodies = new HttpBodies();
        asyncRestTemplate.execute(
                url,
                HttpMethod.POST,
                asyncRestTemplateHandlers.requestCallback(asyncRestTemplate, new ShopOrder(), ShopOrder.class,
                        httpBodies,
                        settings),

                asyncRestTemplateHandlers.responseExtractor(asyncRestTemplate, OrderResponse.class,
                        httpBodies)
        );
        HttpResponse response = responseCompletableFuture.get();
        final InputStream content = response.getEntity().getContent();
        System.out.println(IOUtils.toString(content));
    }

    @Disabled
    @Test
    public void connectionTimeout() throws Exception {
        httpRequestFactory.setConnectTimeout(100);
        httpRequestFactory.setReadTimeout(100);

        final ServerSocket serverSocket = new ServerSocket();
        System.out.println("binding");
        serverSocket.bind(new InetSocketAddress(12345));

        System.out.println("client");

        final Socket client = new Socket();
        client.connect(new InetSocketAddress("localhost", 12345));

        System.out.println("creating");

        final ClientHttpRequest request = httpRequestFactory.createRequest(
                new URI("http://localhost:12345"),
                HttpMethod.GET
        );

        System.out.println("executing");
        final ClientHttpResponse execute = request.execute();
        System.out.println("done");
    }

}

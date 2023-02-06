package ru.yandex.market.checkout.pushapi.shop;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

/**
 * @author msavelyev
 */
public class CustomHttpComponentsClientHttpRequestFactoryTest {

    private CustomHttpComponentsClientHttpRequestFactory httpRequestFactory;

    @Before
    public void setUp() throws Exception {
        httpRequestFactory = new CustomHttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setTrustManager(new DynamicTrustManager());
        httpRequestFactory.afterPropertiesSet();
    }

    //@Test
    public void testSsl() throws Exception {
        ThreadLocalSettings.setSettings(
            new SettingsBuilder().withFingerprint(
                Hex.decodeHex("0102".toCharArray())
            ).build()
        );

//        final String url = "https://api.clockshop.ru:31443";
        final String url = "https://compax.ru";
//        final String url = "https://khv.shop.mts.ru";
        final HttpGet httpGet = new HttpGet(url);
        final HttpResponse response = httpRequestFactory.getHttpClient().execute(httpGet);
        final InputStream content = response.getEntity().getContent();
        System.out.println(IOUtils.toString(content));
    }

    //@Test
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

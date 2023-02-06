package ru.yandex.market.checkout.checkouter.client;

import java.io.IOException;
import java.net.URI;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import ru.yandex.common.util.IOUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SecuredHttpComponentsClientHttpRequestFactoryTest {

    public static final String USERNAME = "username_1";
    public static final String PASSWORD = "password_2";
    public static final String AUTH_VALUE =
            "Basic " + Base64.encodeBase64String((USERNAME + ":" + PASSWORD).getBytes()).trim();
    public static final String PLAIN_CONNECTOR_RESULT = "PLAIN_CONNECTOR_RESULT";
    private TestServer server;

    private SecuredHttpComponentsClientHttpRequestFactory factory;

    @BeforeEach
    public void start() throws Exception {
        server = new TestServer(new MyServlet());
        createFactory();
    }

    @AfterEach
    public void stopServer() throws Exception {
        server.stop();
    }

    @Disabled
    @Test
    public void shouldUseBasicAuth() throws Exception {
        URI requestUri = server.getSecuredUri("/my/test");
        ClientHttpRequest request = factory.createRequest(requestUri, HttpMethod.GET);
        ClientHttpResponse response = request.execute();
        String result = IOUtils.readInputStream(response.getBody());
        assertEquals(AUTH_VALUE, result);
    }

    @Disabled
    @Test
    public void shouldNotUseBasicAuth() throws IOException {
        URI requestUri = server.getPlainUri("/my/test");
        ClientHttpRequest request = factory.createRequest(requestUri, HttpMethod.GET);
        ClientHttpResponse response = request.execute();
        String result = IOUtils.readInputStream(response.getBody());
        assertEquals(PLAIN_CONNECTOR_RESULT, result);
    }

    private void createFactory() throws Exception {
        factory = new SecuredHttpComponentsClientHttpRequestFactory();
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        factory.setTrustStoreFileName(getClass().getResource("test.truststore").getFile());
        factory.setTrustStorePassword("123456");
        factory.afterPropertiesSet();
    }

    @WebServlet(urlPatterns = {"/my/*"})
    public static class MyServlet extends GenericServlet {

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            if ("https".equals(req.getScheme())) {
                String header = ((HttpServletRequest) req).getHeader("Authorization");
                if (header == null) {
                    res.getWriter().write("NONE");
                } else {
                    res.getWriter().write(header);
                }
                return;
            }
            res.getWriter().write(PLAIN_CONNECTOR_RESULT);
        }
    }
}

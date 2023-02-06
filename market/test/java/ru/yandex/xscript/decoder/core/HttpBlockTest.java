package ru.yandex.xscript.decoder.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpHeaders;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.MimeTypeUtils;
import ru.yandex.xscript.decoder.BaseTest;
import ru.yandex.xscript.decoder.configurations.EntityResolverConfiguration;
import ru.yandex.xscript.decoder.resolver.XsltResolver;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
@RunWith(Theories.class)
public class HttpBlockTest extends BaseTest {
    private HttpServer httpServer = createServer();

    public HttpBlockTest() throws IOException {
    }

    private static HttpServer createServer() {
        for (int i = 0; i < 5; i++) {
            try {
                int port;
                try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName("localhost"))) {
                    port = serverSocket.getLocalPort();
                }
                return HttpServer.create(new InetSocketAddress(10000), 0);
            } catch (IOException ignored) {
            }
        }
        throw new RuntimeException("can't start web server");
    }

    @Before
    public void serverStart() throws IOException {
        httpServer.createContext("/root", exchange -> {
            sendResponse(exchange, "<root>" + exchange.getRequestMethod() + "</root>", MimeTypeUtils.TEXT_XML_VALUE);
        });
        httpServer.createContext("/param", exchange -> {
            sendResponse(exchange, "<root>" + exchange.getRequestURI().getQuery() + "</root>", MimeTypeUtils.TEXT_XML_VALUE);
        });
        httpServer.createContext("/json", exchange -> {
            sendResponse(exchange, "{'request':{'query':'" + exchange.getRequestMethod() + "'}}", MimeTypeUtils.APPLICATION_JSON_VALUE);
        });
        httpServer.start();

    }

    private void sendResponse(HttpExchange exchange, String line, String contentType) throws IOException {
        byte[] response = line.getBytes();
        exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    @After
    public void serverStop() {
        httpServer.stop(0);
    }

    @Theory
    public void get(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        assertExpectedTransform("block/http/get.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <root>GET</root>\n" +
                        "</root>"
        );
    }

    @Theory
    public void post(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        assertExpectedTransform("block/http/post.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <root>POST</root>\n" +
                        "</root>"
        );
    }

    @Theory
    public void param(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        assertExpectedTransform("block/http/param.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <root>test=1</root>\n" +
                        "</root>"
        );
    }

    @Theory
    public void async(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        assertExpectedTransform("block/http/async.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <root>POST</root>\n" +
                        "</root>"
        );
    }

    @Theory
    public void asyncTimeout(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        assertExpectedTransform("block/http/async-timeout.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <xscript_invoke_failed error=\"Timeout was reached\" block=\"http\" method=\"post\"/>\n" +
                        "</root>"
        );
    }

    @Theory
    public void asyncMultiple(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        AsyncBasicBlock.initPool(context);
        String[] results = new String[]{"test1=1", "test2=2", "test3=3", "test4=4", "test5=5"};
        long alreadyCompleted = ((ThreadPoolExecutor) AsyncBasicBlock.httpLoaders).getCompletedTaskCount();
        assertLikeExpectedTransform("block/http/async-multiple.xml", results);
        Assert.assertEquals(alreadyCompleted + 5, ((ThreadPoolExecutor) AsyncBasicBlock.httpLoaders).getCompletedTaskCount());
    }

    @Theory
    public void json(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        assertExpectedTransform("block/http/json.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <json type=\"object\"><request><query>GET</query></request></json>\n" +
                        "</root>"
        );
    }

    @Theory
    public void allowEmpty(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("test-key", "test-value");
        assertExpectedTransform("block/http/allow-empty.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <root>test-not-key=</root>\n" +
                        "</root>"
        );
    }

    @Theory
    public void notAllowEmpty(EntityResolverConfiguration configuration) throws Exception {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        ((MockHttpServletRequest) context.getServletRequest()).addParameter("test-key", "test-value");
        assertExpectedTransform("block/http/not-allow-empty.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <root/>\n" +
                        "</root>"
        );
    }

    @Theory
    public void xsltAfterHttp(EntityResolverConfiguration configuration) {
        context = new MockXscriptContext(EntityResolverConfiguration.getResolver(configuration), new XsltResolver());
        context.putState("port", String.valueOf(httpServer.getAddress().getPort()));
        assertExpectedTransform("block/http/xslt-after-http.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root>\n" +
                        "    <res-tag>res-text:\"test-param=1\"</res-tag>\n" +
                        "</root>"
        );
    }
}
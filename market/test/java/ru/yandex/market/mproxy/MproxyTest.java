package ru.yandex.market.mproxy;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;
import ru.yandex.market.mproxy.config.ApplicationProperties;
import ru.yandex.market.mproxy.config.ProxySectionProperties;
import ru.yandex.market.mproxy.config.TcpEndpoint;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MproxyTest {
    private static final String PROPERTIES_FILE_NAME = "mproxy_test.properties";

    static Thread mainThread;
    static String propertiesPath = Resources.getResource(PROPERTIES_FILE_NAME).getPath();

    @BeforeAll
    public static void setUpObjects() {
        System.setProperty("properties", propertiesPath);
        mainThread = new Thread(() -> {
            try {
                ApplicationMain.main(new String[0]);
            } catch (Exception e) {}
        });
        mainThread.start();
    }

    @AfterAll
    public static void stop() {
        mainThread.interrupt();
    }

    @ParameterizedTest
    @MethodSource("getSections")
    public void testSection(ProxySectionProperties section) {
        switch (section.getType()) {
            case TCP_MIRROR_PROXY:
            case HTTP_RPS_BALANCER:
            case TCP_RPS_BALANCER:
            case HTTP_MIRROR_PROXY:
                dumbCheck(section);
        }
    }

    private static Collection<ProxySectionProperties> getSections() {
        return (new ApplicationProperties(propertiesPath)).getProxySections().values();
    }

    private void dumbCheck(ProxySectionProperties section) {
        int port = section.getLocal().getPort();

        ClientAndServer[] clientAndServers = new ClientAndServer[section.getTargetsCount() + 1];
        for (int i = 1; i <= section.getTargetsCount(); i++) {
            TcpEndpoint endpoint = section.getEndpoint(i);
            clientAndServers[i] = ClientAndServer.startClientAndServer(endpoint.getPort());
            new MockServerClient("127.0.0.1", endpoint.getPort())
                .when(request("/")).respond(response("0;OK"));
        }

        try {
            // Waiting for starting mocks and health checks.
            Thread.sleep(60);
            sendRequestAndValidateResponse(port, new DefaultHttpRequest(
                HttpVersion.HTTP_1_0,
                HttpMethod.GET,
                "/"
            ));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new MockServerClient("127.0.0.1", section.getEndpoint(1).getPort()).verify(
           request("/"), VerificationTimes.atLeast(1)
        );

        for (int i = 1; i <= section.getTargetsCount(); i++) {
            clientAndServers[i].stop();
        }

    }

    private void sendRequestAndValidateResponse(int port, HttpRequest request) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();

        Promise<Void> resultPromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        b.group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new HttpClientCodec());
                    p.addLast(new HttpContentDecompressor());
                    p.addLast(new HttpVerificationClientHandler(resultPromise));
                }
            });
        Channel ch = b.connect("127.0.0.1", port).sync().channel();
        assertTrue(ch.writeAndFlush(request).sync().isSuccess());
        resultPromise.await(1000);
        assertTrue(resultPromise.isDone());
        assertNull(resultPromise.cause());
        ch.close();
    }

    private class HttpVerificationClientHandler extends SimpleChannelInboundHandler<HttpObject> {
        Promise<Void> resultPromise;

        public HttpVerificationClientHandler(Promise<Void> resultPromise) {
            this.resultPromise = resultPromise;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
            if (msg instanceof HttpResponse) {
                assertEquals(HttpResponseStatus.OK, ((HttpResponse) msg).status());
            }
            if (msg instanceof HttpContent) {
                assertEquals("0;OK", ((HttpContent) msg).content().toString(Charsets.UTF_8));
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!resultPromise.isDone()) {
                resultPromise.setSuccess(null);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            this.resultPromise.setFailure(cause);
        }
    }
}
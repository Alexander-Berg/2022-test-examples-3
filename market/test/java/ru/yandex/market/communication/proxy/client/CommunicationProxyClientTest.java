package ru.yandex.market.communication.proxy.client;

import java.io.File;
import java.net.URL;
import java.util.Objects;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.SocketUtils;

import ru.yandex.market.communication.proxy.client.model.CallResolution;
import ru.yandex.market.communication.proxy.client.model.CallsFilter;
import ru.yandex.market.communication.proxy.client.model.CallsRequest;
import ru.yandex.market.communication.proxy.client.model.CallsResponse;
import ru.yandex.market.communication.proxy.client.model.CreateRedirectRequest;
import ru.yandex.market.communication.proxy.client.model.CreateRedirectResponse;
import ru.yandex.market.communication.proxy.client.model.LightCreateRedirectRequest;
import ru.yandex.market.communication.proxy.client.model.PageRequest;
import ru.yandex.market.communication.proxy.client.model.RedirectInfoResponse;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class CommunicationProxyClientTest {

    private static WireMockServer wm;
    private static final int PORT = SocketUtils.findAvailableTcpPort();

    private final CommunicationProxyClient client = CommunicationProxyClient.newBuilder()
            .baseUrl("http://localhost:" + PORT)
            .build();


    @BeforeAll
    public static void setup() {
        wm = new WireMockServer(
                options()
                        .port(PORT)
                        .withRootDirectory(Objects.requireNonNull(getClassPathFile("wiremock")).getAbsolutePath())
        );
        wm.start();
    }

    @AfterAll
    public static void tearDown() {
        wm.shutdown();
    }

    @AfterEach
    void after() {
        wm.resetAll();
    }

    @BeforeEach
    void before() {
        wm.resetAll();
    }

    @Test
    void getRedirect() {
        RedirectInfoResponse response = client.getRedirect("+79111234567");

        Assertions.assertEquals("+79115551234", response.getTargetNumber());
    }

    @Test
    void createRedirect() {
        CreateRedirectResponse response = client.createRedirect(
                new CreateRedirectRequest()
                        .orderId(727L)
                        .partnerId(1L)
        );
        Assertions.assertEquals("+79115551234", response.getProxyNumber());
    }

    @Test
    void lightCreateRedirect() {
        CreateRedirectResponse response = client.lightCreateRedirect(
                new LightCreateRedirectRequest()
                        .orderId(727L)
                        .partnerId(1L)
                        .sourcePhoneNumber("+79211231212")
        );
        Assertions.assertEquals("+79115551234", response.getProxyNumber());
    }

    @Test
    void getCalls() {
        CallsResponse response = client.getCalls(
                new CallsRequest()
                        .pageRequest(
                                new PageRequest()
                                        .pageNum(0)
                                        .pageSize(1)
                        )
                        .callsFilter(
                                new CallsFilter()
                                        .orderId(1L)
                                        .addCallResolutionsItem(CallResolution.INVALID_NUMBER)
                        )
        );
        Assertions.assertEquals(CallResolution.INVALID_NUMBER, response.getCalls().get(0).getResolution());
    }

    private static File getClassPathFile(String path) {
        ClassLoader classLoader = CommunicationProxyClient.class.getClassLoader();
        URL url = classLoader.getResource(path);
        if (url == null) {
            return null;
        } else {
            return new File(url.getFile());
        }
    }

}

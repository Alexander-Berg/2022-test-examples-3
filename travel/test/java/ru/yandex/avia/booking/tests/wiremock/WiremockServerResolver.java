package ru.yandex.avia.booking.tests.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

@Slf4j
public class WiremockServerResolver implements ParameterResolver, AfterEachCallback, BeforeAllCallback, AfterAllCallback {
    static final String WIREMOCK_PORT = "wiremock.port";
    static final String WIREMOCK_SERVER_INSTANCE_KEY = "wiremock.server";

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = context.getStore(Namespace.create(WiremockServerResolver.class));
        WireMockServer server = store.get(WIREMOCK_SERVER_INSTANCE_KEY, WireMockServer.class);
        if (server == null || !server.isRunning()) {
            return;
        }
        server.resetAll();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = context.getStore(Namespace.create(WiremockServerResolver.class));
        WireMockServer server = store.get(WIREMOCK_SERVER_INSTANCE_KEY, WireMockServer.class);
        if (server == null || !server.isRunning()) {
            return;
        }
        log.info("Stopping wiremock server on localhost:{}", server.port());
        server.stop();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        WireMockConfiguration configuration = WireMockConfiguration.options()
                .dynamicPort()
                .fileSource(new ClasspathFileSource("."));
        WireMockServer server = new WireMockServer(configuration);
        server.start();
        ExtensionContext.Store store = context.getStore(Namespace.create(WiremockServerResolver.class));
        store.put(WIREMOCK_SERVER_INSTANCE_KEY, server);
        store.put(WIREMOCK_PORT, server.port());
        log.info("Started wiremock server on localhost:{}", server.port());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        return (parameterContext.getParameter().isAnnotationPresent(Wiremock.class) ||
                parameterContext.getParameter().isAnnotationPresent(WiremockUri.class));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        if (parameterContext.getParameter().isAnnotationPresent(Wiremock.class)) {
            return getStore(context).get(WIREMOCK_SERVER_INSTANCE_KEY);
        } else if (parameterContext.getParameter().isAnnotationPresent(WiremockUri.class)) {
            return "http://localhost:" + getStore(context).get(WIREMOCK_PORT);
        }
        return null;
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(WiremockServerResolver.class));
    }
}

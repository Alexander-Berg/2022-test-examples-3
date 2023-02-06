package ru.yandex.market.checkout.common.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public final class WireMockServerFactory {

    private WireMockServerFactory() {
    }

    public static WireMockServer newServer() {
        return new WireMockServer(new WireMockConfiguration().dynamicPort().notifier(new ConsoleNotifier(false)));
    }
}

package ru.yandex.market.ff4shops.util;

import org.springframework.web.util.UriComponentsBuilder;

public class TestUrlBuilder {

    private final int localPort;

    public TestUrlBuilder(int localPort) {
        this.localPort = localPort;
    }

    public String url(String... path) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + localPort)
                .pathSegment(path)
                .toUriString();
    }

}

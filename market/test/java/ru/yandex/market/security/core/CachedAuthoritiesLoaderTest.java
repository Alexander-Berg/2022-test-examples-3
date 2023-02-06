package ru.yandex.market.security.core;

import org.junit.jupiter.api.BeforeEach;

class CachedAuthoritiesLoaderTest extends SimpleAuthoritiesLoaderTest {

    @BeforeEach
    void setUp() {
        KampferFactory factory = new CachedKampferFactory(dataSource, 10);
        loader = new SimpleAuthoritiesLoader();
        loader.setKampferFactory(factory);
    }

}

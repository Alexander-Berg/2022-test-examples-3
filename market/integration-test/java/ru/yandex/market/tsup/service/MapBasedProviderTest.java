package ru.yandex.market.tsup.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.service.provider.MapBasedProvider;

public class MapBasedProviderTest extends AbstractContextualTest {

    @Autowired
    private List<MapBasedProvider> providers;

    @Test
    void checkProviderTypes() {
        providers.forEach(this::eachKeyNoExcept);
    }

    private void eachKeyNoExcept(MapBasedProvider provider) {
        provider.getKeys().forEach(provider::provide);
    }
}

package ru.yandex.market.delivery.transport_manager.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.provider.map.MapBasedProvider;

@SuppressWarnings("rawtypes")
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

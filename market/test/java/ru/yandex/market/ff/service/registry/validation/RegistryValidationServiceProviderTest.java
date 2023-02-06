package ru.yandex.market.ff.service.registry.validation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;

class RegistryValidationServiceProviderTest extends IntegrationTest {

    private static final Set<RequestType> IGNORED_REQUEST_TYPES = EnumSet.of(RequestType.CROSSDOCK);

    @Autowired RegistryValidationServiceProvider provider;

    @Test
    void shouldProvideServicesForAllTheTypes() {
        var exceptions = new ArrayList<Exception>();
        for (RequestType requestType : RequestType.values()) {
            if (IGNORED_REQUEST_TYPES.contains(requestType)) {
                continue;
            }
            try {
                provider.provide(requestType);
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            Assertions.fail(() -> exceptions.stream().map(Exception::getMessage).collect(Collectors.joining("\n")));
        }
    }

}

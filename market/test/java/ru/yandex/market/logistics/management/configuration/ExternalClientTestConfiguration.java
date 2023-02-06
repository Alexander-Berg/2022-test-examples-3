package ru.yandex.market.logistics.management.configuration;

import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.management.util.ExternalClientsUtil;
import ru.yandex.market.request.trace.Module;

public final class ExternalClientTestConfiguration {

    private ExternalClientTestConfiguration() {
        throw new UnsupportedOperationException();
    }

    public static RestTemplate restTemplate(Module module) {
        return ExternalClientsUtil.restTemplate(module);
    }
}

package ru.yandex.market.hrms.core.service.outstaff.stubs;

import java.util.Optional;

import org.springframework.web.client.RestTemplate;

import ru.yandex.market.hrms.core.service.outstaff.client.BlackBoxClient;

public class BlackBoxClientStub extends BlackBoxClient {
    private String value;

    public BlackBoxClientStub(RestTemplate blackBoxRestTemplate) {
        super(blackBoxRestTemplate);
    }

    public void withValue(String newValue) {
        value = newValue;
    }

    @Override
    public Optional<String> getLoginByUid(long ytUid) {
        return Optional.of(value);
    }
}

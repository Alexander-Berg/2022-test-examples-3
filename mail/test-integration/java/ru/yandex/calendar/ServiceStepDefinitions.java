package ru.yandex.calendar;

import io.cucumber.java.en.Given;
import ru.yandex.calendar.support.ServiceStorage;

import javax.inject.Inject;

public class ServiceStepDefinitions {
    @Inject
    ServiceStorage storage;

    @Given("service {string} with tvm id={long}")
    public void serviceMayaWithTvmId(String serviceName, long tvmId) {
        storage.register(serviceName, tvmId);
    }
}

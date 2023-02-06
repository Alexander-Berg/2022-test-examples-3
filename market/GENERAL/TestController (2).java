package ru.yandex.market.logistic.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"testing"})
public class TestController {

    @Value("${test.property}")
    private long testProperty;

    @GetMapping(path = "test-property")
    public long getTestProperty()  {
        return testProperty;
    }
}

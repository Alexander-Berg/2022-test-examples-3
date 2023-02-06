package ru.yandex.market.health.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.health.annotations.ErrorPercent;
import ru.yandex.market.health.annotations.Timing;
import ru.yandex.market.health.context.Period;

@RestController
public class TestController {

    @GetMapping("/test/get")
    public String getProperty() {
        return "";
    }

    @RequestMapping(value = "/test/{property}/name",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.HEAD})
    @Timing(warn = 10000, period = Period.ONE_MIN)
    @ErrorPercent(warn = 10, period = Period.ONE_MIN)
    public String manyRequestMethods(@PathVariable String property) {
        return "";
    }
}

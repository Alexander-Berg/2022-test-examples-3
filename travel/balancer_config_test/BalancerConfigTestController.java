package ru.yandex.travel.externalapi.endpoint.balancer_config_test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping(value = "/balancer_config_test")
@RequiredArgsConstructor
@Slf4j
public class BalancerConfigTestController {

    @RequestMapping(value = "/v1/fail", produces = "application/json")
    public DeferredResult<String> fail() {
        DeferredResult<String> result = new DeferredResult<>();
        result.setErrorResult(new RuntimeException("Failed"));
        return result;
    }

    @RequestMapping(value = "/v1/success", produces = "application/json")
    public DeferredResult<String> success() {
        DeferredResult<String> result = new DeferredResult<>();
        result.setResult(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()));
        return result;
    }
}

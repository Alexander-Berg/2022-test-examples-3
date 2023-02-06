package ru.yandex.travel.api.endpoints.test;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping(value = "/api/balancer_config_test")
@RequiredArgsConstructor
@Slf4j
public class BalancerConfigTest {

    @RequestMapping(value = "/v1/fail", produces = "application/json")
    @ApiOperation("Создание запроса информации о заказах пользователя")
    public DeferredResult<String> fail() {
        DeferredResult<String> result = new DeferredResult<>();
        result.setErrorResult(new RuntimeException("Failed"));
        return result;
    }
}

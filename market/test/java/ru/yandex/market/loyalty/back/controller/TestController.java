package ru.yandex.market.loyalty.back.controller;

import java.util.Optional;

import javax.annotation.security.PermitAll;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.health.annotations.NoHealth;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.core.config.DatabaseUsage;
import ru.yandex.market.loyalty.spring.retry.spring.PgaasNoRetry;

import static ru.yandex.market.loyalty.core.config.DatasourceType.NONE;

@RestController
@RequestMapping("/for/test/")
public class TestController {

    @PermitAll
    @PgaasNoRetry
    @NoHealth
    @DatabaseUsage(NONE)
    @GetMapping("withEnumParamInQuery")
    public String withEnumParamInQuery(@RequestParam("color") MarketPlatform platform) {
        return platform.getCode();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @PermitAll
    @PgaasNoRetry
    @NoHealth
    @DatabaseUsage(NONE)
    @GetMapping({
            "withEnumParamInPath",
            "withEnumParamInPath/{color}"
    })
    public String withEnumParamInPath(@PathVariable(value = "color", required = false) Optional<MarketPlatform> platform) {
        return platform.orElse(MarketPlatform.BLUE).getCode();
    }
}

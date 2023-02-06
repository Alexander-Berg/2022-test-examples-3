package ru.yandex.market.clickphite.config;

import java.io.File;
import java.io.IOException;

import org.assertj.core.api.AbstractThrowableAssert;

import ru.yandex.market.clickphite.config.validation.context.ConfigValidationException;
import ru.yandex.market.clickphite.utils.ResourceUtils;
import ru.yandex.market.health.configs.clickphite.metric.graphite.SplitNotFoundException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailureValidationTestTool {

    private String basePath;
    private final ConfigurationService configurationService;

    FailureValidationTestTool(String basePath) {
        this.basePath = ResourceUtils.getResourcePath(basePath);
        configurationService = new ConfigurationService();
    }

    AbstractThrowableAssert<?, ? extends Throwable> shouldFail(String configName) {
        final AbstractThrowableAssert<?, ? extends Throwable> throwableAssert = assertThatThrownBy(
            () -> validate(configName));
        throwableAssert.isInstanceOf(ConfigValidationException.class);
        return throwableAssert;
    }

    void validate(String configName) {
        try {
            configurationService.parseAndCheck(new ConfigFile(new File(basePath + configName + ".json")));
        } catch (IOException | SplitNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}

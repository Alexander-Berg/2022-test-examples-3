package ru.yandex.canvas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.canvas.configs.CanvasTvmInterceptor;
import ru.yandex.direct.env.Environment;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.tvm.TvmIntegrationStub;

@Configuration
public class CanvasTvmStubConfiguration {
    @Bean
    static EnvironmentType environmentType() {
        return Environment.getCached();
    }

    @Bean
    public CanvasTvmInterceptor tvmInterceptor(EnvironmentType environmentType) {
        return new CanvasTvmInterceptor(new TvmIntegrationStub(), environmentType);
    }
}

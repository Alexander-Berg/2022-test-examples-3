package ru.yandex.market.mbi;

import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import ru.yandex.market.common.test.spring.H2Config;

@Configuration
public class FunctionalTestConfig extends H2Config {
    @Nonnull
    @Override
    protected List<Resource> databaseResources() {
        return List.of();
    }
}

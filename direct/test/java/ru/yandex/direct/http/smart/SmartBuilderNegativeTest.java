package ru.yandex.direct.http.smart;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.http.smart.core.Smart;

import static org.mockito.Mockito.mock;

public class SmartBuilderNegativeTest {

    public Smart.Builder builder;

    @BeforeEach
    public void setUp() {
        builder = Smart.builder().withBaseUrl("https://ya.ru")
                .withProfileName("test")
                .withParallelFetcherFactory(mock(ParallelFetcherFactory.class));
    }

    @Test
    public void withoutBaseUrl() {
        Assertions.assertThatThrownBy(() -> {
            builder.withBaseUrl(null).build();
        }).isInstanceOf(IllegalStateException.class).hasMessageContaining("Base URL required");
    }

    @Test
    public void withoutParallelFetcherFactory() {
        Assertions.assertThatThrownBy(() -> {
            builder.withParallelFetcherFactory(null).build();
        }).isInstanceOf(IllegalStateException.class).hasMessageContaining("parallelFetcherFactory required");
    }

    @Test
    public void incorrectBaseUrl() {
        Assertions.assertThatThrownBy(() -> {
            builder.withBaseUrl("").build();
        }).isInstanceOf(IllegalArgumentException.class);
    }
}

package ru.yandex.market.logistics.iris.configuration;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
public class TvmConfigurationTest extends SoftAssertionSupport {

    @Autowired
    private TvmTicketChecker internalTvmTicketChecker;

    @Test
    @DisplayName("Проверка tvm-валидации внутренних методов")
    public void testInternal() {
        Stream.of(
            Pair.of("/ping", false),
            Pair.of("/partner", true),
            Pair.of("/any", true),
            Pair.of("/internal/admin", true),
            Pair.of("/ad/min", true)
        ).forEach(arguments -> {
            String url = arguments.getLeft();
            boolean needCheck = arguments.getRight();
            assertions().assertThat(internalTvmTicketChecker.isRequestedUrlAcceptable(url))
                .isEqualTo(needCheck);
        });
    }

    @Configuration
    @PropertySource("classpath:application-tvm.properties")
    public static class Config {

        @Bean
        public TvmClientApi tvmClientApiMock() {
            return mock(TvmClientApi.class);
        }

        @Bean
        @ConfigurationProperties("iris.tvm")
        public TvmTicketChecker irisTicketChecker() {
            return new TvmTicketCheckerImpl();
        }
    }
}

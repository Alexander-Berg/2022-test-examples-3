package ru.yandex.market.logistics.cs.config;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;

@Import(TvmConfigTest.Config.class)
public class TvmConfigTest extends AbstractIntegrationTest {

    @Autowired
    private TvmTicketChecker internalTvmTicketChecker;

    @Nonnull
    private static Stream<Arguments> internalSource() {
        return Stream.of(
            Pair.of("/admin/capacity/counters/1", true),
            Pair.of("/ping", false)
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("internalSource")
    @DisplayName("Проверка tvm-валидации внутренних методов")
    void internal(String url, Boolean needCheck) {
        softly.assertThat(internalTvmTicketChecker.isRequestedUrlAcceptable(url))
            .isEqualTo(needCheck);
    }

    @Configuration
    @PropertySource("classpath:application-tvm.properties")
    @EnableConfigurationProperties
    public static class Config {

        @Bean
        @ConfigurationProperties("tvm.internal")
        public TvmTicketChecker internalTvmTicketChecker() {
            return new TvmTicketCheckerImpl();
        }
    }
}

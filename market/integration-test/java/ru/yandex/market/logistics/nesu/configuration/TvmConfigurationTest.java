package ru.yandex.market.logistics.nesu.configuration;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@MockBean({
    TvmClientApi.class,
})
@SpringBootTest(
    classes = TvmConfigurationTest.Config.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "environment=local"
)
class TvmConfigurationTest {

    @InjectSoftAssertions
    protected SoftAssertions softly;
    @Autowired
    private TvmTicketChecker adminTvmTicketChecker;
    @Autowired
    private TvmTicketChecker internalTvmTicketChecker;
    @Autowired
    private TvmTicketChecker backOfficeTvmTicketChecker;

    @Nonnull
    private static Stream<Arguments> adminSource() {
        return Stream.of(
            Pair.of("/partner", false),
            Pair.of("/admin", true),
            Pair.of("/admin/lom/order/1", true),
            Pair.of("/internal/admin", false),
            Pair.of("/ad/min", false),
            Pair.of("/admin/nesu/ping", true)
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @ParameterizedTest(name = AbstractContextualTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("adminSource")
    @DisplayName("Проверка tvm-валидации методов админки")
    void admin(String url, Boolean needCheck) {
        softly.assertThat(adminTvmTicketChecker.isRequestedUrlAcceptable(url))
            .isEqualTo(needCheck);
    }

    @Nonnull
    private static Stream<Arguments> internalSource() {
        return Stream.of(
            Pair.of("/partner", false),
            Pair.of("/any", false),
            Pair.of("/admin/lom/order/1", false),
            Pair.of("/internal/admin", true),
            Pair.of("/any/internal/admin", false),
            Pair.of("/ad/min", false),
            Pair.of("/admin/nesu/ping", false)
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @ParameterizedTest(name = AbstractContextualTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("internalSource")
    @DisplayName("Проверка tvm-валидации внутренних методов")
    void internal(String url, Boolean needCheck) {
        softly.assertThat(internalTvmTicketChecker.isRequestedUrlAcceptable(url))
            .isEqualTo(needCheck);
    }

    @Nonnull
    private static Stream<Arguments> backOfficeSource() {
        return Stream.of(
            Pair.of("/back-office/", true),
            Pair.of("/any", false),
            Pair.of("/any/back-office", false),
            Pair.of("/admin/lom/order/1", false),
            Pair.of("/back-office/admin", true),
            Pair.of("/back/office", false),
            Pair.of("/admin/nesu/ping", false)
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @ParameterizedTest(name = AbstractContextualTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("backOfficeSource")
    @DisplayName("Проверка tvm-валидации back-office методов")
    void backOffice(String url, Boolean needCheck) {
        softly.assertThat(backOfficeTvmTicketChecker.isRequestedUrlAcceptable(url))
            .isEqualTo(needCheck);
    }

    @Configuration
    @PropertySource("classpath:application-tvm.properties")
    @EnableConfigurationProperties
    public static class Config {

        @Bean
        @ConfigurationProperties("tvm.admin")
        public TvmTicketChecker adminTvmTicketChecker() {
            return new TvmTicketCheckerImpl();
        }

        @Bean
        @ConfigurationProperties("tvm.internal")
        public TvmTicketChecker internalTvmTicketChecker() {
            return new TvmTicketCheckerImpl();
        }

        @Bean
        @ConfigurationProperties("tvm.back-office")
        public TvmTicketChecker backOfficeTvmTicketChecker() {
            return new TvmTicketCheckerImpl();
        }

    }
}

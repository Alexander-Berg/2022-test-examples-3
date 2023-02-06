package ru.yandex.market.logistics.management.configuration;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
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

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

@ExtendWith(SpringExtension.class)
@MockBean({
    TvmClientApi.class,
})
@SpringBootTest(
    classes = TvmConfigurationTest.Config.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "log.dir=logs",
        "environment=local",
        "http.port=8080"
    }
)
public class TvmConfigurationTest {

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    private TvmTicketChecker internalTvmTicketChecker;
    @Autowired
    private TvmTicketChecker idmTvmTicketChecker;

    @ParameterizedTest(name = AbstractContextualTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("internalSource")
    @DisplayName("Проверка tvm-валидации внутренних методов")
    public void testInternal(String url, Boolean needCheck) {
        softly.assertThat(internalTvmTicketChecker.isRequestedUrlAcceptable(url))
            .isEqualTo(needCheck);
    }

    @Nonnull
    private static Stream<Arguments> internalSource() {
        return Stream.of(
            Pair.of("/partner", true),
            Pair.of("/any", true),
            Pair.of("/admin/lom/order/1", true),
            Pair.of("/internal/admin", true),
            Pair.of("/ad/min", true),
            Pair.of("/admin/lms/ping", true),
            Pair.of("/idm/info", false),
            Pair.of("/", false)
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @ParameterizedTest(name = AbstractContextualTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("idmSource")
    @DisplayName("Проверка tvm-валидации idm методов")
    public void testIdm(String url, Boolean needCheck) {
        softly.assertThat(idmTvmTicketChecker.isRequestedUrlAcceptable(url))
            .isEqualTo(needCheck);
    }

    @Nonnull
    private static Stream<Arguments> idmSource() {
        return Stream.of(
            Pair.of("/partner", false),
            Pair.of("/any", false),
            Pair.of("/admin/lom/order/1", false),
            Pair.of("/internal/admin", false),
            Pair.of("/ad/min", false),
            Pair.of("/admin/lms/ping", false),
            Pair.of("/idm/info", true),
            Pair.of("/idm/add-role", true),
            Pair.of("/idm/any", true),
            Pair.of("/", false)
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Configuration
    @PropertySource("classpath:application-tvm.properties")
    @EnableConfigurationProperties
    public static class Config {

        @Bean
        @ConfigurationProperties("tvm.idm")
        public TvmTicketChecker idmTvmTicketChecker() {
            return new TvmTicketCheckerImpl();
        }

        @Bean
        @ConfigurationProperties("tvm.internal")
        public TvmTicketChecker internalTvmTicketChecker() {
            return new TvmTicketCheckerImpl();
        }

    }
}

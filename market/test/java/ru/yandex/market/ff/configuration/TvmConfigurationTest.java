package ru.yandex.market.ff.configuration;

import java.util.Arrays;
import java.util.stream.Collectors;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
public class TvmConfigurationTest {

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    private TvmTicketChecker internalTvmTicketChecker;

    @ParameterizedTest(name = "{index} : {0}.")
    @MethodSource("internalSource")
    @DisplayName("Проверка tvm-валидации внутренних методов")
    public void testInternal(String url, Boolean needCheck) {
        softly.assertThat(internalTvmTicketChecker.isRequestedUrlAcceptable(url))
            .isEqualTo(needCheck);
    }

    @Nonnull
    private static Stream<Arguments> internalSource() {
        return Stream.of(
            Pair.of("/any", true),
            Pair.of("/partner", true),
            Pair.of("/internal/admin", true),
            Pair.of("/ad/min", true),
            Pair.of("/idm", true)
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Configuration
    @PropertySource("classpath:servant-tvm.properties")
    public static class Config {

        @Bean
        public TvmClientApi tvmClientApiMock() {
            return mock(TvmClientApi.class);
        }

        @Bean
        public TvmTicketChecker internalTvmTicketChecker(
            @Value("${ffwf.tvm.internal.check-user-ticket:false}") boolean checkUserTicket,
            @Value("${ffwf.tvm.internal.log-only-mode:true}") boolean logOnlyMode,
            @Value("${ffwf.tvm.internal.allowed-service-ids:}") String[] allowedServiceIds,
            @Value("${ffwf.tvm.internal.api-methods}") String[] apiMethods
        ) {
            TvmTicketCheckerImpl internalChecker = new TvmTicketCheckerImpl();
            internalChecker.setCheckUserTicket(checkUserTicket);
            internalChecker.setAllowedServiceIds(
                Arrays.stream(allowedServiceIds).map(Integer::valueOf).collect(Collectors.toSet())
            );
            internalChecker.setApiMethods(Arrays.stream(apiMethods).collect(Collectors.toSet()));
            internalChecker.setLogOnlyMode(logOnlyMode);
            return internalChecker;
        }

    }
}

package ru.yandex.market.logistics.management.util.tvm;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.util.client.tvm.TvmAuthenticationToken;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static ru.yandex.market.logistics.management.util.tvm.TvmInfoRequestContextInjector.TVM_SERVICE_ID_KEY;

class TvmInfoRequestContextInjectorTest extends AbstractTest {

    private final TvmInfoRequestContextInjector tvmInfoRequestContextInjector = new TvmInfoRequestContextInjector();

    @BeforeEach
    void setUp() {
        RequestContextHolder.createContext("");
    }

    @AfterAll
    static void afterAll() {
        RequestContextHolder.clearContext();
    }

    @Test
    void isRequestedUrlAcceptableTest() {
        assertThat(tvmInfoRequestContextInjector.isRequestedUrlAcceptable("any string")).isEqualTo(true);
    }

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "]")
    @MethodSource("tvmServiceIdSource")
    @DisplayName("Проверка устанавливаемых значений")
    void checkServiceTicketTest(Integer tokenId, Object expectedId) {
        TvmAuthenticationToken token = new TvmAuthenticationToken("", "", "", "");
        token.setTvmServiceId(tokenId);
        tvmInfoRequestContextInjector.checkServiceTicket(token);

        var tvmId = Optional.of(RequestContextHolder.getContext())
            .map(RequestContext::getAppData)
            .map(map -> map.get(TVM_SERVICE_ID_KEY))
            .orElse(null);

        assertThat(tvmId).isEqualTo(expectedId);
    }

    @Test
    void isLogOnlyModeTest() {
        assertThat(tvmInfoRequestContextInjector.isLogOnlyMode()).isEqualTo(false);
    }

    private static Stream<Arguments> tvmServiceIdSource() {
        return Stream.of(
            Arguments.of(444, 444L),
            Arguments.of(0, null)
        );
    }
}

package ru.yandex.market.logistics.management.util.tvm;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static ru.yandex.market.logistics.management.util.tvm.TvmInfoRequestContextInjector.TVM_SERVICE_ID_KEY;

class TvmInfoRequestContextExtractorTest extends AbstractTest {

    private final TvmInfoRequestContextExtractor tvmInfoRequestContextExtractor = new TvmInfoRequestContextExtractor();

    @BeforeEach
    void setUp() {
        RequestContextHolder.createContext("");
    }

    @AfterAll
    static void afterAll() {
        RequestContextHolder.clearContext();
    }

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "]")
    @MethodSource
    @DisplayName("Проверка доставаемых значений")
    void testTvmServiceIdHasBeenSetCorrectly(String key, Object value, Long expected) {
        RequestContext context = RequestContextHolder.getContext();
        Map<String, Object> appData = Map.of(key, value);
        context.setAppData(appData);
        assertThat(tvmInfoRequestContextExtractor.getTvmServiceId()).isEqualTo(expected);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testTvmServiceIdHasBeenSetCorrectly() {
        return Stream.of(
            Arguments.of(TVM_SERVICE_ID_KEY, 444L, 444L),
            Arguments.of(TVM_SERVICE_ID_KEY, "not a number", null),
            Arguments.of(TVM_SERVICE_ID_KEY, 444, null),
            Arguments.of("wrong key", 444, null)
        );
    }
}

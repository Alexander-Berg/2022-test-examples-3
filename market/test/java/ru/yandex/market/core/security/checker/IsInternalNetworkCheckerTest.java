package ru.yandex.market.core.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.parameters.ParametersSourceImpl;
import ru.yandex.market.security.model.Authority;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IsInternalNetworkCheckerTest {

    private IsInternalNetworkChecker isInternalNetworkChecker = new IsInternalNetworkChecker();

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void test(boolean isInternalNetwork, boolean expected) {
        assertEquals(expected, isInternalNetworkChecker.checkTyped(new ParametersSourceImpl(Pair.of("isInternalNetwork",
                Boolean.toString(isInternalNetwork))), new Authority()));
    }
}

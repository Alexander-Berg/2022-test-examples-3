package ru.yandex.market.core.feed.model;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.feed.assortment.model.FeedProcessingResult;

/**
 * @author fbokovikov
 */
class FeedProcessingResultTest {

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(0, FeedProcessingResult.OK),
                Arguments.of(1, FeedProcessingResult.WARNING),
                Arguments.of(2, FeedProcessingResult.WARNING),
                Arguments.of(3, FeedProcessingResult.ERROR),
                Arguments.of(4, FeedProcessingResult.ERROR),
                Arguments.of(103, FeedProcessingResult.ERROR),
                Arguments.of(100, FeedProcessingResult.UNKNOWN)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void byIndexerCode(int indexerCode, FeedProcessingResult expected) {
        Assertions.assertEquals(
                expected,
                FeedProcessingResult.byIndexerCode(indexerCode)
        );
    }
}

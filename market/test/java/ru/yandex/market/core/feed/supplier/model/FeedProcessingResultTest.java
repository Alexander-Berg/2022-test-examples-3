package ru.yandex.market.core.feed.supplier.model;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.feed.assortment.model.FeedProcessingResult;
import ru.yandex.market.core.feed.validation.model.FeedValidationResult;

/**
 * @author fbokovikov
 */
class FeedProcessingResultTest {

    /**
     * Код, возвращенный индексатором, корректно конвертируется в {@link FeedProcessingResult}.
     */
    @ParameterizedTest
    @MethodSource("testIndexerIntCodeConversionData")
    void testIndexerIntCodeConversion(int indexerCode, FeedProcessingResult expectedResult) {
        Assertions.assertEquals(expectedResult, FeedProcessingResult.byIndexerCode(indexerCode));
    }

    private static Stream<Arguments> testIndexerIntCodeConversionData() {
        return Stream.of(
                Arguments.of(-1, FeedProcessingResult.UNKNOWN),
                Arguments.of(0, FeedProcessingResult.OK),
                Arguments.of(1, FeedProcessingResult.WARNING),
                Arguments.of(2, FeedProcessingResult.WARNING),
                Arguments.of(3, FeedProcessingResult.ERROR),
                Arguments.of(4, FeedProcessingResult.ERROR),
                Arguments.of(5, FeedProcessingResult.UNKNOWN),
                Arguments.of(999, FeedProcessingResult.UNKNOWN)
        );
    }

    /**
     * {@link FeedProcessingResult} корректно формируется из строковых кодов.
     */
    @ParameterizedTest
    @MethodSource("testIndexerStringCodeConversionData")
    void testIndexerStringCodeConversion(String code, FeedProcessingResult expectedResult) {
        Assertions.assertEquals(expectedResult, FeedProcessingResult.of(code));
    }

    private static Stream<Arguments> testIndexerStringCodeConversionData() {
        return Stream.of(
                Arguments.of(null, FeedProcessingResult.UNKNOWN),
                Arguments.of("bad_value", FeedProcessingResult.UNKNOWN),
                Arguments.of(FeedValidationResult.ERROR.name(), FeedProcessingResult.ERROR),
                Arguments.of(FeedValidationResult.WARNING.name(), FeedProcessingResult.WARNING),
                Arguments.of(FeedValidationResult.UNKNOWN.name(), FeedProcessingResult.UNKNOWN),
                Arguments.of(FeedValidationResult.OK.name(), FeedProcessingResult.OK)
        );
    }
}

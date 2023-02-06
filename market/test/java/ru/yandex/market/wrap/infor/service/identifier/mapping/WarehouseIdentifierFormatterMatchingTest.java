package ru.yandex.market.wrap.infor.service.identifier.mapping;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

class WarehouseIdentifierFormatterMatchingTest extends SoftAssertionSupport {

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(null, false),
            Arguments.of("", false),
            Arguments.of("0000000000000000001", false),
            Arguments.of("TST0000000000000000001", false),
            Arguments.of("ROV9223372036854775807BOM1", false),
            Arguments.of("ROV0000000000000000001", true),
            Arguments.of("ROV9223372036854775807", true)
        );
    }

    private final WarehouseIdentifierFormatter warehouseIdentifierFormatter = new WarehouseIdentifierFormatter("ROV");

    @MethodSource("data")
    @ParameterizedTest
    void testMatching(String inputValue, boolean expectedResult) {
        softly.assertThat(warehouseIdentifierFormatter.hasCorrectFormat(inputValue)).isEqualTo(expectedResult);
    }
}

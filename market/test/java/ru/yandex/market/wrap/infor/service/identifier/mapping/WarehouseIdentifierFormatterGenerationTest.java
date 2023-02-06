package ru.yandex.market.wrap.infor.service.identifier.mapping;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

class WarehouseIdentifierFormatterGenerationTest extends SoftAssertionSupport {

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(Long.MIN_VALUE, null, IllegalArgumentException.class),
            Arguments.of(-1, null, IllegalArgumentException.class),
            Arguments.of(0, "ROV0000000000000000000", null),
            Arguments.of(1, "ROV0000000000000000001", null),
            Arguments.of(Long.MAX_VALUE, "ROV9223372036854775807", null)
        );
    }

    private final WarehouseIdentifierFormatter warehouseIdentifierFormatter = new WarehouseIdentifierFormatter("ROV");

    @MethodSource("data")
    @ParameterizedTest
    void formatting(long id, String expectedValue, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            softly.assertThatThrownBy(() -> warehouseIdentifierFormatter.format(id)).isInstanceOf(expectedException);
        } else {
            softly.assertThat(warehouseIdentifierFormatter.format(id)).isEqualTo(expectedValue);
        }
    }
}

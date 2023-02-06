package ru.yandex.market.tpl.core.service.vehicle.validator;

import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleRegistrationNumberCountry;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleRegistrationNumberCountry.RUS;

@RequiredArgsConstructor
public class VehicleRegistrationNumberValidatorTest extends TplAbstractTest {
    private final VehicleRegistrationNumberValidator validator;

    @ParameterizedTest
    @EnumSource(VehicleRegistrationNumberCountry.class)
    void testHappyPath(VehicleRegistrationNumberCountry country) {
        assertDoesNotThrow(() -> validator.validate("A000AA", "77", country));
    }

    @ParameterizedTest
    @MethodSource("invalidParameters")
    void testValidationFailed(String number, String region) {
        assertThrows(TplIllegalArgumentException.class, () -> validator.validate(number, region, RUS));
    }

    private static Stream<Arguments> invalidParameters() {
        return Stream.of(
                Arguments.of("A0000AA", "77"),
                Arguments.of("A00AA", "77"),
                Arguments.of("A0D0AA", "77"),
                Arguments.of("D000AA", "77"),
                Arguments.of("A000AD", "77"),
                Arguments.of(" ", "77"),
                Arguments.of("A000A", "1177"),
                Arguments.of("A000A", "1")
        );
    }
}

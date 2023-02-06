package ru.yandex.market.logistics.lom.converter.lms;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Location;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

@DisplayName("LogisticsPointLmsConverter юнит-тест")
public class LogisticsPointLmsConverterTest extends AbstractTest {

    private static final String VALID_INSTRUCTION = "Инструкция";
    private static final String VALID_ADDRESS_COMMENT = "Комментарий к адресу";

    private final LogisticsPointLmsConverter logisticsPointLmsConverter = new LogisticsPointLmsConverter(
        new AddressLmsConverter(),
        new ScheduleDayLmsConverter(),
        new ContactLmsConverter()
    );

    @DisplayName("Проверяем конвертацию инструкции к складу")
    @ParameterizedTest
    @MethodSource("arguments")
    void checkPartnerTypeConversionFromLms(
        String instruction,
        String comment,
        String expectedInstruction
    ) {
        Location actualLocation = logisticsPointLmsConverter.fromExternal(getLogisticsPoint(instruction, comment));
        softly.assertThat(actualLocation).isNotNull();
        softly.assertThat(actualLocation).extracting(Location::getInstruction).isEqualTo(expectedInstruction);
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(null, null, ""),
            Arguments.of("", null, ""),
            Arguments.of("   ", null, ""),
            Arguments.of(VALID_INSTRUCTION, null, VALID_INSTRUCTION),
            Arguments.of(null, "", ""),
            Arguments.of("", "", ""),
            Arguments.of("   ", "", ""),
            Arguments.of(VALID_INSTRUCTION, "", VALID_INSTRUCTION),
            Arguments.of(null, "   ", ""),
            Arguments.of("", "   ", ""),
            Arguments.of("   ", "   ", ""),
            Arguments.of(VALID_INSTRUCTION, "   ", VALID_INSTRUCTION),
            Arguments.of(null, VALID_ADDRESS_COMMENT, VALID_ADDRESS_COMMENT),
            Arguments.of("", VALID_ADDRESS_COMMENT, VALID_ADDRESS_COMMENT),
            Arguments.of("   ", VALID_ADDRESS_COMMENT, VALID_ADDRESS_COMMENT),
            Arguments.of(VALID_INSTRUCTION, VALID_ADDRESS_COMMENT, VALID_INSTRUCTION)
        );
    }

    @Nonnull
    private static LogisticsPointResponse getLogisticsPoint(
        @Nullable String instruction,
        @Nullable String addressComment
    ) {
        return LogisticsPointResponse.newBuilder()
            .instruction(instruction)
            .address(Address.newBuilder().comment(addressComment).build())
            .type(PointType.PICKUP_POINT)
            .phones(Set.of())
            .build();
    }
}

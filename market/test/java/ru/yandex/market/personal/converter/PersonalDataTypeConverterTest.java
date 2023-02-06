package ru.yandex.market.personal.converter;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.lang.NonNull;

import ru.yandex.market.logistics.personal.model.Address;
import ru.yandex.market.personal.AbstractTest;
import ru.yandex.market.personal.client.model.CommonType;
import ru.yandex.market.personal.client.model.CommonTypeEnum;
import ru.yandex.market.personal.client.model.FullName;
import ru.yandex.market.personal.client.model.GpsCoord;
import ru.yandex.market.personal.enums.PersonalDataType;

class PersonalDataTypeConverterTest extends AbstractTest {
    private final PersonalDataTypeConverter converter = new PersonalDataTypeConverter();

    @ParameterizedTest
    @MethodSource(value = "convertArgumentsToCommonType")
    @DisplayName("Тест конвертации всех типов в commonType")
    void convertToAllCommonTypes(CommonTypeEnum typeEnum, Object object) {
        softly.assertThat(converter.toCommonType(typeEnum).apply(object)).isInstanceOf(CommonType.class);
    }

    @ParameterizedTest
    @MethodSource(value = "convertArguments")
    @DisplayName("Тест конвертации всех типов в объект")
    void convertFromAllCommonTypes(CommonTypeEnum typeEnum, CommonType commonType, Class<?> outClass) {
        softly.assertThat(converter.toObject(typeEnum).apply(commonType)).isInstanceOf(outClass);
    }

    @Test
    @DisplayName("Тест получения конвертеров для всех значений енама")
    void testDataTypeConvertersCount() {
        Arrays.stream(PersonalDataType.values()).forEach(dataType ->
            softly.assertThat(converter.toObject(dataType.getCommonTypeEnum())).isInstanceOf(Function.class)
        );
    }

    @NonNull
    private static Stream<Arguments> convertArguments() {
        return Stream.of(
            Arguments.of(
                CommonTypeEnum.PHONE,
                new CommonType().phone(ConverterUtils.PHONE), String.class),
            Arguments.of(
                CommonTypeEnum.EMAIL,
                new CommonType().email(ConverterUtils.EMAIL), String.class),
            Arguments.of(
                CommonTypeEnum.FULL_NAME,
                new CommonType().fullName(ConverterUtils.createFullName()), FullName.class),
            Arguments.of(
                CommonTypeEnum.GPS_COORD,
                new CommonType().gpsCoord(ConverterUtils.createGps()), GpsCoord.class),
            Arguments.of(
                CommonTypeEnum.ADDRESS,
                new CommonType().address(ConverterUtils.createAddressMap()), Address.class)
        );
    }
    @NonNull
    private static Stream<Arguments> convertArgumentsToCommonType() {
        return Stream.of(
            Arguments.of(CommonTypeEnum.PHONE, ConverterUtils.PHONE),
            Arguments.of(CommonTypeEnum.EMAIL, ConverterUtils.EMAIL),
            Arguments.of(CommonTypeEnum.FULL_NAME, ConverterUtils.createFullName()),
            Arguments.of(CommonTypeEnum.GPS_COORD, ConverterUtils.createGps()),
            Arguments.of(CommonTypeEnum.ADDRESS, ConverterUtils.createAddress())
        );
    }
}

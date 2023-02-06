package ru.yandex.market.personal.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.lang.NonNull;

import ru.yandex.market.logistics.personal.converter.AddressConverter;
import ru.yandex.market.personal.AbstractTest;
import ru.yandex.market.personal.client.model.CommonType;
import ru.yandex.market.personal.client.model.MultiTypeStoreResponseItem;
import ru.yandex.market.personal.client.model.PersonalMultiTypeStoreResponse;
import ru.yandex.market.personal.enums.PersonalDataType;
import ru.yandex.market.personal.model.PersonalError;
import ru.yandex.market.personal.model.PersonalStoreRequestItem;
import ru.yandex.market.personal.model.PersonalStoreResponseItem;

class MultiTypeStoreConverterTest extends AbstractTest {
    private static final AddressConverter ADDRESS_CONVERTER = new AddressConverter();
    private final PersonalDataTypeConverter personalDataTypeConverter = new PersonalDataTypeConverter();
    private final MultiTypeStoreConverter converter = new MultiTypeStoreConverter(personalDataTypeConverter);

    @ParameterizedTest(name = DISPLAY_NAME_INDEX_PLACEHOLDER)
    @MethodSource(value = "convertArguments")
    @DisplayName("Тест конвертации всех типов")
    void convertAllTypes(
        String name,
        List<MultiTypeStoreResponseItem> multiTypeItems,
        List<PersonalStoreRequestItem> items,
        List<PersonalStoreResponseItem> result
    ) {
        PersonalMultiTypeStoreResponse response = new PersonalMultiTypeStoreResponse().items(multiTypeItems);
        softly.assertThat(converter.convertResponseToInternalFormat(items, response)).isEqualTo(result);
    }

    @Test
    @DisplayName("Тест конвертации всех типов в generated типы")
    void convertToCommonTypeTest() {
        softly.assertThat(converter.toCommonType(
            List.of(
                new PersonalStoreRequestItem(PersonalDataType.ADDRESS, ConverterUtils.createAddress()),
                new PersonalStoreRequestItem(PersonalDataType.EMAIL, ConverterUtils.EMAIL),
                new PersonalStoreRequestItem(PersonalDataType.FULL_NAME, ConverterUtils.createFullName()),
                new PersonalStoreRequestItem(PersonalDataType.GPS_COORD, ConverterUtils.createGps()),
                new PersonalStoreRequestItem(PersonalDataType.PHONE, ConverterUtils.PHONE)
            )
        )).isEqualTo(
            List.of(
                personalDataTypeConverter
                    .toCommonType(PersonalDataType.ADDRESS.getCommonTypeEnum())
                    .apply(ConverterUtils.createAddress()),
                personalDataTypeConverter
                    .toCommonType(PersonalDataType.EMAIL.getCommonTypeEnum())
                    .apply(ConverterUtils.EMAIL),
                personalDataTypeConverter
                    .toCommonType(PersonalDataType.FULL_NAME.getCommonTypeEnum())
                    .apply(ConverterUtils.createFullName()),
                personalDataTypeConverter
                    .toCommonType(PersonalDataType.GPS_COORD.getCommonTypeEnum())
                    .apply(ConverterUtils.createGps()),
                personalDataTypeConverter
                    .toCommonType(PersonalDataType.PHONE.getCommonTypeEnum())
                    .apply(ConverterUtils.PHONE)
            )
        );
    }

    @NonNull
    private static Stream<Arguments> convertArguments() {
        return Stream.of(
            createArguments(
                "Конвертируем телефон",
                List.of(new PersonalStoreRequestItem(PersonalDataType.PHONE, ConverterUtils.PHONE)),
                Map.of(new CommonType().phone(ConverterUtils.PHONE), "id1"),
                List.of(
                    new PersonalStoreResponseItem(
                        "id1",
                        PersonalDataType.PHONE,
                        ConverterUtils.PHONE,
                        ConverterUtils.PHONE,
                        null
                    ))
                ),
            createArguments(
                "Конвертируем email",

                List.of(new PersonalStoreRequestItem(PersonalDataType.EMAIL, ConverterUtils.EMAIL)),
                Map.of(new CommonType().email(ConverterUtils.EMAIL), "id2"),
                List.of(
                    new PersonalStoreResponseItem(
                        "id2",
                        PersonalDataType.EMAIL,
                        ConverterUtils.EMAIL,
                        ConverterUtils.EMAIL,
                        null
                    )
                )),
            createArguments(
                "Конвертируем ФИО",
                List.of(new PersonalStoreRequestItem(PersonalDataType.FULL_NAME, ConverterUtils.createFullName())),
                Map.of(new CommonType().fullName(ConverterUtils.createFullName()), "id3"),
                List.of(
                    new PersonalStoreResponseItem(
                        "id3",
                        PersonalDataType.FULL_NAME,
                        ConverterUtils.createFullName(),
                        ConverterUtils.createFullName(),
                        null
                    )
                )),
            createArguments(
                "Конвертируем адрес",
                List.of(new PersonalStoreRequestItem(PersonalDataType.ADDRESS, ConverterUtils.createAddress())),
                Map.of(
                    new CommonType().address(ADDRESS_CONVERTER.convertFromModel(ConverterUtils.createAddress())),
                    "id4"
                ),
                List.of(
                    new PersonalStoreResponseItem(
                        "id4",
                        PersonalDataType.ADDRESS,
                        ConverterUtils.createAddress(),
                        ConverterUtils.createAddress(),
                        null
                    )
                )
            ),
            createArguments(
                "Конвертируем gps",
                List.of(new PersonalStoreRequestItem(PersonalDataType.GPS_COORD, ConverterUtils.createGps())),
                Map.of(new CommonType().gpsCoord(ConverterUtils.createGps()), "id5"),
                List.of(
                    new PersonalStoreResponseItem(
                        "id5",
                        PersonalDataType.GPS_COORD,
                        ConverterUtils.createGps(),
                        ConverterUtils.createGps(),
                        null
                    )
                )
            ),
            createArguments(
                "Конвертируем два поля, но приходит одно",
                List.of(
                    new PersonalStoreRequestItem(PersonalDataType.PHONE, ConverterUtils.PHONE),
                    new PersonalStoreRequestItem(PersonalDataType.EMAIL, ConverterUtils.EMAIL)
                ),
                Map.of(new CommonType().phone(ConverterUtils.PHONE), "id6"),
                List.of(
                    new PersonalStoreResponseItem(
                        "id6",
                        PersonalDataType.PHONE,
                        ConverterUtils.PHONE,
                        ConverterUtils.PHONE,
                        null
                    ),
                    new PersonalStoreResponseItem(
                        null,
                        PersonalDataType.EMAIL,
                        ConverterUtils.EMAIL,
                        null,
                        new PersonalError("Object not stored")
                    )
                )
            ),
            createArguments(
                "Конвертируем одно поле, но приходит два",
                List.of(new PersonalStoreRequestItem(PersonalDataType.GPS_COORD, ConverterUtils.createGps())),
                getResponse7(),
                List.of(
                    new PersonalStoreResponseItem(
                        "id7",
                        PersonalDataType.GPS_COORD,
                        ConverterUtils.createGps(),
                        ConverterUtils.createGps(),
                        null
                    )
                )
            )
        );
    }

    @NonNull
    private static Map<CommonType, String> getResponse7() {
        var map = new HashMap<CommonType, String>();
        map.put(new CommonType().gpsCoord(ConverterUtils.createGps()), "id7");
        map.put(new CommonType().fullName(ConverterUtils.createFullName()), "id8");
        return map;
    }

    @NonNull
    private static Arguments createArguments(
        String name,
        List<PersonalStoreRequestItem> items,
        Map<CommonType, String> response,
        List<PersonalStoreResponseItem> result
    ) {
        var multiTypeStoreResponseItems = response.entrySet().stream()
            .map(entry -> new MultiTypeStoreResponseItem()
                .id(entry.getValue())
                .value(entry.getKey())
                .normalized(entry.getKey())
            )
            .collect(Collectors.toList());
        return Arguments.of(name, multiTypeStoreResponseItems, items, result);
    }
}

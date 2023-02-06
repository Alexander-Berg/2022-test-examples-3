package ru.yandex.market.personal.converter;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.lang.NonNull;

import ru.yandex.market.logistics.personal.converter.AddressConverter;
import ru.yandex.market.personal.AbstractTest;
import ru.yandex.market.personal.client.model.CommonType;
import ru.yandex.market.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.personal.client.model.PersonalMultiTypeRetrieveResponse;
import ru.yandex.market.personal.enums.PersonalDataType;
import ru.yandex.market.personal.model.PersonalResponseItem;

class MultiTypeRetrieveConverterTest extends AbstractTest {
    private static final AddressConverter ADDRESS_CONVERTER = new AddressConverter();

    private final MultiTypeRetrieveConverter converter = new MultiTypeRetrieveConverter(
        new PersonalDataTypeConverter()
    );

    @ParameterizedTest(name = DISPLAY_NAME_INDEX_PLACEHOLDER)
    @MethodSource(value = "convertArguments")
    @DisplayName("Тест конвертации всех типов")
    void convertAllTypes(MultiTypeRetrieveResponseItem multiTypeItem, PersonalResponseItem personalItem) {
        PersonalMultiTypeRetrieveResponse response = new PersonalMultiTypeRetrieveResponse()
            .addItemsItem(multiTypeItem);
        softly.assertThat(converter.convertResponseToInternalFormat(response)).containsExactly(personalItem);
    }

    @NonNull
    private static Stream<Arguments> convertArguments() {
        return Stream.of(
            createArguments(
                "id1", PersonalDataType.PHONE, new CommonType().phone(ConverterUtils.PHONE), ConverterUtils.PHONE),
            createArguments(
                "id2", PersonalDataType.EMAIL, new CommonType().email(ConverterUtils.EMAIL), ConverterUtils.EMAIL),
            createArguments(
                "id3",
                PersonalDataType.FULL_NAME,
                new CommonType().fullName(ConverterUtils.createFullName()),
                ConverterUtils.createFullName()
            ),
            createArguments(
                "id4",
                PersonalDataType.ADDRESS,
                new CommonType().address(ADDRESS_CONVERTER.convertFromModel(ConverterUtils.createAddress())),
                ConverterUtils.createAddress()
            ),
            createArguments("id5", PersonalDataType.GPS_COORD, new CommonType().gpsCoord(
                ConverterUtils.createGps()), ConverterUtils.createGps())
        );
    }

    @NonNull
    private static Arguments createArguments(
        String id,
        PersonalDataType dataType,
        CommonType commonType,
        Object value
    ) {
        MultiTypeRetrieveResponseItem multiTypeRetrieveResponseItem = new MultiTypeRetrieveResponseItem()
            .id(id)
            .type(dataType.getCommonTypeEnum())
            .value(commonType);
        PersonalResponseItem personalResponseItem = new PersonalResponseItem(id, dataType, value);
        return Arguments.of(multiTypeRetrieveResponseItem, personalResponseItem);
    }
}

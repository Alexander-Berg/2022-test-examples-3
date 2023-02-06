package ru.yandex.market.logistics.personal;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.personal.converter.AddressConverter;
import ru.yandex.market.logistics.personal.model.Address;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@DisplayName("Конвертация адреса")
class AddressConverterTest extends AbstractTest {

    private final AddressConverter addressConverter = new AddressConverter();

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "] {0}={1}")
    @MethodSource
    @DisplayName("Конвертация из мапы в модель")
    void convertToModel(String key, String value, Address expectedAddress) {
        Map<String, String> map = Map.of(key, value);
        Address address = addressConverter.convertToModel(map);
        softly.assertThat(address)
            .usingRecursiveComparison()
            .isEqualTo(expectedAddress);
    }

    @Test
    @DisplayName("Конвертация из мапы в модель c подмешиванием страны")
    void convertToModelWithBuilder() {
        Map<String, String> map = Map.of(Address.CITY_KEY, "city");
        Address address = addressConverter.convertToModel(map, (Address.Builder builder) -> builder.country("country"));
        softly.assertThat(address)
                .usingRecursiveComparison()
                .isEqualTo(Address.builder().country("country").city("city").build());
    }

    @Test
    @DisplayName("Замена поля")
    void replaceValues() {
        Address address = addressConverter.replaceValues(Address.builder().country("country1").build(),
                (Address.Builder builder) -> builder.country("country2"));
        softly.assertThat(address)
                .usingRecursiveComparison()
                .isEqualTo(Address.builder().country("country2").build());
    }

    @Test
    @DisplayName("Конвертация чекаутерного адреса в логистический с заменой поля")
    void convertFromCheckoutModel() {
        var from = Address.builder()
                .country("country1")
                .city("city")
                .subRegion("subRegion")
                .federalDistrict("federalDistrict")
                .region("region")
                .geoId(213)
                .comment("comment")
                .apartment("apartment")
                .block("block")
                .building("building")
                .district("building")
                .entrance("entrance")
                .entryPhone("entryPhone")
                .estate("estate")
                .floor(1)
                .house("house")
                .housing("housing")
                .km("km")
                .intercom("intercom")
                .settlement("settlement")
                .locality("locality")
                .metro("metro")
                .porch("porch")
                .postcode("postcode")
                .room("room")
                .street("street")
                .subway("subway")
                .zipCode("zipCode")
                .build();

        var to = Address.builder()
                .country("country2")
                .subRegion("subRegion")
                .federalDistrict("federalDistrict")
                .region("region")
                .geoId(213)
                .comment("comment")
                .building("building")
                .district("building")
                .estate("estate")
                .floor(1)
                .house("house")
                .housing("block")
                .km("km")
                .intercom("entryPhone")
                .settlement("settlement")
                .locality("city")
                .metro("subway")
                .porch("entrance")
                .room("apartment")
                .street("street")
                .zipCode("postcode")
                .build();

        Address toActual = addressConverter.convertFromCheckoutModel(from,
                (Address.Builder builder) -> builder.country("country2"));
        softly.assertThat(toActual)
                .usingRecursiveComparison()
                .isEqualTo(to);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Проверка адреса на чекаутерность")
    void isCheckout(Address address, boolean result) {
        softly.assertThat(addressConverter.isCheckout(address)).isEqualTo(result);
    }

    @Nonnull
    static Stream<Arguments> isCheckout() {
        return Stream.of(
            Arguments.of(Address.builder().city("city").locality("test").build(), true),
            Arguments.of(Address.builder().city("city").build(), true),
            Arguments.of(Address.builder().locality("test").build(), false)
        );
    }


    @ParameterizedTest
    @MethodSource
    @DisplayName("Проверка мапы адреса на чекаутерность")
    void isCheckoutByMap(Map<String, String> address, boolean result) {
        softly.assertThat(addressConverter.isCheckout(address)).isEqualTo(result);
    }

    @Nonnull
    static Stream<Arguments> isCheckoutByMap() {
        return Stream.of(
                Arguments.of(Map.of(Address.CITY_KEY, "city", Address.LOCALITY_KEY, "locality"), true),
                Arguments.of(Map.of(Address.CITY_KEY, "city"), true),
                Arguments.of(Map.of(Address.LOCALITY_KEY, "locality"), false)
        );
    }

    @Nonnull
    static Stream<Arguments> convertToModel() {
        return Stream.of(
            Arguments.of("country", "Страна", Address.builder().country("Страна").build()),
            Arguments.of(
                "federalDistrict",
                "Федеральный_округ",
                Address.builder().federalDistrict("Федеральный_округ").build()
            ),
            Arguments.of("region", "Область", Address.builder().region("Область").build()),
            Arguments.of("locality", "Населенный_пункт", Address.builder().locality("Населенный_пункт").build()),
            Arguments.of("subRegion", "Городской_округ", Address.builder().subRegion("Городской_округ").build()),
            Arguments.of("settlement", "Поселение", Address.builder().settlement("Поселение").build()),
            Arguments.of("district", "Микрорайон", Address.builder().district("Микрорайон").build()),
            Arguments.of("street", "Улица", Address.builder().street("Улица").build()),
            Arguments.of("house", "Дом", Address.builder().house("Дом").build()),
            Arguments.of("building", "Строение", Address.builder().building("Строение").build()),
            Arguments.of("housing", "Корпус", Address.builder().housing("Корпус").build()),
            Arguments.of("room", "Квартира_или_офис", Address.builder().room("Квартира_или_офис").build()),
            Arguments.of("zipCode", "Почтовый_индекс", Address.builder().zipCode("Почтовый_индекс").build()),
            Arguments.of("porch", "Подъезд", Address.builder().porch("Подъезд").build()),
            Arguments.of("floor", "15", Address.builder().floor(15).build()),
            Arguments.of("metro", "Станция_метро", Address.builder().metro("Станция_метро").build()),
            Arguments.of("geoId", "213", Address.builder().geoId(213).build()),
            Arguments.of("geoId", "non_parseable_to_integer", Address.builder().build()),
            Arguments.of("intercom", "Код_домофона", Address.builder().intercom("Код_домофона").build()),
            Arguments.of("comment", "Комментарий", Address.builder().comment("Комментарий").build()),
            Arguments.of("postcode", "Индекс-чекаут", Address.builder().postcode("Индекс-чекаут").build()),
            Arguments.of("city", "Город-чекаут", Address.builder().city("Город-чекаут").build()),
            Arguments.of("subway", "Метро-чекаут", Address.builder().subway("Метро-чекаут").build()),
            Arguments.of("km", "KM-чекаут", Address.builder().km("KM-чекаут").build()),
            Arguments.of("estate", "Поместье-чекаут", Address.builder().estate("Поместье-чекаут").build()),
            Arguments.of("block", "Корпус-чекаут", Address.builder().block("Корпус-чекаут").build()),
            Arguments.of("entrance", "Подъезд-чекаут", Address.builder().entrance("Подъезд-чекаут").build()),
            Arguments.of("entryphone", "Домофон-чекаут", Address.builder().entryPhone("Домофон-чекаут").build()),
            Arguments.of("apartment", "Квартира-чекаут", Address.builder().apartment("Квартира-чекаут").build()),
            Arguments.of("unknownKey", "value", Address.builder().build())
        );
    }

    @Test
    void allFieldCoveredWithTestCases() {
        Set<String> getterKeys = Address.GETTERS.keySet();
        Set<String> setterKeys = Address.SETTERS.keySet();
        softly.assertThat(SetUtils.disjunction(getterKeys, setterKeys))
            .as("Getters and setters maps have disjoint keys")
            .isEmpty();

        Set<String> convertToModelTestKeys = convertToModel()
            .map(arguments -> (String) arguments.get()[0])
            .collect(Collectors.toSet());

        softly.assertThat(SetUtils.difference(getterKeys, convertToModelTestKeys))
            .as("Not all getters covered with test cases")
            .isEmpty();

        Set<String> convertFromModelTestKeys = convertFromModel()
            .map(arguments -> (String) arguments.get()[1])
            .collect(Collectors.toSet());
        softly.assertThat(SetUtils.difference(setterKeys, convertFromModelTestKeys))
            .as("Not all setters covered with test cases")
            .isEmpty();
    }

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "] {1}")
    @MethodSource
    @DisplayName("Конвертация из модели в мапу")
    void convertFromModel(Address address, String key, String value) {
        Map<String, String> map = addressConverter.convertFromModel(address);
        softly.assertThat(map)
            .usingRecursiveComparison()
            .isEqualTo(Map.of(key, value));
    }

    @Nonnull
    static Stream<Arguments> convertFromModel() {
        return Stream.of(
            Arguments.of(Address.builder().country("Страна").build(), "country", "Страна"),
            Arguments.of(
                Address.builder().federalDistrict("Федеральный_округ").build(),
                "federalDistrict",
                "Федеральный_округ"
            ),
            Arguments.of(Address.builder().region("Область").build(), "region", "Область"),
            Arguments.of(Address.builder().locality("Населенный_пункт").build(), "locality", "Населенный_пункт"),
            Arguments.of(Address.builder().subRegion("Городской_округ").build(), "subRegion", "Городской_округ"),
            Arguments.of(Address.builder().settlement("Поселение").build(), "settlement", "Поселение"),
            Arguments.of(Address.builder().district("Микрорайон").build(), "district", "Микрорайон"),
            Arguments.of(Address.builder().street("Улица").build(), "street", "Улица"),
            Arguments.of(Address.builder().house("Дом").build(), "house", "Дом"),
            Arguments.of(Address.builder().building("Строение").build(), "building", "Строение"),
            Arguments.of(Address.builder().housing("Корпус").build(), "housing", "Корпус"),
            Arguments.of(Address.builder().room("Квартира_или_офис").build(), "room", "Квартира_или_офис"),
            Arguments.of(Address.builder().zipCode("Почтовый_индекс").build(), "zipCode", "Почтовый_индекс"),
            Arguments.of(Address.builder().porch("Подъезд").build(), "porch", "Подъезд"),
            Arguments.of(Address.builder().floor(15).build(), "floor", "15"),
            Arguments.of(Address.builder().metro("Станция_метро").build(), "metro", "Станция_метро"),
            Arguments.of(Address.builder().geoId(213).build(), "geoId", "213"),
            Arguments.of(Address.builder().intercom("Код_домофона").build(), "intercom", "Код_домофона"),
            Arguments.of(Address.builder().comment("Комментарий").build(), "comment", "Комментарий"),
            Arguments.of(Address.builder().postcode("Индекс-чекаут").build(), "postcode", "Индекс-чекаут"),
            Arguments.of(Address.builder().city("Город-чекаут").build(), "city", "Город-чекаут"),
            Arguments.of(Address.builder().subway("Метро-чекаут").build(), "subway", "Метро-чекаут"),
            Arguments.of(Address.builder().km("KM-чекаут").build(), "km", "KM-чекаут"),
            Arguments.of(Address.builder().estate("Поместье-чекаут").build(), "estate", "Поместье-чекаут"),
            Arguments.of(Address.builder().block("Корпус-чекаут").build(), "block", "Корпус-чекаут"),
            Arguments.of(Address.builder().entrance("Подъезд-чекаут").build(), "entrance", "Подъезд-чекаут"),
            Arguments.of(Address.builder().entryPhone("Домофон-чекаут").build(), "entryphone", "Домофон-чекаут"),
            Arguments.of(Address.builder().apartment("Квартира-чекаут").build(), "apartment", "Квартира-чекаут"),
            Arguments.of(Address.builder().comment("Комментарий").build(), "comment", "Комментарий")
        );
    }
}

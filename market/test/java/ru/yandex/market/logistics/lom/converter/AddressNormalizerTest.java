package ru.yandex.market.logistics.lom.converter;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;

@DisplayName("Unit тесты для AddressNormalizer")
class AddressNormalizerTest extends AbstractTest {

    private static final long NON_CDEK_PARTNER_ID = 1;

    private static final long CDEK_PARTNER_ID = 51;

    private final AddressNormalizer addressNormalizer = new AddressNormalizer();

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("parameters")
    @DisplayName("Подмена региона для некоторых locationId")
    void normalizeRegion(String region, Integer locationId, Long partnerId, String expectedRegion) {
        String normalizeRegion = addressNormalizer.normalizeRegion(region, locationId, partnerId);
        softly.assertThat(normalizeRegion).isEqualTo(expectedRegion);
    }

    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of("Москва", 213, CDEK_PARTNER_ID, "Москва и Московская область"),
            Arguments.of("Подольск", 10747, CDEK_PARTNER_ID, "Москва и Московская область"),
            Arguments.of("Софьино", 120013, CDEK_PARTNER_ID, "Москва и Московская область"),
            Arguments.of("Томилино", 101060, CDEK_PARTNER_ID, "Москва и Московская область"),
            Arguments.of("Котельники", 21651, CDEK_PARTNER_ID, "Москва и Московская область"),
            Arguments.of("Санкт-Петербург", 2, CDEK_PARTNER_ID, "Санкт-Петербург и Ленинградская область"),
            Arguments.of("Санкт-Петербург", null, CDEK_PARTNER_ID, "Санкт-Петербург"),
            Arguments.of(null, 213, CDEK_PARTNER_ID, "Москва и Московская область"),
            Arguments.of(null, null, CDEK_PARTNER_ID, null),
            Arguments.of("Москва", 213, NON_CDEK_PARTNER_ID, "Москва и Московская область"),
            Arguments.of("Подольск", 10747, NON_CDEK_PARTNER_ID, "Москва и Московская область"),
            Arguments.of("Софьино", 120013, NON_CDEK_PARTNER_ID, "Софьино"),
            Arguments.of("Томилино", 101060, NON_CDEK_PARTNER_ID, "Томилино"),
            Arguments.of("Котельники", 21651, NON_CDEK_PARTNER_ID, "Котельники"),
            Arguments.of("Санкт-Петербург", 2, NON_CDEK_PARTNER_ID, "Санкт-Петербург"),
            Arguments.of("Санкт-Петербург", null, NON_CDEK_PARTNER_ID, "Санкт-Петербург"),
            Arguments.of(null, 213, NON_CDEK_PARTNER_ID, "Москва и Московская область"),
            Arguments.of(null, null, NON_CDEK_PARTNER_ID, null),
            // Мы не предполагаем сейчас возможности partnerId == null (и поэтому @Nonnull).
            // Но NPE в этой ситуации всё равно быть не должно.
            Arguments.of("Москва", 213, null, "Москва и Московская область"),
            Arguments.of("Подольск", 10747, null, "Москва и Московская область"),
            Arguments.of("Софьино", 120013, null, "Софьино"),
            Arguments.of("Томилино", 101060, null, "Томилино"),
            Arguments.of("Котельники", 21651, null, "Котельники"),
            Arguments.of("Санкт-Петербург", 2, null, "Санкт-Петербург"),
            Arguments.of("Санкт-Петербург", null, null, "Санкт-Петербург"),
            Arguments.of(null, 213, null, "Москва и Московская область"),
            Arguments.of(null, null, null, null)
        );
    }
}

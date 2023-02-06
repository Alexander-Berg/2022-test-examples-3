package ru.yandex.market.logistics.management.entity.validation;

import javax.validation.ValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Юнит тест валидатора PartnerExternalParam")
class PartnerExternalParamValidatorTest {

    private final PartnerExternalParamValidator validator = new PartnerExternalParamValidator();

    @Test
    @DisplayName("Нет значения")
    void noValue() {
        assertThrows(
            ValidationException.class,
            () -> validator.validate(PartnerExternalParamType.LAST_MILE_RECIPIENT_DEADLINE.name(), null)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "0", "1"})
    @DisplayName("Проверка валидации для типа BOOLEAN")
    void validBoolean(String value) {
        validator.validate(PartnerExternalParamType.IS_DROPOFF.name(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"60", "3147483647", "string", "15:00:00"})
    @DisplayName("Проверка невалидного значения для типа BOOLEAN")
    void invalidBoolean(String value) {
        assertThrows(
            ValidationException.class,
            () -> validator.validate(PartnerExternalParamType.IS_DROPOFF.name(), value)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"2147483647", "-2147483648", "60"})
    @DisplayName("Проверка валидации для типа INTEGER")
    void validInteger(String value) {
        validator.validate(PartnerExternalParamType.DAYS_FOR_CONTRACT_DS_SHIPMENT_RETURN.name(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "3147483647", "string", "15:00:00"})
    @DisplayName("Проверка невалидного значения для типа INTEGER")
    void invalidInteger(String value) {
        assertThrows(
            ValidationException.class,
            () -> validator.validate(PartnerExternalParamType.DAYS_FOR_CONTRACT_DS_SHIPMENT_RETURN.name(), value)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"18:00", "15:00:00"})
    @DisplayName("Проверка валидации для типа TIME")
    void validTime(String value) {
        validator.validate(PartnerExternalParamType.LAST_MILE_RECIPIENT_DEADLINE.name(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "3147483647", "string", "15"})
    @DisplayName("Проверка невалидного значения для типа TIME")
    void invalidTime(String value) {
        assertThrows(
            ValidationException.class,
            () -> validator.validate(PartnerExternalParamType.LAST_MILE_RECIPIENT_DEADLINE.name(), value)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775807", "-9223372036854775807", "15"})
    @DisplayName("Проверка валидации для типа LONG")
    void validLong(String value) {
        validator.validate(PartnerExternalParamType.EXPRESS_RETURN_SORTING_CENTER_ID.name(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Сортировочный центр Яндекс Маркета — Тест",
        "Сортировочный центр Яндекс Маркета — Тест(транзит)",
        "Сортировочный центр DPD — Тест",
        "Склад Яндекс Маркета — Тест"
    })
    @DisplayName("Проверка валидации для READABLE_PARTNER_NAME_FOR_SPRAVKA")
    void validReadableNameParamString(String value) {
        validator.validate(PartnerExternalParamType.READABLE_PARTNER_NAME_FOR_SPRAVKA.name(), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "СЦ Яндекс Маркета — Тест",
        "Сортировочный центр DPD — тест",
        "Сортировочный центр (dpd) — Тест",
        "Склад Яндекс Маркета тест"
    })
    @DisplayName("Проверка ошибки валидации для READABLE_PARTNER_NAME_FOR_SPRAVKA")
    void invalidReadableNameParamString(String value) {
        assertThrows(
            ValidationException.class,
            () -> validator.validate(PartnerExternalParamType.READABLE_PARTNER_NAME_FOR_SPRAVKA.name(), value)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "string", "15:00:00"})
    @DisplayName("Проверка невалидного значения для типа LONG")
    void invalidLong(String value) {
        assertThrows(
            ValidationException.class,
            () -> validator.validate(PartnerExternalParamType.EXPRESS_RETURN_SORTING_CENTER_ID.name(), value)
        );
    }
}

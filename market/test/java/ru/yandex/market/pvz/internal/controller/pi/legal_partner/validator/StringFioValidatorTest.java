package ru.yandex.market.pvz.internal.controller.pi.legal_partner.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringFioValidatorTest {

    private final StringFioValidator validator = new StringFioValidator();

    @Test
    void validFio() {
        String fio = "Иванов Петр Федорович";
        assertThat(validator.isValid(fio, null)).isTrue();
    }

    @Test
    void validFi() {
        String fio = "Иванов Петр";
        assertThat(validator.isValid(fio, null)).isTrue();
    }

    @Test
    void validFioWithDashedLastName() {
        String fio = "Иванов-Сидоров Петр Федорович";
        assertThat(validator.isValid(fio, null)).isTrue();
    }

    @Test
    void validFioWithDashedLastAndFirstName() {
        String fio = "Иванов-Сидоров Петр-Павел Федорович";
        assertThat(validator.isValid(fio, null)).isTrue();
    }

    @Test
    void validNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void notValidOnlyLastName() {
        String fio = "Иванов";
        assertThat(validator.isValid(fio, null)).isFalse();
    }

    @Test
    void notValidFourWords() {
        String fio = "Иванов Сидоров Иван Федорович";
        assertThat(validator.isValid(fio, null)).isFalse();
    }

}

package ru.yandex.market.tpl.common.web.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NullOrNotBlankValidatorTest {

    private final NullOrNotBlankValidator validator = new NullOrNotBlankValidator();

    @Test
    void nullValue() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void notBlankValue() {
        String value = "test";
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @Test
    void blankValue() {
        String value = "";
        assertThat(validator.isValid(value, null)).isFalse();
    }

    @Test
    void trimBlankValue() {
        String value = "     ";
        assertThat(validator.isValid(value, null)).isFalse();
    }
}

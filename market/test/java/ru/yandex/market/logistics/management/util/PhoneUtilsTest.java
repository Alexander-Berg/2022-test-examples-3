package ru.yandex.market.logistics.management.util;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;

public class PhoneUtilsTest extends AbstractTest {

    @Test
    public void normalizeTest() {
        softly.assertThat(PhoneUtils.normalize("89051112233")).isEqualTo("+79051112233");
        softly.assertThat(PhoneUtils.normalize("79051112233")).isEqualTo("+79051112233");
        softly.assertThat(PhoneUtils.normalize("+79051112233")).isEqualTo("+79051112233");
        softly.assertThat(PhoneUtils.normalize("8 (905) 111-22-33")).isEqualTo("+79051112233");
        softly.assertThat(PhoneUtils.normalize(" + 7 (905)   111-22-33")).isEqualTo("+79051112233");
        softly.assertThat(PhoneUtils.normalize("\u202D\u202D+79999783713")).isEqualTo("+79999783713");
        softly.assertThat(PhoneUtils.normalize("+9152204132")).isEqualTo("+79152204132");
    }

    @Test
    public void testValidPhone() {
        softly.assertThat(PhoneUtils.validate("+79505401544")).isTrue();
        softly.assertThat(PhoneUtils.validate("+79858425434")).isTrue();
        softly.assertThat(PhoneUtils.validate("+79851111111")).isTrue();
        softly.assertThat(PhoneUtils.validate("+380501111111")).isTrue();
    }

    @Test
    public void testInvalidPhone() {
        softly.assertThat(PhoneUtils.validate("string")).isFalse();
        softly.assertThat(PhoneUtils.validate("+7912123121")).isFalse();
        softly.assertThat(PhoneUtils.validate("+7912123121123")).isFalse();
    }

    @Test
    public void formatTest() {
        softly.assertThat(PhoneUtils.format("+79051112233")).isEqualTo("+7 (905) 111-22-33");
        softly.assertThat(PhoneUtils.format("89051112233")).isEqualTo("8 (905) 111-22-33");
        softly.assertThat(PhoneUtils.format("+109051112233")).isEqualTo("+109051112233");
    }
}

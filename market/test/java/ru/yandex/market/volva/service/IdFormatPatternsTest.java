package ru.yandex.market.volva.service;

import org.junit.Test;

import ru.yandex.market.volva.entity.IdType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class IdFormatPatternsTest {

    @Test
    public void validatePuid() {
        var good = "4235152";
        var bad = "001231235";
        var good2 = "12345678901234567890";
        var bad2 = "123456789012345678901";
        assertThat(IdFormatPatterns.test(IdType.PUID, good)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.PUID, bad)).isFalse();
        assertThat(IdFormatPatterns.test(IdType.PUID, good2)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.PUID, bad2)).isFalse();
    }

    @Test
    public void validateUuid() {
        var good = "123e4567e89b12d3a456426614174000";
        var bad = "123e4567e89b12d3a45642661417400";
        assertThat(IdFormatPatterns.test(IdType.UUID, good)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.UUID, bad)).isFalse();
    }

    @Test
    public void validateYandexuid() {
        var good = "12345678901234567";
        var bad = "02345678901234567";
        assertThat(IdFormatPatterns.test(IdType.YANDEXUID, good)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.YANDEXUID, bad)).isFalse();
    }

    @Test
    public void validateCard() {
        var good = "a29d1b90a32ba00b55bb735a";
        var bad = "a29d1b90a32ba00b55bb735s";
        assertThat(IdFormatPatterns.test(IdType.CARD, good)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.CARD, bad)).isFalse();
    }

    @Test
    public void validateCrypta() {
        var good = "1902321321";
        var bad = "012";
        var good2 = "12345678901234567890";
        var bad2 = "123456789012345678901";
        assertThat(IdFormatPatterns.test(IdType.CRYPTA_ID, good)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.CRYPTA_ID, bad)).isFalse();
        assertThat(IdFormatPatterns.test(IdType.CRYPTA_ID, good2)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.CRYPTA_ID, bad2)).isFalse();
    }

    @Test
    public void validateDeviceId() {
        var good = "aaa406ab-51ad-4360-a0f7-bc2103485932";
        var good2 = "9DF29497-DC36-453D-ABCB-7D77B241A380";
        var good3 = "d161a6f0e50ff6c64b887731deff3ccd";
        var bad = "d161a6f0e50ff6c64b887731deff3cc";
        var bad2 = "00000000-0000-0000-0000-000000000000";
        assertThat(IdFormatPatterns.test(IdType.DEVICE_ID, good)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.DEVICE_ID, good2)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.DEVICE_ID, good3)).isTrue();
        assertThat(IdFormatPatterns.test(IdType.DEVICE_ID, bad)).isFalse();
        assertThat(IdFormatPatterns.test(IdType.DEVICE_ID, bad2)).isFalse();
    }
}

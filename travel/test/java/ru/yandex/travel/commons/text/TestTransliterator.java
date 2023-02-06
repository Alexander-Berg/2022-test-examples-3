package ru.yandex.travel.commons.text;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTransliterator {
    @Test
    public void testTransliterateMixed() {
        assertThat(Transliterator.transliterate("Анна-Юлия D'Baærё")).isEqualTo("Anna-Iuliia DBare");
    }

    @Test
    public void testTransliterateLatin() {
        assertThat(Transliterator.transliterate("John Doe")).isEqualTo("John Doe");
    }
}

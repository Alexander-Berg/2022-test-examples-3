package ru.yandex.direct.utils.text;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TranslitServiceTest {

    @Test
    public void test() {
        var got = TranslitService.translit("  D | feed | zakupka | Бытовая техника ");
        assertThat(got).isEqualTo("  D | feed | zakupka | Bytovaya tekhnika ");
    }

    @Test
    public void testRusAlphabet() {
        var got = TranslitService.translit("АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ абвгдеёжзийклмнопрстуфхцчшщъыьэюя");
        assertThat(got).isEqualTo("ABVGDEYoZhZIYKLMNOPRSTUFKhTsChShShchYEYuYa " +
                "abvgdeyozhziyklmnoprstufkhtschshshchyeyuya");
    }

    @Test
    public void testNotLatinAndNotSupportedLetters() {
        var got = TranslitService.translit("다사랑중앙병원");
        assertThat(got).isEqualTo("다사랑중앙병원");
    }

    @Test
    public void testSpecialChars() {
        var got = TranslitService.translit("™®©’°⁰¹²³⁴⁵⁶⁷⁸⁹\u20bd");
        assertThat(got).isEqualTo("™®©’°⁰¹²³⁴⁵⁶⁷⁸⁹\u20BD");
    }

    @Test
    public void testEmptySting() {
        var got = TranslitService.translit("");
        assertThat(got).isEqualTo("");
    }

    @Test
    public void testNullSting() {
        var got = TranslitService.translit(null);
        assertThat(got).isNull();
    }
}

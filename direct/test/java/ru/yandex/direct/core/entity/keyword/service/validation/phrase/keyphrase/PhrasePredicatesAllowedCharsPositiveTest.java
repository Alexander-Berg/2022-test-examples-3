package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.allowedChars;
import static ru.yandex.direct.utils.TextConstants.LETTERS;

@RunWith(Parameterized.class)
public class PhrasePredicatesAllowedCharsPositiveTest {

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"символы английского алфавита", "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz"},
                {"символы русского алфавита", "АаБбВвГгДдЕеЁёЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЬьЫыЪъЭэЮюЯя"},
                {"символы украинского алфавита", "ҐґЄєІіЇї"},
                {"символы турецкого алфавита", "ÇçĞğİiÖöŞşÜü"},
                {"символы казахского алфавита", "ӘәҒғҚқҢңӨөҰұҮүҺһІі"},
                {"символы немецкого алфавита", "ÄäÖöÜüß"},
                {"символы белорусского алфавита", "ІіЎў"},

                {"фраза на русском", "Хорошая фраза"},
                {"фраза на турецком", "çişü"},
                {"фраза на украинском", "ґєнкі"},
                {"фраза на казахском", "ғәфұрі"},
                {"фраза на немецком", "Köln"},
                {"фраза на белорусском", "наяўнымі"},
                {"все допустимые буквы", LETTERS},
                {"цифры", "0123456789"},
                {"спец. символы и пробел", ".!\"[]+- '"},
                {"фраза со всеми спец. символами", "\"раз [два !три] +раз 1.2 -минус\""},
        });
    }

    @SuppressWarnings("unused")
    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(allowedChars().test(keyword), is(true));
    }
}

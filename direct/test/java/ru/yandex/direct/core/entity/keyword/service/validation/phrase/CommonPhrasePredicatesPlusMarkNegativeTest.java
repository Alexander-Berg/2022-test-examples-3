package ru.yandex.direct.core.entity.keyword.service.validation.phrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.CommonPhrasePredicates.validPlusMark;

@RunWith(Parameterized.class)
public class CommonPhrasePredicatesPlusMarkNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // одиночный +
                {"+"},
                {"+ "},
                {" + "},
                {"+ бв"},
                {"бв +"},
                {"аб + вг"},

                // сдвоенный +
                {"++"},
                {" ++ "},
                {"++важно"},
                {"сдвоенный ++важно"},

                // + перед запрещенным символом
                {"+["},
                {"+[абв]"},
                {" +[абв] "},
                {"а+[абв]"},

                {"+]"},
                {"[абв+]"},
                {" [абв+] "},
                {"[абв+]а"},

                {"+\""},
                {"+\"абв\""},
                {" +\"абв\" "},
                {"а+\"абв\""},

                {"+."},
                {"+.абв"},
                {" +.абв "},
                {"а+.абв"},

                {"+!"},
                {"+!абв"},
                {" +!абв "},
                {"а+!абв"},

                {"+'"},
                {"+'абв"},
                {" +'абв "},
                {"а+'абв"},

                {"+-"},
                {"+-абв"},
                {" +-абв "},
                {"а+-абв"},

                {"+ "},
                {"+ абв"},
                {" + абв "},
                {"а+ абв"},

                {"-+"},

                // + после запрещенного символа
                {"в+"},
                {"-в+"},
                {" -в+ "},
                {"в+абв"},

                {"!+"},
                {"-!+"},
                {" -!+ "},
                {"!+абв"},

                {"]+"},
                {"-]+"},
                {" -]+ "},
                {"]+абв"},

                {".+"},
                {"-.+"},
                {" -.+ "},
                {".+абв"},

                {"'+"},
                {"-'+"},
                {" -'+ "},
                {"'+абв"},

                // + после разрешенного "-", но когда это дефис, а не минус
                {"санкт-+петербург"},
                {"в-+петербург"},
                {"в-+п"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validPlusMark().test(keyword), is(false));
    }
}

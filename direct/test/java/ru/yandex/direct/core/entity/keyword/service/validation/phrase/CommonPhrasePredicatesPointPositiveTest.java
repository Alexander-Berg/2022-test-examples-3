package ru.yandex.direct.core.entity.keyword.service.validation.phrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.CommonPhrasePredicates.validPoint;

@RunWith(Parameterized.class)
public class CommonPhrasePredicatesPointPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // точка в середине слова
                {"н.а"},
                {" н.а "},
                {"слово н.а слово"},
                {"!н.а"},
                {" !н.а "},
                {"слово !н.а слово"},
                {"+н.а"},
                {" +н.а "},
                {"слово +н.а слово"},
                {"[н.а]"},
                {" [ н.а ] "},
                {" [слово н.а слово] "},
                {"\"н.а\""},
                {"\" н.а \""},
                {"\"слово н.а слово\""},

                {"домен.ру"},
                {" домен.ру "},
                {"!домен.ру"},
                {"+домен.ру"},
                {"[домен.ру]"},
                {"\"домен.ру\""},

                // точка в конце слова
                {"а."},
                {" а. "},
                {"слово а. слово"},

                {"слово."},
                {"слово слово. слово"},

                {"[слово.]"},
                {"слово [слово.] слово"},

                {"\"слово.\""},
                {"\"слово [слово.] слово\""},

                // точка в середине числа
                {"1.2"},
                {"!1.2"},
                {"+1.2"},
                {"[1.2]"},
                {"\"1.2\""},

                // точка в середине числа c буквами
                {"1.v"},
                {"v.1"},

                // несколько точек
                {"127.0.0.1"},
                {"127.v.0.1"},

                // несколько слов с точками
                {"слово 127.0.0.1 1 2 домен.ру"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validPoint().test(keyword), is(true));
    }
}

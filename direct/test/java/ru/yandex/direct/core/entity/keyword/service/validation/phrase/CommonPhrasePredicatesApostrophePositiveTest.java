package ru.yandex.direct.core.entity.keyword.service.validation.phrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.CommonPhrasePredicates.validApostrophe;

@RunWith(Parameterized.class)
public class CommonPhrasePredicatesApostrophePositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // апостроф в конце
                {"а'"},
                {" а' "},
                {"слово а' слово"},

                {"слово'"},
                {"слово слово' слово"},

                {"[слово']"},
                {"слово [слово'] слово"},

                {"\"слово'\""},
                {"\"слово [слово'] слово\""},

                // апостроф в середине слова
                {"н'а"},
                {" н'а "},
                {"слово н'а слово"},
                {"!н'а"},
                {" !н'а "},
                {"слово !н'а слово"},
                {"+н'а"},
                {" +н'а "},
                {"слово +н'а слово"},
                {"[н'а]"},
                {" [ н'а ] "},
                {" [слово н'а слово] "},
                {"\"н'а\""},
                {"\" н'а \""},
                {"\"слово н'а слово\""},

                {"д'артаньян"},
                {" д'артаньян "},
                {"!д'артаньян"},
                {"+д'артаньян"},
                {"[д'артаньян]"},
                {"\"д'артаньян\""},

                // апостроф в середине числа
                {"1'2"},
                {"!1'2"},
                {"+1'2"},
                {"[1'2]"},
                {"\"1'2\""},

                // апостроф в середине числа c буквами
                {"1'v"},
                {"v'1"},

                // несколько апострофов
                {"127'0'0'1"},
                {"127'v'0'1"},

                // несколько слов с апострофами
                {"слово 127'0'0'1 1 2 домен'ру"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validApostrophe().test(keyword), is(true));
    }
}

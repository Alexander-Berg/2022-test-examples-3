package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseConstraints.onlySingleMinusWordsRaw;

@RunWith(Parameterized.class)
public class PhraseConstraintsSingleMinusWordRawNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // плюс-слово и минус-фраза
                {"фраза -слово слово"},

                {"фраза -слово !слово"},
                {"фраза -!слово слово"},
                {"фраза -!слово !слово"},

                {"фраза -слово +слово"},
                {"фраза -+слово слово"},
                {"фраза -+слово +слово"},

                {"фраза -слово слово -слово"},
                {"фраза -слово -слово слово"},
                {"фраза -слово -слово слово -слово"},

                // плюс-слово и минус-слово c дефисом
                {"фраза -санкт-петербург"},
                {"фраза -москва -санкт-петербург"},
                {"фраза -санкт-петербург -москва"},
                {"фраза -санкт-петербург -санкт-петербург"},

                // плюс-слово и минус-слово, разделяемое по точке
                {"фраза -1.2.3"},

                // плюс-слово и минус-фраза с квадратными скобками или кавычками
                {"фраза -[слово слово]"},
                {"фраза -слово [слово]"},
                {"фраза -[слово] слово"},

                {"фраза -\"слово слово\""},
                {"фраза -3-низкие"},
                {"фраза -ульяновск-чебоксары"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(onlySingleMinusWordsRaw().apply(keyword), notNullValue());
    }
}

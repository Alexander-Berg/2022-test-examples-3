package ru.yandex.direct.core.entity.keyword.service;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;

@RunWith(Parameterized.class)
public class KeywordUtilsSplitKeywordsWithParenthesisTest {

    @Parameterized.Parameter()
    public String phrase;

    @Parameterized.Parameter(1)
    public List<String> expectedPhrases;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"фраза без скобок", singletonList("фраза без скобок")},
                //всегда добавляются пробелы до и после фразы из скобок (для простоты)
                {"A (B|C) D", asList("A  B  D", "A  C  D")},
                {"A(B|C)D", asList("A B D", "A C D")},
                //больше двух вариантов в скобках
                {"AA(BB|C|DDD)FF", asList("AA BB FF", "AA C FF", "AA DDD FF")},
                //несколько скобок
                {"A(B|C)D(E|F)", asList("A B D E ", "A B D F ", "A C D E ", "A C D F ")},
                //скобки в начале
                {"(A|B)C", asList(" A C", " B C")},
                //скобки в конце
                {"A(B|C)", asList("A B ", "A C ")},
                //только скобки
                {"(ничего кроме скобок|только скобки)", asList(" ничего кроме скобок ", " только скобки ")},
                //другие операторы игнорируются
                {"!фраза +с [оператором и] (скобками|выбором)",
                        asList("!фраза +с [оператором и]  скобками ", "!фраза +с [оператором и]  выбором ")},
                //даже если там будут ошибки
                {"\"фраза с кавычками\" (только сама по себе|одна) 123",
                        asList("\"фраза с кавычками\"  только сама по себе  123",
                                "\"фраза с кавычками\"  одна  123")},
                //одинаковые варианты на этом этапе не запрещены
                {"одинаковые (слова|слова)", asList("одинаковые  слова ", "одинаковые  слова ")},

                //ошибки - результат null
                //никаких вложенных скобок
                {"((A|B)|C)D", null},
                //непарные скобки
                {"(непарные|скобки оказались", null},
                {"(скобка потерялась", null},
                {"скобка ( потерялась в середине", null},
                {"скобка потерялась в конце(", null},
                {"неправильная ) скобка", null},
                {"раз (два ( а закрыть", null},
                {"(", null},
                {")", null},
                //пайп без скобок запрещен
                {"обычная хорошая | фраза", null},
                {"обычная хорошая фраза 2 |", null},
                {"|", null},
                //пустые части в скобках запрещены (в том числе, если там только пробельные символы)
                {"какие-то слова ( |только одна половина)", null},
                {"какие-то слова ( только одна половина |)", null}
        });
    }

    @Test
    public void splitPhrases() {
        List<String> actual = KeywordUtils.splitKeywordWithParenthesis(phrase);
        if (expectedPhrases == null) {
            assertThat(actual, nullValue());
        } else {
            assertThat(actual, containsInAnyOrder(expectedPhrases.toArray()));
        }
    }

}

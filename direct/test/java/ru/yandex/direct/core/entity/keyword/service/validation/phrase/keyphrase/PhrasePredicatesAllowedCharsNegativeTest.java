package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.allowedChars;

@RunWith(Parameterized.class)
public class PhrasePredicatesAllowedCharsNegativeTest {

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"греческие символы", "ἱερογλύφ"},
                {"арабские символы", "لويكيبيديا"},
                {"иероглифы", "漢字"},
                {"запятая", ","},
                {"эл. собачка", "@"},
                {"зведочка", "*"},
                {"двоеточие", ":"},
                {"точка с запятой", ";"},
                {"знак равно", "="},
                {"слеш", "/"},
                {"бекслеш", "\\"},
                {"знак вопроса", "?"},
                {"закрывающая скобка", "("},
                {"закрывающая скобка", ")"},
                {"открывающая фигурная скобка", "{"},
                {"закрывающая фигурная скобка", "}"},
                {"апостроф", "`"},
        });
    }

    @SuppressWarnings("unused")
    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(allowedChars().test(keyword), is(false));
    }
}

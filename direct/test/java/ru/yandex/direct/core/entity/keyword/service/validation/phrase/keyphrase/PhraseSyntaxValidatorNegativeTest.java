package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.bracketsInMinusWords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.illegalCharacters;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidApostrophe;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidBrackets;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidExclamationMark;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidMinusMark;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidPlusMark;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidPoint;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidQuotes;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.minusWordInsideBracketsOrQuotes;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.noPlusWords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.notSingleMinusWord;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.plusMarkInBrackets;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseSyntaxValidator.keywordSyntaxValidator;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class PhraseSyntaxValidatorNegativeTest {

    // минус-слова с дефисом

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{

                {null, notNull()},
                {"", notEmptyString()},
                {"  ", notEmptyString()},

                {"слон конь*", illegalCharacters(singletonList("конь*"))},
                {"красивый lpg ©опирайт -011006 -ngk∫ -system -tankı",
                        illegalCharacters(Arrays.asList("©опирайт", "-ngk∫"))},

                // неправильное использование кавычек
                {"\"слон конь\" замороженный", invalidQuotes()},

                // неправильное использование скобок
                {"[слон конь", invalidBrackets()},

                // неправильное использование оператора \"!\"
                {"!!слон", invalidExclamationMark()},

                // неправильное использование оператора \"+\"
                {"++слон", invalidPlusMark()},

                // неправильное использование оператора \"-\"
                {"--слон", invalidMinusMark()},

                // неправильное использование точки
                {"конь .слон", invalidPoint()},

                // неправильное использование апострофа
                {"конь 'слон", invalidApostrophe()},

                // минус-слова в квадратных скобках
                {"[конь -слон]", minusWordInsideBracketsOrQuotes()},

                // квадратные скобки в минус-словах
                {"конь -слон [слон]", bracketsInMinusWords()},

                // оператор \"+\" в квадратных скобках
                {"[+слон]", plusMarkInBrackets()},

                // плюс-слово с разделяемым минус-словом
                {"слон -санкт-петербург", notSingleMinusWord(singletonList("санкт-петербург"))},

                // отсутствуют плюс-слова
                {"-слон -конь", noPlusWords()},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Parameterized.Parameter(1)
    public Defect expectedDefinition;

    @Test
    public void testValidator() {
        ValidationResult<String, Defect> validationResult =
                keywordSyntaxValidator().apply(keyword);
        assertThat(validationResult,
                hasDefectWithDefinition(validationError(path(), expectedDefinition)));
    }
}

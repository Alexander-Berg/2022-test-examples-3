package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxCountWordsInKeyword;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator.minusKeywordIsValid;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class MinusPhraseValidatorNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{

                {"запрещенный символ*",
                        defect(MinusPhraseDefects::illegalMinusKeywordChars)},

                {"\"неправильные\" кавычки",
                        defect(MinusPhraseDefects::wrongQuotes)},

                {"неправильная . точка",
                        defect(MinusPhraseDefects::separateDot)},

                {"неправильное начало [.слова]",
                        defect(MinusPhraseDefects::invalidWordFirstCharacter)},

                {"два числа 345. с точкой 123.345",
                        defect(MinusPhraseDefects::invalidDot)},

                {"слишком много слов краткость сестра таланта что добавить",
                        defect(kw -> maxCountWordsInKeyword(7, kw))},

                {"оченьдлинноесловотакоечтонепрочесть1",
                        defect(kw -> MinusPhraseDefects.maxLengthMinusWord(35, kw))},

                {"несбалансированные [скобки",
                        defect(MinusPhraseDefects::imbalancedSquareBrackets)},

                {"пустые [] скобки",
                        defect(MinusPhraseDefects::nestedOrEmptySquareBrackets)},

                {"недопустимый оператор внутри [+скобок]",
                        defect(MinusPhraseDefects::invalidOperatorsInsideSquareBrackets)},

                {"неправильное использование !!восклицательного знака",
                        defect(MinusPhraseDefects::invalidExclamationMark)},

                {"неправильное использование знака -",
                        defect(MinusPhraseDefects::invalidMinusMark)},

                {"неправильное использование знака +",
                        defect(MinusPhraseDefects::invalidPlusMark)},

                // invalidCombinationSpecialSymbols - до этого вообще не понятно, как добраться
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Parameterized.Parameter(1)
    public Function<List<String>, Defect> expectedDefinitionProvider;

    @Test
    public void testValidator() {
        ValidationResult<List<String>, Defect> validationResult =
                minusKeywordIsValid(MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE).apply(singletonList(keyword));
        assertThat(validationResult,
                hasDefectWithDefinition(validationError(path(),
                        expectedDefinitionProvider.apply(singletonList(keyword)))));
    }

    private static Function<List<String>, Defect> defect(Function<List<String>, Defect> fn) {
        return fn;
    }
}

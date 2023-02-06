package ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.cannotContainLoneDot;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.cannotContainsOnlyMinusWords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.containsOnlyStopWords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.emptyOrNestedSquareBrackets;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.incorrectCombinationOfSpecialSymbols;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.incorrectUseOfExclamationMarks;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.incorrectUseOfMinusSign;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.incorrectUseOfPlusSign;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.minusWordNotInQuotedPhrase;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.minusWordsCannotSubtractPlusWords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.minusWordsNoPhraseWithDot;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.modifiersInsideSquareBrackets;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.noMinusPhrasesOnlyWords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.onlySingleDotBetweenNumbers;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.tooLongKeyword;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.tooManyWords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.unpairedQuotes;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseDefects.unpairedSquareBrackets;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseValidator.MAX_CHARACTERS_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseValidator.MAX_KEYWORDS_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.advqphrase.AdvqPhraseValidator.MAX_KEYWORD_LENGTH;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.StringDefects.admissibleChars;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdvqPhraseValidatorTest {

    @Autowired
    private AdvqPhraseValidator validator;

    @Test
    public void validate_stringLongerThanMaxCharacters() {
        String test = new String(new char[MAX_CHARACTERS_COUNT + 1]).replace("\0", " ");
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), maxStringLength(MAX_CHARACTERS_COUNT))));
    }

    @Test
    public void validate_stringLongerThanMaxCharacters_plusInMinusWordIsIgnored() {
        String minusWord = "-!xxx";
        String test = minusWord + new String(new char[MAX_CHARACTERS_COUNT - minusWord.length() + 1]);
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                not(hasItem(validationError(path(), maxStringLength(MAX_CHARACTERS_COUNT)))));
    }

    @Test
    public void validate_stringContainsInvalidCharacters() {
        String test = "aaaaa\uE751bbbbbb";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(), hasItem(validationError(path(), admissibleChars())));
    }

    @Test
    public void validate_stringContainsDotAtTheBeginning() {
        String test = ". xxx";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), cannotContainLoneDot())));
    }

    @Test
    public void validate_stringContainsDotAtTheMiddle() {
        String test = "xxx . yyy";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), cannotContainLoneDot())));
    }

    @Test
    public void validate_stringContainsDotAtTheEnd() {
        String test = "xxx .";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), cannotContainLoneDot())));
    }

    @Test
    public void validate_stringContainsLoneQuote() {
        String test = "\"тестовая фраза";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), unpairedQuotes())));
    }

    @Test
    public void validate_stringContainsThreeQuotes() {
        String test = "\"тестовая фраза\" запрос\"";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), unpairedQuotes())));
    }

    @Test
    public void validate_oneStopWord() {
        String test = "in";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), containsOnlyStopWords())));
    }

    @Test
    public void validate_threeStopWords() {
        String test = "in and of";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), containsOnlyStopWords())));
    }

    @Test
    public void validate_oneMinusWord() {
        String test = "-stop";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), cannotContainsOnlyMinusWords())));
    }

    @Test
    public void validate_threeMinusWords() {
        String test = "-стоп -брейк -stop";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), cannotContainsOnlyMinusWords())));
    }

    @Test
    public void validate_keywordTooLong() {
        String test = "xxx zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz yyy";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), tooLongKeyword(MAX_KEYWORD_LENGTH))));
    }

    @Test
    public void validate_tooManyWords() {
        String test = "aaa bbbb cc dddd eeeee ffff ggggg hhhhhh";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(),
                        tooManyWords(MAX_KEYWORDS_COUNT))));
    }

    @Test
    public void validate_minusWordsAreNotCounted() {
        String test = "aaa bbbb -cc dddd eeeee ffff ggggg hhhhhh";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                not(hasItem(validationError(path(),
                        tooManyWords(MAX_KEYWORDS_COUNT)))));
    }

    @Test
    public void validate_stopWordsAreNotCounted() {
        String test = "aaa bbbb in dddd eeeee ffff ggggg hhhhhh";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                not(hasItem(validationError(path(),
                        tooManyWords(MAX_KEYWORDS_COUNT)))));
    }

    @Test
    public void validate_unpairedLeftSquareBracket() {
        String test = "xxxx [bbb fff";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), unpairedSquareBrackets())));
    }


    @Test
    public void validate_unpairedRightSquareBracket() {
        String test = "xxxx bbb] fff";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), unpairedSquareBrackets())));
    }

    @Test
    public void validate_unpairedLeftSquareBracketWithPaired() {
        String test = "xxxx [bbb [fff argh frfrfr]";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), unpairedSquareBrackets())));
    }

    @Test
    public void validate_noUnorderedSquareBrackets() {
        String test = "]test[";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), unpairedSquareBrackets())));
    }

    @Test
    public void validate_emptySquareBrackets() {
        String test = "xxxx [] pffft frfrfr";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), emptyOrNestedSquareBrackets())));
    }

    @Test
    public void validate_nestedSquareBrackets() {
        String test = "xxxx [ [pffft] frfrfr]";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), emptyOrNestedSquareBrackets())));
    }

    @Test
    public void validate_noModifiersInsideSquareBrackets() {
        String test = "xxxx [+\"pffft\"] frfrfr";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), modifiersInsideSquareBrackets())));
    }

    @Test
    public void validate_noMinusModifiersInsideSquareBrackets() {
        String test = "xxxx [-pffft] frfrfr";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), modifiersInsideSquareBrackets())));
    }

    @Test
    public void validate_notMinusModifiersInsideSquareBrackets() {
        String test = "xxxx [pffff-pffft] frfrfr";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                not(hasItem(validationError(path(), modifiersInsideSquareBrackets()))));
    }

    @Test
    public void validate_doubleExclamationMarks() {
        String test = "xxxx !!pffff-pffft frfrfr";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), incorrectUseOfExclamationMarks())));
    }

    @Test
    public void validate_loneExclamationMark() {
        String test = "xxxx ! pffff-pffft frfrfr";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), incorrectUseOfExclamationMarks())));
    }

    @Test
    public void validate_exclamationMarkEndsPhrase() {
        String test = "xxxx pffff-pffft frfrfr!";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), incorrectUseOfExclamationMarks())));
    }

    @Test
    public void validate_exclamationMarkWithOtherModifiers() {
        String test = "xxxx !\"pffff-pffft\" frfrfr";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                not(hasItem(validationError(path(), incorrectUseOfExclamationMarks()))));
    }

    @Test
    public void validate_minusSign() {
        String[] tests = {
                "xxxx pffff--pffft frfrfr",
                "xxxx pffff- pffft frfrfr",
                "xxxx pffff pffft frfrfr-",
                "xxxx pffff !-pffft frfrfr",
                "xxxx pffff X-!pffft frfrfr",
                "xxxx pffff -!-pffft frfrfr",
                "xxxx pffff +-pffft frfrfr",
                "xxxx pffff X-+pffft frfrfr",
                "xxxx pffff -+-pffft frfrfr"
        };
        for (String testCase : tests) {
            ValidationResult<String, Defect> result = validator.apply(testCase);
            assertThat(testCase,
                    result.flattenErrors(),
                    hasItem(validationError(path(), incorrectUseOfMinusSign())));
        }
    }

    @Test
    public void validate_plusSign() {
        String[] tests = {
                "xxxx pffff++pffft frfrfr",
                "xxxx pffff+ pffft frfrfr",
                "xxxx pffff pffft frfrfr+",
                "xxxx pffff+pffft frfrfr"
        };
        for (String testCase : tests) {
            ValidationResult<String, Defect> result = validator.apply(testCase);
            assertThat(testCase,
                    result.flattenErrors(),
                    hasItem(validationError(path(), incorrectUseOfPlusSign())));
        }
    }

    @Test
    public void validate_minusPhrase() {
        String test = "xxxx -pffff pffft frfrfr";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), noMinusPhrasesOnlyWords())));
    }

    @Test
    public void validate_minusWithDot() {
        String test = "xxxx -pff.ff -pffft -frfrfr";
        ValidationResult<String, Defect> result = validator.apply(test);
        assertThat(result.flattenErrors(),
                hasItem(validationError(path(), minusWordsNoPhraseWithDot())));
    }

    @Test
    public void validate_minusWordIncorrectSpecialSymbols() {
        String[] tests = {
                "xxxx pffff [.pffft -frfrfr",
                "xxxx pffff -'pffft -frfrfr"
        };
        for (String testCase : tests) {
            ValidationResult<String, Defect> result = validator.apply(testCase);
            assertThat(testCase,
                    result.flattenErrors(),
                    hasItem(validationError(path(), incorrectCombinationOfSpecialSymbols())));
        }
    }

    @Test
    public void validate_minusWordStartsWithDotOrApostroph() {
        // No test cases
    }

    @Test
    public void validate_minusWordQuotedPhrase() {
        String testCase = "\" -pffft\"";
        ValidationResult<String, Defect> result = validator.apply(testCase);
        assertThat(testCase,
                result.flattenErrors(),
                hasItem(validationError(path(), minusWordNotInQuotedPhrase())));
    }

    @Test
    public void validate_minusWordDoubleDot() {
        String testCase = "frfrfr -57.21.45";
        ValidationResult<String, Defect> result = validator.apply(testCase);
        assertThat(testCase,
                result.flattenErrors(),
                hasItem(validationError(path(), onlySingleDotBetweenNumbers())));
    }

    @Test
    public void validate_minusWordCantSubstractPlusWord() {
        String testCase = "frfrfr -xxxx -frfrfr";
        ValidationResult<String, Defect> result = validator.apply(testCase);
        assertThat(testCase,
                result.flattenErrors(),
                hasItem(validationError(path(), minusWordsCannotSubtractPlusWords(Collections.emptyList()))));
    }

}

package ru.yandex.direct.core.entity.minuskeywordspack.service.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.GROUP_MINUS_KEYWORDS_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.WORDS_MAX_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxCountWordsInKeyword;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxLengthMinusKeywords;
import static ru.yandex.direct.core.entity.minuskeywordspack.service.validation.MinusKeywordsPackValidationService.MAX_NAME_LENGTH;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.StringDefects.admissibleChars;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MinusKeywordsPackValidationServiceTest {

    @Autowired
    private MinusKeywordsPackValidationService validationService;
    private MinusKeywordsPack defaultPack;

    @Before
    public void before() {
        defaultPack = new MinusKeywordsPack().withName("default").withMinusKeywords(singletonList("default"));
    }

    public static List<String> createTooLongKeywords() {
        // суммарная длина всех минус-слов = 10 * 5 * 82 = 4100
        List<String> minusKeywords = new ArrayList<>();
        for (int i = 0; i < 82; i++) {
            minusKeywords.add(StringUtils.repeat(randomAlphanumeric(10), " ", 5));
        }
        return minusKeywords;
    }

    @Test
    public void validate_MinusKeywords_Empty() {
        ValidationResult<MinusKeywordsPack, Defect> actual =
                validate(defaultPack.withMinusKeywords(emptyList()));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.MINUS_KEYWORDS.name())),
                        CollectionDefects.notEmptyCollection()))));
    }

    @Test
    public void validate_MinusKeywords_Null() {
        ValidationResult<MinusKeywordsPack, Defect> actual =
                validate(defaultPack.withMinusKeywords(null));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.MINUS_KEYWORDS.name())),
                        CommonDefects.notNull()))));
    }

    @Test
    public void validate_MinusKeywords_TooLong() {
        ValidationResult<MinusKeywordsPack, Defect> actual =
                validate(defaultPack.withMinusKeywords(createTooLongKeywords()));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.MINUS_KEYWORDS.name())),
                        maxLengthMinusKeywords(GROUP_MINUS_KEYWORDS_MAX_LENGTH)))));
    }

    // проверка на количество слов в минус-фразе должна быть после нормализации
    @Test
    public void validate_MinusKeywordsHasTooMuchWords_ResultHasElementError() {
        List<String> minusKeywords = singletonList("один два три четыре пять шесть семь восемь");

        ValidationResult<MinusKeywordsPack, Defect> actual = validate(defaultPack.withMinusKeywords(minusKeywords));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.MINUS_KEYWORDS.name())),
                        maxCountWordsInKeyword(WORDS_MAX_COUNT, minusKeywords)))));
    }

    @Test
    public void validate_NameIsNull() {
        ValidationResult<MinusKeywordsPack, Defect> actual = validate(defaultPack.withName(null));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.NAME.name())), CommonDefects.notNull()))));

    }

    @Test
    public void validate_Name_Empty() {
        ValidationResult<MinusKeywordsPack, Defect> actual = validate(defaultPack.withName(""));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.NAME.name())), StringDefects.notEmptyString()))));

    }

    @Test
    public void validate_Name_SpacesOnly() {
        ValidationResult<MinusKeywordsPack, Defect> actual = validate(defaultPack.withName("   "));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.NAME.name())), StringDefects.notEmptyString()))));

    }

    @Test
    public void validate_Name_GreaterMax() {
        ValidationResult<MinusKeywordsPack, Defect> actual =
                validate(defaultPack.withName(randomAlphanumeric(MAX_NAME_LENGTH + 1)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.NAME.name())), maxStringLength(MAX_NAME_LENGTH)))));

    }

    @Test
    public void validate_Name_MaxAndSpaces() {
        ValidationResult<MinusKeywordsPack, Defect> actual =
                validate(defaultPack.withName(randomAlphanumeric(MAX_NAME_LENGTH - 1) + "   "));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_Name_InvalidSymbols() {
        ValidationResult<MinusKeywordsPack, Defect> actual = validate(defaultPack.withName("\uD83D\uDD71"));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.NAME.name())), admissibleChars()))));

    }

    @Test
    public void validate_NameForPrivatePack_IsNull() {
        ValidationResult<MinusKeywordsPack, Defect> actual = validationService.validatePack(defaultPack, true);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MinusKeywordsPack.NAME.name())), isNull()))));

    }

    private ValidationResult<MinusKeywordsPack, Defect> validate(MinusKeywordsPack pack) {
        return validationService.validatePack(pack, false);
    }
}

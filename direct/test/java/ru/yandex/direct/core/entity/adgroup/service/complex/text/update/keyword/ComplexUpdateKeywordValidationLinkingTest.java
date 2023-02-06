package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.keyword;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.KEYWORDS;
import static ru.yandex.direct.core.entity.keyword.model.Keyword.PHRASE;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.illegalCharacters;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.incorrectUseOfParenthesis;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidBrackets;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidQuotes;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты линковки ошибок добавляемых и обновляемых ключевых фраз с соответствующими группами
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateKeywordValidationLinkingTest extends ComplexUpdateKeywordTestBase {

    // добавление

    @Test
    public void oneAdGroupWithInvalidAddedKeyword() {
        Keyword invalidKeyword = randomKeyword().withPhrase("[]");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(invalidKeyword));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroup));

        Path path = path(index(0), field(KEYWORDS.name()), index(0), field(PHRASE.name()));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path, invalidBrackets())));
        assertThat("ожидается одна ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void oneAdGroupWithAddedKeywordWithInvalidParenthesis() {
        Keyword invalidKeyword = randomKeyword().withPhrase("(раз|два|) фраза");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(invalidKeyword));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroup));

        Path path = path(index(0), field(KEYWORDS.name()), index(0), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path, incorrectUseOfParenthesis())));
        assertThat("ожидается одна ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void oneAdGroupWithAddedKeywordWithParenthesisAndInvalidSecondPhraseInside() {
        Keyword invalidKeyword = randomKeyword().withPhrase("(раз|два*) фраза");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(invalidKeyword));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroup));

        Path path = path(index(0), field(KEYWORDS.name()), index(0), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path,
                        illegalCharacters(singletonList("два*")))));
        assertThat("ожидается одна ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void oneAdGroupWithAddedKeywordWithParenthesisAndInvalidTwoPhrasesInsideAndAddedInvalidKeyword() {
        Keyword invalidKeyword1 = randomKeyword().withPhrase("(ра[]з|два*) фраза");
        Keyword invalidKeyword2 = randomKeyword().withPhrase("\"");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(invalidKeyword1, invalidKeyword2));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroup));

        Path path1 = path(index(0), field(KEYWORDS.name()), index(0), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path1,
                        illegalCharacters(singletonList("два*")))));

        Path path2 = path(index(0), field(KEYWORDS.name()), index(0), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path2, invalidBrackets())));

        Path path3 = path(index(0), field(KEYWORDS.name()), index(1), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path3, invalidQuotes())));

        assertThat("ожидается две ошибки", result.getValidationResult().flattenErrors(), hasSize(3));
    }

    @Test
    public void oneAdGroupWithValidKeywordsAndSecondWithInvalidAddedKeywordAndValidUpdatedKeyword() {
        createSecondAdGroup();
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo2);

        Keyword validKeywordToUpdate1 = randomKeyword(keywordInfo1.getId());
        Keyword validKeywordToUpdate2 = randomKeyword(keywordInfo2.getId());
        Keyword validKeywordToAdd1 = randomKeyword();
        Keyword invalidKeywordToAdd2 = randomKeyword().withPhrase("[]");
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(validKeywordToUpdate1, validKeywordToAdd1));
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(asList(invalidKeywordToAdd2, validKeywordToUpdate2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(adGroup1, adGroup2));

        Path path = path(index(1), field(KEYWORDS.name()), index(0), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path, invalidBrackets())));
        assertThat("ожидается 1 ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void oneAdGroupWithValidKeywordsAndSecondWithInvalidParenthesisAddedKeywordAndValidUpdatedKeyword() {
        createSecondAdGroup();
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo2);

        Keyword validKeywordToUpdate1 = randomKeyword(keywordInfo1.getId());
        Keyword validKeywordToUpdate2 = randomKeyword(keywordInfo2.getId());
        Keyword validKeywordToAdd1 = randomKeyword();
        Keyword invalidKeywordToAdd2 = randomKeyword().withPhrase("(раз|два|) фраза");
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(validKeywordToUpdate1, validKeywordToAdd1));
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(asList(invalidKeywordToAdd2, validKeywordToUpdate2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(adGroup1, adGroup2));

        Path path = path(index(1), field(KEYWORDS.name()), index(0), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path, incorrectUseOfParenthesis())));
        assertThat("ожидается 1 ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    // обновление

    @Test
    public void oneAdGroupWithInvalidUpdatedKeyword() {
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);

        Keyword invalidKeyword = randomKeyword(keywordInfo1.getId()).withPhrase("[]");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(invalidKeyword));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroup));

        Path path = path(index(0), field(KEYWORDS.name()), index(0), field(PHRASE.name()));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path, invalidBrackets())));
        assertThat("ожидается одна ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void oneAdGroupWithUpdatedKeywordWithInvalidParenthesis() {
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);

        Keyword invalidKeyword = randomKeyword(keywordInfo1.getId()).withPhrase("(раз|два|) фраза");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(invalidKeyword));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroup));

        Path path = path(index(0), field(KEYWORDS.name()), index(0), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path, incorrectUseOfParenthesis())));
        assertThat("ожидается одна ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void oneAdGroupWithUpdatedKeywordWithParenthesisAndInvalidSecondPhraseInside() {
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);

        Keyword invalidKeyword = randomKeyword(keywordInfo1.getId()).withPhrase("(раз|два*) фраза");
        ComplexTextAdGroup adGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(singletonList(invalidKeyword));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroup));

        Path path1 = path(index(0), field(KEYWORDS.name()), index(0), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path1,
                        illegalCharacters(singletonList("два*")))));
        assertThat("ожидается одна ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void twoAdGroupsWithValidAddedKeywordsAndInvalidUpdatedKeywords() {
        createSecondAdGroup();
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo2);

        Keyword validKeywordToUpdate1 = randomKeyword(keywordInfo1.getId());
        Keyword validKeywordToAdd1 = randomKeyword();
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(validKeywordToUpdate1, validKeywordToAdd1));

        Keyword invalidKeywordToUpdate2 = randomKeyword(keywordInfo2.getId()).withPhrase("[]");
        Keyword validKeywordToAdd2 = randomKeyword();
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(asList(validKeywordToAdd2, invalidKeywordToUpdate2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(adGroup1, adGroup2));

        Path path = path(index(1), field(KEYWORDS.name()), index(1), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path, invalidBrackets())));
        assertThat("ожидается 1 ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void twoAdGroupsWithValidAddedKeywordsAndInvalidParenthesisUpdatedKeywords() {
        createSecondAdGroup();
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo2);

        Keyword validKeywordToUpdate1 = randomKeyword(keywordInfo1.getId());
        Keyword validKeywordToAdd1 = randomKeyword();
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(validKeywordToUpdate1, validKeywordToAdd1));

        Keyword invalidKeywordToUpdate2 = randomKeyword(keywordInfo2.getId()).withPhrase("(раз|два|) фраза");
        Keyword validKeywordToAdd2 = randomKeyword();
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(asList(validKeywordToAdd2, invalidKeywordToUpdate2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(adGroup1, adGroup2));

        Path path = path(index(1), field(KEYWORDS.name()), index(1), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path, incorrectUseOfParenthesis())));
        assertThat("ожидается 1 ошибка", result.getValidationResult().flattenErrors(), hasSize(1));
    }

    // добавление + обновление

    @Test
    public void twoAdGroupsWithAddedAndUpdatedValidAndInvalidKeywordMix() {
        createSecondAdGroup();
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo2);

        Keyword validKeywordToUpdate1 = randomKeyword(keywordInfo1.getId());
        Keyword validKeywordToAdd1 = randomKeyword();
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(validKeywordToUpdate1, validKeywordToAdd1));

        Keyword invalidKeywordToUpdate2 = randomKeyword(keywordInfo2.getId()).withPhrase("[]");
        Keyword invalidKeywordToAdd2 = randomKeyword().withPhrase("\"");
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(asList(invalidKeywordToUpdate2, invalidKeywordToAdd2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(adGroup1, adGroup2));

        Path path1 = path(index(1), field(KEYWORDS.name()), index(1), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path1, invalidQuotes())));

        Path path2 = path(index(1), field(KEYWORDS.name()), index(0), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path2, invalidBrackets())));
        assertThat("ожидается 2 ошибки", result.getValidationResult().flattenErrors(), hasSize(2));
    }

    @Test
    public void twoAdGroupsWithInvalidPhrasesInsideParenthesis() {
        createSecondAdGroup();
        KeywordInfo keywordInfo1 = createKeyword(adGroupInfo1);
        KeywordInfo keywordInfo2 = createKeyword(adGroupInfo2);

        Keyword validKeywordToUpdate1 = randomKeyword(keywordInfo1.getId());
        Keyword invalidKeywordToAdd1 = randomKeyword().withPhrase("(четыре[]|два*) фраза");
        ComplexTextAdGroup adGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withKeywords(asList(validKeywordToUpdate1, invalidKeywordToAdd1));

        Keyword invalidKeywordToUpdate2 = randomKeyword(keywordInfo2.getId()).withPhrase("(раз|два*) фраза");
        Keyword validKeywordToAdd2 = randomKeyword();
        ComplexTextAdGroup adGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withKeywords(asList(invalidKeywordToUpdate2, validKeywordToAdd2));

        MassResult<Long> result = updateAndCheckBothItemsAreInvalid(asList(adGroup1, adGroup2));

        Path path1 = path(index(0), field(KEYWORDS.name()), index(1), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path1, invalidBrackets())));

        Path path2 = path(index(0), field(KEYWORDS.name()), index(1), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path2, illegalCharacters(singletonList("два*")))));

        Path path3 = path(index(1), field(KEYWORDS.name()), index(0), field(PHRASE.name()));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path3, illegalCharacters(singletonList("два*")))));
        assertThat("ожидается 3 ошибки", result.getValidationResult().flattenErrors(), hasSize(3));
    }
}

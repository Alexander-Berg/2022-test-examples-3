package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAddedWithFixation;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

/**
 * Тесты обработки новых фраз, которые дублируются одновременно
 * между собой и с существующими фразами.
 */
@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationDeduplicationInNewAndExistingPhrasesTest extends KeywordsAddOperationBaseTest {

    @Test
    public void execute_TwoNewPhrasesDuplicatedWithExistingAndByEachOther_ResultHasCorrectInfo() {
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result, isSuccessfulWithMatchers(isNotAdded(PHRASE_1), isNotAdded(PHRASE_1)));
        assertThat(result.get(0).getResult().getId(), equalTo(existingKeywordId));
        assertThat(result.get(1).getResult().getId(), equalTo(existingKeywordId));
        checkValidationHasDuplicateInExistingWarnings(result, true, true);
        checkValidationHasDuplicateInNewWarnings(result, false, false);
    }

    @Test
    public void execute_AllTypesOfDuplicatesAndOneUniquePhrase_ResultHasCorrectInfo() {
        String existingPhrase1 = "дедупликация спасет мир";
        String existingPhrase2 = "дедупликация работает -!не";
        createOneActiveAdGroup();
        Long existingKeywordId1 = createKeyword(adGroupInfo1, existingPhrase1).getId();
        Long existingKeywordId2 = createKeyword(adGroupInfo1, existingPhrase2).getId();

        String uniquePhrase = "у нас все работает как хорошие швейцарские часы";

        // дублируется только с существующей
        String existingDuplicatedPhrase = "мир спасет дедупликация";

        // дублируются только между собой
        String newDuplicatedPhrase1 = "доминируй дедуплицируй";
        String newDuplicatedPhrase2 = "доминируй дедуплицируй";

        // дублируются между собой и с существующей
        String doubleDuplicatedPhrase1 = "работает дедупликация -не";
        String resultDoubleDuplicatedPhrase1 = "работает дедупликация -!не";
        String doubleDuplicatedPhrase2 = "работает дедупликация работает  -!не";
        String resultDoubleDuplicatedPhrase2 = "работает дедупликация работает -!не";

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, doubleDuplicatedPhrase1),
                clientKeyword(adGroupInfo1, uniquePhrase),
                clientKeyword(adGroupInfo1, existingDuplicatedPhrase),
                clientKeyword(adGroupInfo1, newDuplicatedPhrase1),
                clientKeyword(adGroupInfo1, doubleDuplicatedPhrase2),
                clientKeyword(adGroupInfo1, newDuplicatedPhrase2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotAddedWithFixation(resultDoubleDuplicatedPhrase1, "-не", "-!не"),
                        isAdded(uniquePhrase),
                        isNotAdded(existingDuplicatedPhrase),
                        isAdded(newDuplicatedPhrase1),
                        isNotAdded(resultDoubleDuplicatedPhrase2),
                        isNotAdded(newDuplicatedPhrase2)));

        // уникальная
        assertThat(result.get(1).getResult().getId(), not(equalTo(existingKeywordId1)));
        assertThat(result.get(1).getResult().getId(), not(equalTo(existingKeywordId2)));
        assertThat(result.get(1).getResult().getId(), not(equalTo(result.get(3).getResult().getId())));

        // дублируется с существующей
        assertThat(result.get(2).getResult().getId(), equalTo(existingKeywordId1));

        // дублируются между собой
        assertThat(result.get(3).getResult().getId(), equalTo(result.get(5).getResult().getId()));

        // дублируются между собой и с существующей
        assertThat(result.get(0).getResult().getId(), equalTo(existingKeywordId2));
        assertThat(result.get(4).getResult().getId(), equalTo(existingKeywordId2));

        checkValidationHasDuplicateInExistingWarnings(result, true, false, true, false, true, false);
        checkValidationHasDuplicateInNewWarnings(result, false, false, false, false, false, true);
    }

    @Test
    public void execute_NewPhrasesDuplicatedWithExistingAndByEachOtherAreNotCountedInMaxLimitValidation() {
        createOneActiveAdGroup();
        createKeywords(adGroupInfo1, 198);
        createKeyword(adGroupInfo1, PHRASE_1);

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, PHRASE_2),
                clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);

        assertThat(result,
                isSuccessfulWithMatchers(
                        isNotAdded(PHRASE_1),
                        isAdded(PHRASE_2),
                        isNotAdded(PHRASE_1)));
    }
}

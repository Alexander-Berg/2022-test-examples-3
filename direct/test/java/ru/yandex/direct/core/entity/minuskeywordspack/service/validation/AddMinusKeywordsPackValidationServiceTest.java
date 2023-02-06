package ru.yandex.direct.core.entity.minuskeywordspack.service.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.validation.defects.Defects;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.MAX_LIBRARY_PACKS_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.nestedOrEmptySquareBrackets;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.privateMinusKeywordsPack;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddMinusKeywordsPackValidationServiceTest {

    private static final String VALID_KEYWORD = "минус-фраза " + randomNumeric(5);
    private static final String INVALID_KEYWORD = "[[невалидная] минус-фраза] " + randomNumeric(5);

    @Autowired
    private AddMinusKeywordsPackValidationService validationService;
    @Autowired
    protected Steps steps;
    @Autowired
    private MinusKeywordsPackRepository packRepository;

    private MinusKeywordsPack privatePack;
    private MinusKeywordsPack libraryPack;
    private int shard;
    private ClientId clientId;

    @Before
    public void before() {
        privatePack = privateMinusKeywordsPack();
        libraryPack = libraryMinusKeywordsPack();

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
    }

    @Test
    public void preValidate_MinusKeywords_Success() {
        List<MinusKeywordsPack> newPacks = singletonList(privatePack);
        ValidationResult<List<MinusKeywordsPack>, Defect> actual = preValidate(newPacks, true);
        assertThat(actual.hasAnyErrors()).isFalse();
    }

    @Test
    public void preValidate_MinusKeywords_Null() {
        libraryPack.setMinusKeywords(null);
        List<MinusKeywordsPack> newPacks = singletonList(libraryPack);

        ValidationResult<List<MinusKeywordsPack>, Defect> actual = preValidate(newPacks, false);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(MinusKeywordsPack.MINUS_KEYWORDS.name())), notNull()))));
    }

    @Test
    public void preValidate_MinusKeywords_Invalid() {
        privatePack.setMinusKeywords(asList(VALID_KEYWORD, INVALID_KEYWORD));
        List<MinusKeywordsPack> newPacks = singletonList(privatePack);

        ValidationResult<List<MinusKeywordsPack>, Defect> actual = preValidate(newPacks, true);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(MinusKeywordsPack.MINUS_KEYWORDS.name())),
                        nestedOrEmptySquareBrackets(singletonList(INVALID_KEYWORD))))));
    }

    @Test
    public void preValidate_OnlyNewLibraryPacks_TooMuchPacks() {
        List<MinusKeywordsPack> tooMuchNewPacks = Stream
                .generate(TestMinusKeywordsPacks::libraryMinusKeywordsPack)
                .limit(MAX_LIBRARY_PACKS_COUNT + 1)
                .collect(toList());

        ValidationResult<List<MinusKeywordsPack>, Defect> actual = preValidate(tooMuchNewPacks, false);

        assertValidationResultHasPacksLimitDefects(actual);
    }

    @Test
    public void preValidate_ExistLibraryPacksPlusNewLibraryPacks_TooMuchPacks() {
        createLibraryPacksInDb(1);

        List<MinusKeywordsPack> tooMuchNewPacks = Stream
                .generate(TestMinusKeywordsPacks::libraryMinusKeywordsPack)
                .limit(MAX_LIBRARY_PACKS_COUNT)
                .collect(toList());

        ValidationResult<List<MinusKeywordsPack>, Defect> actual = preValidate(tooMuchNewPacks, false);

        assertValidationResultHasPacksLimitDefects(actual);
    }

    private void assertValidationResultHasPacksLimitDefects(ValidationResult<List<MinusKeywordsPack>, Defect> actual) {
        for (int i = 0; i < actual.getValue().size(); i++) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(index(i)),
                            Defects.negativeKeywordSetsLimitExceeded(MAX_LIBRARY_PACKS_COUNT)))));
        }
    }

    @Test
    public void preValidate_TooManyExistLibraryPacksPlusNewPrivatePack_Success() {
        createLibraryPacksInDb(MAX_LIBRARY_PACKS_COUNT);

        List<MinusKeywordsPack> newPacks = singletonList(privatePack);

        ValidationResult<List<MinusKeywordsPack>, Defect> actual = preValidate(newPacks, true);

        assertThat(actual.hasAnyErrors()).isFalse();
    }

    private ValidationResult<List<MinusKeywordsPack>, Defect> preValidate(List<MinusKeywordsPack> packs,
                                                                          boolean isPrivate) {
        return validationService
                .preValidate(packs, MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, isPrivate, shard, clientId);
    }

    private void createLibraryPacksInDb(int count) {
        List<MinusKeywordsPack> packs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            packs.add(libraryMinusKeywordsPack());
        }
        packRepository.createLibraryMinusKeywords(shard, clientId, packs);
    }
}

package ru.yandex.direct.api.v5.entity.keywords.validation;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.keywords.AddRequest;
import com.yandex.direct.api.v5.keywords.KeywordAddItem;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.keywords.container.AddInputItem;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static java.util.Collections.nCopies;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.direct.api.v5.entity.keywords.KeywordsDefectTypes.maxElementsPerKeywordsAdd;
import static ru.yandex.direct.api.v5.entity.keywords.validation.KeywordsAddRequestValidator.MAX_ELEMENTS_PER_ADD;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.DefectTypes.disableAllAutotargetingCategoriesIsForbidden;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
public class KeywordsAddRequestValidatorTest {
    private static final long AD_GROUP_ID = 131313;
    private KeywordsAddRequestValidator validator;

    private static AddRequest createRequest(int numberOfIds) {
        KeywordAddItem item = new KeywordAddItem();
        item.setAdGroupId(AD_GROUP_ID);
        return new AddRequest().withKeywords(nCopies(numberOfIds, item));
    }

    @Before
    public void before() {
        validator = new KeywordsAddRequestValidator();
    }

    @Test
    public void validate_idsAtTheLimit_noError() {
        AddRequest request = createRequest(MAX_ELEMENTS_PER_ADD);
        ValidationResult<AddRequest, DefectType> result = validator.validate(request);
        assertThat(result).is(hasNoDefects());
    }

    @Test
    public void validate_idsOverflowTheLimit_errorIsGenerated() {
        AddRequest request = createRequest(MAX_ELEMENTS_PER_ADD + 1);
        ValidationResult<AddRequest, DefectType> result = validator.validate(request);
        assertThat(result).is(hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.KEYWORDS.schemaName.getLocalPart())),
                        maxElementsPerKeywordsAdd(MAX_ELEMENTS_PER_ADD))));
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_Keyword() {
        Keyword keyword = new Keyword();
        AddInputItem addInputItem = AddInputItem.createItemForKeyword(keyword, false);
        ValidationResult<List<AddInputItem>, DefectType> result = validator.validateInternalRequest(of(addInputItem));
        assertThat(result).is(hasNoDefects());
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_Null() {
        RelevanceMatch relevanceMatch = new RelevanceMatch();
        AddInputItem addInputItem = AddInputItem.createItemForRelevanceMatch(relevanceMatch, false);
        ValidationResult<List<AddInputItem>, DefectType> result = validator.validateInternalRequest(of(addInputItem));
        assertThat(result).is(hasNoDefects());
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_EmptyList() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withRelevanceMatchCategories(emptySet());
        AddInputItem addInputItem = AddInputItem.createItemForRelevanceMatch(relevanceMatch, false);
        ValidationResult<List<AddInputItem>, DefectType> result = validator.validateInternalRequest(of(addInputItem));
        assertThat(result).is(hasDefectWith(validationError(path(index(0)),
                disableAllAutotargetingCategoriesIsForbidden())));
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_NotEmptyList() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.exact_mark));
        AddInputItem addInputItem = AddInputItem.createItemForRelevanceMatch(relevanceMatch, false);
        ValidationResult<List<AddInputItem>, DefectType> result = validator.validateInternalRequest(of(addInputItem));
        assertThat(result).is(hasNoDefects());
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_KeywordAndRelevanceMatchWithEmptyList() {
        Keyword keyword = new Keyword();
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withRelevanceMatchCategories(emptySet());
        AddInputItem addInputItemKeyword = AddInputItem.createItemForKeyword(keyword, false);
        AddInputItem addInputItemRelevanceMatch = AddInputItem.createItemForRelevanceMatch(relevanceMatch, false);
        ValidationResult<List<AddInputItem>, DefectType> result =
                validator.validateInternalRequest(of(addInputItemKeyword, addInputItemRelevanceMatch));
        assertThat(result).is(hasDefectWith(validationError(path(index(1)),
                disableAllAutotargetingCategoriesIsForbidden())));
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_KeywordAndRelevanceMatchWithNotEmptyList() {
        Keyword keyword = new Keyword();
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.exact_mark));
        AddInputItem addInputItemKeyword = AddInputItem.createItemForKeyword(keyword, false);
        AddInputItem addInputItemRelevanceMatch = AddInputItem.createItemForRelevanceMatch(relevanceMatch, false);
        ValidationResult<List<AddInputItem>, DefectType> result =
                validator.validateInternalRequest(of(addInputItemKeyword, addInputItemRelevanceMatch));
        assertThat(result).is(hasNoDefects());
    }
}

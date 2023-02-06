package ru.yandex.direct.api.v5.entity.keywords.validation;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.keywords.KeywordUpdateItem;
import com.yandex.direct.api.v5.keywords.UpdateRequest;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.keywords.container.UpdateInputItem;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static java.util.Collections.nCopies;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.direct.api.v5.entity.keywords.KeywordsDefectTypes.maxElementsPerKeywordsUpdate;
import static ru.yandex.direct.api.v5.entity.keywords.validation.KeywordsUpdateRequestValidator.MAX_ELEMENTS;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.DefectTypes.disableAllAutotargetingCategoriesIsForbidden;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
public class KeywordsUpdateRequestValidatorTest {
    private static final long KEYWORD_ID = 11;
    private static final long RELEVANCE_MATCH_ID = 22;
    private KeywordsUpdateRequestValidator validator;

    @Before
    public void before() {
        validator = new KeywordsUpdateRequestValidator();
    }

    @Test
    public void validate_idsAtTheLimit_noError() {
        UpdateRequest request = createRequest(MAX_ELEMENTS);
        ValidationResult<UpdateRequest, DefectType> result = validator.validate(request);
        assertThat(result).is(hasNoDefects());
    }

    @Test
    public void validate_idsOverflowTheLimit_errorIsGenerated() {
        UpdateRequest request = createRequest(MAX_ELEMENTS + 1);
        ValidationResult<UpdateRequest, DefectType> result = validator.validate(request);
        assertThat(result).is(hasDefectWith(
                validationError(
                        path(field("Keywords")),
                        maxElementsPerKeywordsUpdate(MAX_ELEMENTS))));
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_Keyword() {
        ModelChanges<Keyword> changes = new ModelChanges<>(KEYWORD_ID, Keyword.class);
        UpdateInputItem updateInputItem = UpdateInputItem.createItemForKeyword(changes);
        ValidationResult<List<UpdateInputItem>, DefectType> result =
                validator.validateInternalRequest(of(updateInputItem));
        assertThat(result).is(hasNoDefects());
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_Null() {
        ModelChanges<RelevanceMatch> changes = new ModelChanges<>(RELEVANCE_MATCH_ID, RelevanceMatch.class);
        UpdateInputItem updateInputItem = UpdateInputItem.createItemForRelevanceMatch(changes);
        ValidationResult<List<UpdateInputItem>, DefectType> result =
                validator.validateInternalRequest(of(updateInputItem));
        assertThat(result).is(hasNoDefects());
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_EmptyList() {
        ModelChanges<RelevanceMatch> changes = new ModelChanges<>(RELEVANCE_MATCH_ID, RelevanceMatch.class);
        changes.process(emptySet(), RelevanceMatch.RELEVANCE_MATCH_CATEGORIES);
        UpdateInputItem updateInputItem = UpdateInputItem.createItemForRelevanceMatch(changes);
        ValidationResult<List<UpdateInputItem>, DefectType> result =
                validator.validateInternalRequest(of(updateInputItem));
        assertThat(result).is(hasDefectWith(validationError(path(index(0)),
                disableAllAutotargetingCategoriesIsForbidden())));
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_NotEmptyList() {
        ModelChanges<RelevanceMatch> changes = new ModelChanges<>(RELEVANCE_MATCH_ID, RelevanceMatch.class);
        changes.process(asSet(RelevanceMatchCategory.exact_mark), RelevanceMatch.RELEVANCE_MATCH_CATEGORIES);
        UpdateInputItem updateInputItem = UpdateInputItem.createItemForRelevanceMatch(changes);
        ValidationResult<List<UpdateInputItem>, DefectType> result =
                validator.validateInternalRequest(of(updateInputItem));
        assertThat(result).is(hasNoDefects());
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_KeywordAndRelevanceMatchWithEmptyList() {
        ModelChanges<Keyword> keywordChanges = new ModelChanges<>(KEYWORD_ID, Keyword.class);
        ModelChanges<RelevanceMatch> relevanceMatchChanges = new ModelChanges<>(RELEVANCE_MATCH_ID,
                RelevanceMatch.class);
        relevanceMatchChanges.process(emptySet(), RelevanceMatch.RELEVANCE_MATCH_CATEGORIES);
        UpdateInputItem updateInputItemKeyword = UpdateInputItem.createItemForKeyword(keywordChanges);
        UpdateInputItem updateInputItemRelevanceMatch =
                UpdateInputItem.createItemForRelevanceMatch(relevanceMatchChanges);
        ValidationResult<List<UpdateInputItem>, DefectType> result =
                validator.validateInternalRequest(of(updateInputItemKeyword, updateInputItemRelevanceMatch));
        assertThat(result).is(hasDefectWith(validationError(path(index(1)),
                disableAllAutotargetingCategoriesIsForbidden())));
    }

    @Test
    public void validate_notEmptyRelevanceMatchCategories_KeywordAndRelevanceMatchWithNotEmptyList() {
        ModelChanges<Keyword> keywordChanges = new ModelChanges<>(KEYWORD_ID, Keyword.class);
        ModelChanges<RelevanceMatch> relevanceMatchChanges = new ModelChanges<>(RELEVANCE_MATCH_ID,
                RelevanceMatch.class);
        relevanceMatchChanges.process(asSet(RelevanceMatchCategory.exact_mark),
                RelevanceMatch.RELEVANCE_MATCH_CATEGORIES);
        UpdateInputItem updateInputItemKeyword = UpdateInputItem.createItemForKeyword(keywordChanges);
        UpdateInputItem updateInputItemRelevanceMatch =
                UpdateInputItem.createItemForRelevanceMatch(relevanceMatchChanges);
        ValidationResult<List<UpdateInputItem>, DefectType> result =
                validator.validateInternalRequest(of(updateInputItemKeyword, updateInputItemRelevanceMatch));
        assertThat(result).is(hasNoDefects());
    }

    private static UpdateRequest createRequest(int numberOfIds) {
        KeywordUpdateItem item = new KeywordUpdateItem();
        item.setId(1L);
        return new UpdateRequest().withKeywords(nCopies(numberOfIds, item));
    }
}

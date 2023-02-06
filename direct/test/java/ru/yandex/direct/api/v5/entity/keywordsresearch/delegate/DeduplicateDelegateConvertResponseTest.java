package ru.yandex.direct.api.v5.entity.keywordsresearch.delegate;

import java.util.List;

import com.yandex.direct.api.v5.keywordsresearch.DeduplicateResponse;
import com.yandex.direct.api.v5.keywordsresearch.DeduplicateResponseAddItem;
import com.yandex.direct.api.v5.keywordsresearch.DeduplicateResponseUpdateItem;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.keyword.processing.ProcessedKeyword;
import ru.yandex.direct.core.entity.keyword.processing.unglue.KeywordUngluer;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.libs.keywordutils.parser.KeywordParser.parseWithMinuses;

public class DeduplicateDelegateConvertResponseTest {
    private DeduplicateDelegate delegate;

    @Before
    public void before() {
        delegate = new DeduplicateDelegate(mock(ApiAuthenticationSource.class),
                mock(ResultConverter.class),
                mock(KeywordNormalizer.class),
                mock(KeywordUngluer.class),
                mock(StopWordService.class),
                mock(KeywordWithLemmasFactory.class),
                mock(SingleKeywordsCache.class));
    }

    /**
     * 0:A    -> Insert(A)
     */
    @Test
    public void convertResponse_OneItemWithoutId_IsInserted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(null, "A", "A", null)));

        checkAdd(response, "A");
    }

    /**
     * 0:A -C -> Insert(A)
     * 0:A    -> Ignore
     */
    @Test
    public void convertResponse_TwoItemsWithoutIdAndTheSame_BothAreGluedOneIsInserted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(null, "A -C", "A -C", null),
                item(null, "A", "A -C", null)));

        checkAdd(response, "A -C");
    }

    /**
     * 0:A    -> Ignore
     * 0:A -C -> Insert(A)
     */
    @Test
    public void convertResponse_TwoItemsWithoutIdAndTheSame_BothAreGluedGluedIsInserted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(null, "A", "A -C", null),
                item(null, "A -C", "A -C", null)));

        checkAdd(response, "A -C");
    }

    /**
     * 0:A    -> Ignore
     * 1:A    -> Ignore
     */
    @Test
    public void convertResponse_twoItemsWithAndWithoutIdAndEqual_bothAreGluedNothingIsInsertedOrUpdated() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(null, "A", "A", null),
                item(1L, "A", "A", null)));

        checkAdd(response);
        checkUpdate(response);
    }

    /**
     * 0:A -C -> Ignore
     * 1:A    -> Update(A -C)
     */
    @Test
    public void convertResponse_TwoItemsWithAndWithoutIdAndTheSame_BothAreGluedWithIdIsUpdated() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(null, "A -C", "A -C", null),
                item(1L, "A", "A -C", null)));

        checkAdd(response);
        checkUpdate(response, update(1L, "A -C"));
    }

    /**
     * 0:A    -> Ignore
     * 1:A -C -> Ignore
     */
    @Test
    public void convertResponse_TwoItemsWithAndWithoutIdAndTheSame_BothAreGluedWithIdMatchesGluedAndIsNotUpdated() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(null, "A", "A -C", null),
                item(1L, "A -C", "A -C", null)));

        checkAdd(response);
        checkUpdate(response);
    }

    /**
     * 1:A    -> Ignore
     * 2:A    -> Delete
     */
    @Test
    public void convertResponse_TwoItemsWithIdsAndEqualDirectOrder_OneIsDeleted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(1L, "A", "A", 2L),
                item(2L, "A", "A", 1L)));

        checkUpdate(response);
        checkDelete(response, 2L);
    }

    /**
     * 2:A    -> Delete
     * 1:A    -> Ignore
     */
    @Test
    public void convertResponse_TwoItemsWithIdsAndEqualReverseOrder_OneIsDeleted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(1L, "A", "A", 1L),
                item(2L, "A", "A", 2L)));

        checkUpdate(response);
        checkDelete(response, 1L);
    }

    /**
     * 1:A    -> Ignore
     * 2:A    -> Delete
     * 3:A    -> Delete
     */
    @Test
    public void convertResponse_ThreeItemsWithIdsAndEqualDirectOrder_TwoAreDeleted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(1L, "A", "A", 3L),
                item(2L, "A", "A", 2L),
                item(3L, "A", "A", 1L)));

        checkUpdate(response);
        checkDelete(response, 2L, 3L);
    }

    /**
     * 3:A    -> Delete
     * 2:A    -> Delete
     * 1:A    -> Ignore
     */
    @Test
    public void convertResponse_ThreeItemsWithIdsAndEqualReverseOrder_TwoAreDeleted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(1L, "A", "A", 1L),
                item(2L, "A", "A", 2L),
                item(3L, "A", "A", 3L)));

        checkUpdate(response);
        checkDelete(response, 2L, 1L);
    }

    /**
     * 1:A -C -> Ignore
     * 2:A    -> Delete
     */
    @Test
    public void convertResponse_TwoItemsWithIdsAndTheSameDirectOrder_BothAreGluedExtraIsDeleted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(1L, "A -C", "A -C", 2L),
                item(2L, "A", "A -C", 1L)));

        checkUpdate(response);
        checkDelete(response, 2L);
    }

    /**
     * 2:A -C -> Delete
     * 1:A    -> Update(A -C)
     */
    @Test
    public void convertResponse_TwoItemsWithIdsAndTheSameReverseOrder_BothAreGluedExtraIsDeleted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(1L, "A -C", "A -C", 1L),
                item(2L, "A", "A -C", 2L)));

        checkUpdate(response, update(2L, "A -C"));
        checkDelete(response, 1L);
    }

    /**
     * 1:A    -> Update(A -C)
     * 2:A -C -> Delete
     */
    @Test
    public void convertResponse_TwoItemsWithIdsAndEqualDirectOrder_BothAreGluedOneIsUpdatedExtraIsDeleted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(1L, "A", "A -C", 2L),
                item(2L, "A -C", "A -C", 1L)));

        checkUpdate(response, update(1L, "A -C"));
        checkDelete(response, 2L);
    }

    /**
     * 2:A    -> Delete
     * 1:A -C -> Ignore
     */
    @Test
    public void convertResponse_TwoItemsWithIdsAndEqualReverseOrder_BothAreGluedOneIsUpdatedExtraIsDeleted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(1L, "A", "A -C", 1L),
                item(2L, "A -C", "A -C", 2L)));

        checkUpdate(response);
        checkDelete(response, 1L);
    }

    @Test
    public void convertResponse_OneItemWithDifferentInitialAndNormalizedKeywords_InitialKeywordsAreInserted() {
        DeduplicateResponse response = delegate.convertResponse(result(
                item(null, "A", "a", null)));

        checkAdd(response, "A");
    }

    @SafeVarargs
    private static ApiResult<List<ApiResult<DeduplicateKeywordResponse>>> result(
            ApiResult<DeduplicateKeywordResponse>... results) {
        return ApiResult.successful(StreamEx.of(results).toList());
    }

    private static ApiResult<DeduplicateKeywordResponse> item(Long id, String originalKeywordWithOriginalMinusWords,
                                                              String normalizedKeywordWithProcessedMinusWords, Long weight) {
        return ApiResult.successful(
                new DeduplicateKeywordResponse(id, new ProcessedKeyword(kw(originalKeywordWithOriginalMinusWords),
                        kw(normalizedKeywordWithProcessedMinusWords)), weight, false));
    }

    private static KeywordWithMinuses kw(String keyword) {
        return parseWithMinuses(keyword);
    }

    private static Pair<Long, String> update(Long id, String keyword) {
        return Pair.of(id, keyword);
    }

    private static void checkAdd(DeduplicateResponse response, String... adds) {
        assertThat(response.getAdd().size())
                .withFailMessage("Количество добавляемых фраз не соответствует ожидаемому")
                .isEqualTo(adds.length);
        for (int i = 0; i < response.getAdd().size(); i++) {
            DeduplicateResponseAddItem actual = response.getAdd().get(i);
            String expected = adds[i];
            assertThat(actual.getKeyword())
                    .withFailMessage("Текст добавляемой фразы не соответствует ожидаемому, %s, а не %s",
                            actual.getKeyword(), expected)
                    .isEqualTo(expected);
        }
    }

    @SafeVarargs
    private static void checkUpdate(DeduplicateResponse response, Pair<Long, String>... updates) {
        assertThat(response.getUpdate().size())
                .withFailMessage("Количество обновляемых фраз не соответствует ожидаемому")
                .isEqualTo(updates.length);
        for (int i = 0; i < response.getUpdate().size(); i++) {
            DeduplicateResponseUpdateItem actual = response.getUpdate().get(i);
            Pair<Long, String> expected = updates[i];
            assertThat(actual.getId())
                    .withFailMessage("Id обновляемой фразы не соответствует ожидаемому, %d, а не %d ",
                            actual.getId(), expected.getLeft())
                    .isEqualTo(expected.getLeft());
            assertThat(actual.getKeyword())
                    .withFailMessage("Текст обновляемой фразы не соответствует ожидаемому, %s, а не %s",
                            actual.getKeyword(), expected.getRight())
                    .isEqualTo(expected.getRight());
        }
    }

    private static void checkDelete(DeduplicateResponse response, Long... deletes) {
        assertThat(response.getDelete().getIds().size())
                .withFailMessage("Количество удаляемых фраз не соответствует ожидаемому")
                .isEqualTo(deletes.length);
        for (int i = 0; i < response.getDelete().getIds().size(); i++) {
            Long actual = response.getDelete().getIds().get(i);
            Long expected = deletes[i];
            assertThat(actual)
                    .withFailMessage("Id удаляемой фразы не соответствует ожидаемому, %d, а не %d",
                            actual, expected)
                    .isEqualTo(expected);
        }
    }
}

package ru.yandex.direct.api.v5.entity.keywords.converter;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import com.yandex.direct.api.v5.general.IdsCriteria;
import com.yandex.direct.api.v5.keywords.DeleteRequest;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.keywords.container.DeleteInputItem;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KeywordsDeleteRequestConverterTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1111L);
    private static final long KEYWORD_ID = 10L;
    private static final long RELEVANCE_MATCH_ID = 20L;

    private RelevanceMatchService relevanceMatchService;
    private KeywordsDeleteRequestConverter requestConverter;

    @Before
    public void setUp() {
        relevanceMatchService = mock(RelevanceMatchService.class);
        when(relevanceMatchService.getRelevanceMatchIds(
                eq(CLIENT_ID), eq(Sets.newHashSet(KEYWORD_ID, RELEVANCE_MATCH_ID))))
                .thenReturn(Collections.singleton(RELEVANCE_MATCH_ID));
        requestConverter = new KeywordsDeleteRequestConverter(relevanceMatchService);
    }

    @Test
    public void convertRequest_CheckCalls() {
        requestConverter.convertRequest(deleteRequest(KEYWORD_ID, RELEVANCE_MATCH_ID), CLIENT_ID);
        verify(relevanceMatchService).getRelevanceMatchIds(
                eq(CLIENT_ID), eq(Sets.newHashSet(KEYWORD_ID, RELEVANCE_MATCH_ID)));
    }

    @Test
    public void convertRequest_EmptyRequest() {
        List<DeleteInputItem> result = requestConverter.convertRequest(deleteRequest(), CLIENT_ID);
        assertThat(result).isEmpty();
    }

    @Test
    public void convertRequest_OneKeywordOneRelevanceMatch() {
        List<DeleteInputItem> result = requestConverter.convertRequest(
                deleteRequest(KEYWORD_ID, RELEVANCE_MATCH_ID), CLIENT_ID);
        assertThat(result).containsExactlyInAnyOrder(DeleteInputItem.createItemForKeyword(KEYWORD_ID),
                DeleteInputItem.createItemForRelevanceMatch(RELEVANCE_MATCH_ID));
    }

    private DeleteRequest deleteRequest(Long... ids) {
        return new DeleteRequest().withSelectionCriteria(new IdsCriteria().withIds(ids));
    }
}

package ru.yandex.market.mbo.tms.redis;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.JobExecutionContext;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 15.03.2020
 */
public class RedisCategoryRendererExecutorTest {

    private static final int THREAD_COUNT = 1;
    private static final long TEST_CATEGORY_1 = 1L;
    private static final long REMOVED_CATEGORY = 2L;
    private static final long TEST_CATEGORY_2 = 3L;

    private TovarTreeServiceMock tovarTreeServiceMock;
    private UpdateRedisTovarTreeService updateRedisTovarTreeServiceMock;
    private UpdateRedisCategoryService updateRedisCategoryServiceMock;
    private UpdateRedisCategorySizeMeasureService updateRedisCategorySizeMeasureServiceMock;
    private UpdateRedisCategorySizeMeasuresInfoService updateRedisCategorySizeMeasuresInfoServiceMock;
    private RedisCategoryRendererExecutor executor;


    @Before
    public void setUp() {
        tovarTreeServiceMock = new TovarTreeServiceMock();
        updateRedisTovarTreeServiceMock = mock(UpdateRedisTovarTreeService.class);

        updateRedisCategoryServiceMock = mock(UpdateRedisCategoryService.class);
        updateRedisCategorySizeMeasureServiceMock = mock(UpdateRedisCategorySizeMeasureService.class);
        updateRedisCategorySizeMeasuresInfoServiceMock = mock(UpdateRedisCategorySizeMeasuresInfoService.class);
        tovarTreeServiceMock.addCategory(new TovarCategory("TEST1", TEST_CATEGORY_1, TovarCategory.NO_ID));
        executor = new RedisCategoryRendererExecutor(
            tovarTreeServiceMock,
            updateRedisTovarTreeServiceMock,
            updateRedisCategoryServiceMock,
            updateRedisCategorySizeMeasureServiceMock,
            updateRedisCategorySizeMeasuresInfoServiceMock,
            THREAD_COUNT
        );
    }

    @Test
    public void newCategory() throws Exception {
        when(updateRedisCategoryServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(Collections.emptyMap());
        when(updateRedisCategorySizeMeasureServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(Collections.emptyMap());
        when(updateRedisCategorySizeMeasuresInfoServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(Collections.emptyMap());

        executor.doRealJob(contextMock());

        verify(updateRedisTovarTreeServiceMock, times(1))
            .updateTovarTree(any(TovarTree.class));
        verify(updateRedisCategoryServiceMock, times(1))
            .updateCategory(anyLong());
        verify(updateRedisCategorySizeMeasureServiceMock, times(1))
            .updateCategorySizeMeasure(anyLong());
        verify(updateRedisCategorySizeMeasuresInfoServiceMock, times(1))
            .updateCategorySizeMeasuresInfo(anyLong());
        verify(updateRedisTovarTreeServiceMock, never())
            .removeTovarTree();
        verify(updateRedisCategoryServiceMock, never())
            .removeCategory(anyLong(), anyLong());
        verify(updateRedisCategorySizeMeasureServiceMock, never())
            .removeCategorySizeMeasure(anyLong(), anyLong());
        verify(updateRedisCategorySizeMeasuresInfoServiceMock, never())
            .removeCategorySizeMeasuresInfo(anyLong(), anyLong());
    }

    @Test
    public void updateCategory() throws Exception {
        when(updateRedisCategoryServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(ImmutableMap.of(TEST_CATEGORY_1, 0L));
        when(updateRedisCategorySizeMeasureServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(ImmutableMap.of(TEST_CATEGORY_1, 0L));
        when(updateRedisCategorySizeMeasuresInfoServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(ImmutableMap.of(TEST_CATEGORY_1, 0L));

        executor.doRealJob(contextMock());

        verify(updateRedisTovarTreeServiceMock, times(1))
            .updateTovarTree(any(TovarTree.class));
        verify(updateRedisCategoryServiceMock, times(1))
            .updateCategory(anyLong());
        verify(updateRedisCategorySizeMeasureServiceMock, times(1))
            .updateCategorySizeMeasure(anyLong());
        verify(updateRedisCategorySizeMeasuresInfoServiceMock, times(1))
            .updateCategorySizeMeasuresInfo(anyLong());
        verify(updateRedisTovarTreeServiceMock, never())
            .removeTovarTree();
        verify(updateRedisCategoryServiceMock, never())
            .removeCategory(anyLong(), anyLong());
        verify(updateRedisCategorySizeMeasureServiceMock, never())
            .removeCategorySizeMeasure(anyLong(), anyLong());
        verify(updateRedisCategorySizeMeasuresInfoServiceMock, never())
            .removeCategorySizeMeasuresInfo(anyLong(), anyLong());
    }

    @Test
    public void removeCategory() throws Exception {
        when(updateRedisCategoryServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(ImmutableMap.of(REMOVED_CATEGORY, 0L));
        when(updateRedisCategorySizeMeasureServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(ImmutableMap.of(REMOVED_CATEGORY, 0L));
        when(updateRedisCategorySizeMeasuresInfoServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(ImmutableMap.of(REMOVED_CATEGORY, 0L));

        executor.doRealJob(contextMock());

        verify(updateRedisTovarTreeServiceMock, times(1))
            .updateTovarTree(any(TovarTree.class));
        verify(updateRedisCategoryServiceMock, times(1))
            .updateCategory(anyLong());
        verify(updateRedisCategorySizeMeasureServiceMock, times(1))
            .updateCategorySizeMeasure(anyLong());
        verify(updateRedisCategorySizeMeasuresInfoServiceMock, times(1))
            .updateCategorySizeMeasuresInfo(anyLong());
        verify(updateRedisTovarTreeServiceMock, never())
            .removeTovarTree();
        verify(updateRedisCategoryServiceMock, times(1))
            .removeCategory(anyLong(), anyLong());
        verify(updateRedisCategorySizeMeasureServiceMock, times(1))
            .removeCategorySizeMeasure(anyLong(), anyLong());
        verify(updateRedisCategorySizeMeasuresInfoServiceMock, times(1))
            .removeCategorySizeMeasuresInfo(anyLong(), anyLong());
    }

    @Test
    public void updateOldCategoriesFirst() throws Exception {
        tovarTreeServiceMock.addCategory(new TovarCategory("TEST2", TEST_CATEGORY_2, TEST_CATEGORY_1));
        when(updateRedisCategoryServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(ImmutableMap.of(
                TEST_CATEGORY_1, 1L,
                TEST_CATEGORY_2, 0L
            ));
        when(updateRedisCategorySizeMeasureServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(ImmutableMap.of(
                TEST_CATEGORY_1, 1L,
                TEST_CATEGORY_2, 0L
            ));
        when(updateRedisCategorySizeMeasuresInfoServiceMock.getAllCategoriesLastUpdated())
            .thenReturn(ImmutableMap.of(
                TEST_CATEGORY_1, 1L,
                TEST_CATEGORY_2, 0L
            ));

        executor.doRealJob(contextMock());

        ArgumentCaptor<Long> hids = ArgumentCaptor.forClass(Long.class);
        verify(updateRedisCategoryServiceMock, times(2))
            .updateCategory(hids.capture());
        verify(updateRedisCategorySizeMeasureServiceMock, times(2))
            .updateCategorySizeMeasure(hids.capture());
        verify(updateRedisCategorySizeMeasuresInfoServiceMock, times(2))
            .updateCategorySizeMeasuresInfo(hids.capture());
        verify(updateRedisCategoryServiceMock, never())
            .removeCategory(anyLong(), anyLong());
        verify(updateRedisCategorySizeMeasureServiceMock, never())
            .removeCategorySizeMeasure(anyLong(), anyLong());
        verify(updateRedisCategorySizeMeasuresInfoServiceMock, never())
            .removeCategorySizeMeasuresInfo(anyLong(), anyLong());
        Assertions.assertThat(hids.getAllValues())
            .containsExactly(TEST_CATEGORY_2, TEST_CATEGORY_1, TEST_CATEGORY_2, TEST_CATEGORY_1,
                TEST_CATEGORY_2, TEST_CATEGORY_1);
    }

    private JobExecutionContext contextMock() {
        return mock(JobExecutionContext.class);
    }
}

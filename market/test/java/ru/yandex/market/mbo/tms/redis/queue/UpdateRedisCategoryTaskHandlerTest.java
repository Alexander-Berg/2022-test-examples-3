package ru.yandex.market.mbo.tms.redis.queue;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.tovartree.UpdateRedisCategoryTask;
import ru.yandex.market.mbo.tms.redis.UpdateRedisCategoryService;
import ru.yandex.market.mbo.tms.redis.UpdateRedisCategorySizeMeasureService;
import ru.yandex.market.mbo.tms.redis.UpdateRedisCategorySizeMeasuresInfoService;
import ru.yandex.market.mbo.tms.redis.UpdateRedisGetTovarTreeResponseService;
import ru.yandex.market.mbo.tms.redis.UpdateRedisTovarTreeService;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicnumber")
public class UpdateRedisCategoryTaskHandlerTest {

    public static final long HID = 1L;
    public static final long TS = 4L;

    private UpdateRedisTovarTreeService updateRedisTovarTreeService;
    private UpdateRedisGetTovarTreeResponseService updateRedisGetTovarTreeResponseService;
    private UpdateRedisCategoryService updateRedisCategoryService;
    private UpdateRedisCategoryTaskHandler updateRedisCategoryTaskHandler;
    private UpdateRedisCategorySizeMeasureService updateRedisCategorySizeMeasureService;
    private UpdateRedisCategorySizeMeasuresInfoService updateRedisCategorySizeMeasuresInfoService;


    @Before
    public void setUp() {
        updateRedisTovarTreeService = mock(UpdateRedisTovarTreeService.class);
        updateRedisGetTovarTreeResponseService = mock(UpdateRedisGetTovarTreeResponseService.class);
        updateRedisCategoryService = mock(UpdateRedisCategoryService.class);
        updateRedisCategorySizeMeasureService = mock(UpdateRedisCategorySizeMeasureService.class);
        updateRedisCategorySizeMeasuresInfoService = mock(UpdateRedisCategorySizeMeasuresInfoService.class);
        updateRedisCategoryTaskHandler = new UpdateRedisCategoryTaskHandler(
            updateRedisTovarTreeService,
            updateRedisCategoryService,
            updateRedisCategorySizeMeasureService,
            updateRedisCategorySizeMeasuresInfoService
        );
    }

    @Test
    public void testAllUpdated() throws Exception {
        when(updateRedisTovarTreeService.updateTovarTree(anyLong()))
            .thenReturn(RedisUpdateResult.UPDATED);
        when(updateRedisCategoryService.updateCategory(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.UPDATED);
        when(updateRedisCategorySizeMeasureService.updateCategorySizeMeasure(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.UPDATED);
        when(updateRedisCategorySizeMeasuresInfoService.updateCategorySizeMeasuresInfo(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.UPDATED);
        when(updateRedisGetTovarTreeResponseService
                .updateGetTovarTreeResponse(anyLong()))
                .thenReturn(RedisUpdateResult.UPDATED);
        Assertions.assertThat(updateRedisCategoryTaskHandler.handle(new UpdateRedisCategoryTask(HID, TS), null))
            .isEqualTo(RedisUpdateResult.UPDATED);
    }

    @Test
    public void testOneUpdated() throws Exception {
        when(updateRedisTovarTreeService.updateTovarTree(anyLong()))
            .thenReturn(RedisUpdateResult.UPDATED);
        when(updateRedisGetTovarTreeResponseService
                .updateGetTovarTreeResponse(anyLong()))
                .thenReturn(RedisUpdateResult.SKIPPED);
        when(updateRedisCategoryService.updateCategory(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.SKIPPED);
        when(updateRedisCategorySizeMeasureService.updateCategorySizeMeasure(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.SKIPPED);
        when(updateRedisCategorySizeMeasuresInfoService.updateCategorySizeMeasuresInfo(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.SKIPPED);
        Assertions.assertThat(updateRedisCategoryTaskHandler.handle(new UpdateRedisCategoryTask(HID, TS), null))
            .isEqualTo(RedisUpdateResult.UPDATED);
    }

    @Test
    public void testAllSkipped() throws Exception {
        when(updateRedisTovarTreeService.updateTovarTree(anyLong()))
            .thenReturn(RedisUpdateResult.SKIPPED);
        when(updateRedisCategoryService.updateCategory(anyLong(), anyLong()))
                .thenReturn(RedisUpdateResult.SKIPPED);
        when(updateRedisGetTovarTreeResponseService
                .updateGetTovarTreeResponse(anyLong()))
                .thenReturn(RedisUpdateResult.SKIPPED);
        when(updateRedisCategorySizeMeasureService.updateCategorySizeMeasure(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.SKIPPED);
        when(updateRedisCategorySizeMeasuresInfoService.updateCategorySizeMeasuresInfo(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.SKIPPED);
        Assertions.assertThat(updateRedisCategoryTaskHandler.handle(new UpdateRedisCategoryTask(HID, TS), null))
            .isEqualTo(RedisUpdateResult.SKIPPED);
    }

    @Test
    public void testAllFailed() throws Exception {
        when(updateRedisTovarTreeService.updateTovarTree(anyLong()))
            .thenReturn(RedisUpdateResult.FAILED);
        when(updateRedisCategoryService.updateCategory(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.FAILED);
        when(updateRedisGetTovarTreeResponseService
                .updateGetTovarTreeResponse(anyLong()))
                .thenReturn(RedisUpdateResult.FAILED);
        when(updateRedisCategorySizeMeasureService.updateCategorySizeMeasure(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.FAILED);
        when(updateRedisCategorySizeMeasuresInfoService.updateCategorySizeMeasuresInfo(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.FAILED);
        Assertions.assertThat(updateRedisCategoryTaskHandler.handle(new UpdateRedisCategoryTask(HID, TS), null))
            .isEqualTo(RedisUpdateResult.FAILED);
    }

    @Test
    public void testOneFailed() throws Exception {
        when(updateRedisTovarTreeService.updateTovarTree(anyLong()))
            .thenReturn(RedisUpdateResult.SKIPPED);
        when(updateRedisCategoryService.updateCategory(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.FAILED);
        when(updateRedisGetTovarTreeResponseService
                .updateGetTovarTreeResponse(anyLong()))
                .thenReturn(RedisUpdateResult.UPDATED);
        when(updateRedisCategorySizeMeasureService.updateCategorySizeMeasure(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.UPDATED);
        when(updateRedisCategorySizeMeasuresInfoService.updateCategorySizeMeasuresInfo(anyLong(), anyLong()))
            .thenReturn(RedisUpdateResult.UPDATED);
        Assertions.assertThat(updateRedisCategoryTaskHandler.handle(new UpdateRedisCategoryTask(HID, TS), null))
            .isEqualTo(RedisUpdateResult.FAILED);
    }
}

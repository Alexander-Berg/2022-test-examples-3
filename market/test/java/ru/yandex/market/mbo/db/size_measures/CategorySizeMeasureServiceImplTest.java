package ru.yandex.market.mbo.db.size_measures;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureDto;
import ru.yandex.market.mbo.redis.categories.CategorySizeMeasureRedisCache;
import ru.yandex.market.mbo.redis.categories.CategorySizeMeasuresInfoRedisCache;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author galaev@yandex-team.ru
 * @since 05/12/2018.
 */
public class CategorySizeMeasureServiceImplTest {

    private CategorySizeMeasureRedisCache sizeMeasureRedisCache;
    private CategorySizeMeasuresInfoRedisCache sizeMeasuresInfoRedisCache;
    private CategorySizeMeasureService service;

    @Before
    public void setUp() {
        EnhancedRandom random = new EnhancedRandomBuilder()
            .seed(1)
            .build();
        SizeMeasureDto sizeMeasureDto = random.nextObject(SizeMeasureDto.class);
        SizeMeasureService measureService = Mockito.mock(SizeMeasureService.class);
        Mockito.when(measureService.listSizeMeasureScales(any())).thenReturn(Collections.emptyList());
        Mockito.when(measureService.listSizeMeasures(eq(1L), anyLong()))
            .thenReturn(Collections.singletonList(sizeMeasureDto));
        Mockito.when(measureService.listSizeMeasures(eq(2L)))
            .thenReturn(Collections.emptyList());
        sizeMeasureRedisCache = Mockito.mock(CategorySizeMeasureRedisCache.class);
        sizeMeasuresInfoRedisCache = Mockito.mock(CategorySizeMeasuresInfoRedisCache.class);
        service = new CategorySizeMeasureServiceImpl(measureService, sizeMeasureRedisCache, sizeMeasuresInfoRedisCache);
    }

    @Test
    public void testGetSizeMeasuresInfo() {
        MboSizeMeasures.GetSizeMeasuresInfoRequest request = MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(1)
            .setRedisCacheTs(0L)
            .build();
        MboSizeMeasures.GetSizeMeasuresInfoResponse response = service.getSizeMeasuresInfo(request);

        Assertions.assertThat(response.getSizeMeasuresList()).hasSize(1);
    }

    @Test
    public void testGetSizeMeasuresInfoEmptyResponse() {
        MboSizeMeasures.GetSizeMeasuresInfoRequest request = MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(2)
            .setRedisCacheTs(0L)
            .build();
        MboSizeMeasures.GetSizeMeasuresInfoResponse response = service.getSizeMeasuresInfo(request);

        Assertions.assertThat(response.getSizeMeasuresList()).hasSize(0);
    }
}

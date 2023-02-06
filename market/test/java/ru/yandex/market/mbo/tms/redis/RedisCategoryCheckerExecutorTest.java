package ru.yandex.market.mbo.tms.redis;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.redis.categories.CategoryRedisReader;
import ru.yandex.market.mbo.redis.categories.CategorySizeMeasureRedisReader;
import ru.yandex.market.mbo.redis.categories.CategorySizeMeasuresInfoRedisReader;
import ru.yandex.market.mbo.utils.OracleDbTimestampGetter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

public class RedisCategoryCheckerExecutorTest {

    private static final long ROOT = 333L;
    private static final long CATEGORY_1 = 101L;
    private static final long CATEGORY_2 = 102L;

    private TovarTreeServiceMock tovarTreeService;
    private CategoryRedisReader categoryRedisReader;
    private CategorySizeMeasureRedisReader categorySizeMeasureRedisReader;
    private CategorySizeMeasuresInfoRedisReader categorySizeMeasuresInfoRedisReader;
    private OracleDbTimestampGetter oracleDbTimestampGetter;
    private RedisCategoryCheckerExecutor redisCategoryCheckerExecutor;


    @Before
    public void setUp() {
        tovarTreeService = new TovarTreeServiceMock();
        tovarTreeService.addCategory(new TovarCategory("root", ROOT, -1));
        tovarTreeService.addCategory(new TovarCategory("1", CATEGORY_1, ROOT));
        tovarTreeService.addCategory(new TovarCategory("2", CATEGORY_2, ROOT));

        categoryRedisReader = Mockito.mock(CategoryRedisReader.class);
        categorySizeMeasureRedisReader = Mockito.mock(CategorySizeMeasureRedisReader.class);
        categorySizeMeasuresInfoRedisReader = Mockito.mock(CategorySizeMeasuresInfoRedisReader.class);
        oracleDbTimestampGetter = Mockito.mock(OracleDbTimestampGetter.class);
        redisCategoryCheckerExecutor = new RedisCategoryCheckerExecutor(tovarTreeService,
            categoryRedisReader, categorySizeMeasureRedisReader, categorySizeMeasuresInfoRedisReader,
            oracleDbTimestampGetter);
    }

    @Test
    public void shouldFailIfFoundFailedCategories() {
        Mockito.when(oracleDbTimestampGetter.getCurrentUtcMillis()).thenReturn(Long.MAX_VALUE);
        Mockito.when(categoryRedisReader.getCategoryTs(ROOT)).thenReturn(Optional.of(Long.MAX_VALUE));
        Mockito.when(categoryRedisReader.getCategoryTs(CATEGORY_1)).thenReturn(Optional.empty());
        Mockito.when(categoryRedisReader.getCategoryTs(CATEGORY_2)).thenReturn(Optional.of(1L));
        Mockito.when(categorySizeMeasureRedisReader.getCategoryTs(ROOT)).thenReturn(Optional.of(Long.MAX_VALUE));
        Mockito.when(categorySizeMeasureRedisReader.getCategoryTs(CATEGORY_1)).thenReturn(Optional.empty());
        Mockito.when(categorySizeMeasureRedisReader.getCategoryTs(CATEGORY_2)).thenReturn(Optional.of(1L));
        Mockito.when(categorySizeMeasuresInfoRedisReader.getCategoryTs(ROOT)).thenReturn(Optional.of(Long.MAX_VALUE));
        Mockito.when(categorySizeMeasuresInfoRedisReader.getCategoryTs(CATEGORY_1)).thenReturn(Optional.empty());
        Mockito.when(categorySizeMeasuresInfoRedisReader.getCategoryTs(CATEGORY_2)).thenReturn(Optional.of(1L));

        Assertions.assertThatThrownBy(() -> redisCategoryCheckerExecutor.doRealJob(null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(String.valueOf(CATEGORY_1))
            .hasMessageContaining(String.valueOf(CATEGORY_2))
            .hasMessageNotContaining(String.valueOf(ROOT));
    }


    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldNotFailIfCategoriesUpToDate() throws Exception {
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000;
        Mockito.when(oracleDbTimestampGetter.getCurrentUtcMillis()).thenReturn(now);

        Mockito.when(categoryRedisReader.getCategoryTs(ROOT)).thenReturn(Optional.of(now));
        Mockito.when(categoryRedisReader.getCategoryTs(CATEGORY_1)).thenReturn(Optional.of(now));
        Mockito.when(categoryRedisReader.getCategoryTs(CATEGORY_2)).thenReturn(Optional.of(now));
        Mockito.when(categorySizeMeasureRedisReader.getCategoryTs(ROOT)).thenReturn(Optional.of(now));
        Mockito.when(categorySizeMeasureRedisReader.getCategoryTs(CATEGORY_1)).thenReturn(Optional.of(now));
        Mockito.when(categorySizeMeasureRedisReader.getCategoryTs(CATEGORY_2)).thenReturn(Optional.of(now));
        Mockito.when(categorySizeMeasuresInfoRedisReader.getCategoryTs(ROOT)).thenReturn(Optional.of(now));
        Mockito.when(categorySizeMeasuresInfoRedisReader.getCategoryTs(CATEGORY_1)).thenReturn(Optional.of(now));
        Mockito.when(categorySizeMeasuresInfoRedisReader.getCategoryTs(CATEGORY_2)).thenReturn(Optional.of(now));

        redisCategoryCheckerExecutor.doRealJob(null);
    }
}

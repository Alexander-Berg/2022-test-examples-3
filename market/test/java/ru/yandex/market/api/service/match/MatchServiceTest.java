package ru.yandex.market.api.service.match;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.yandex.market.api.category.CategoryService;
import ru.yandex.market.api.category.RestrictionService;
import ru.yandex.market.api.controller.Parameters;
import ru.yandex.market.api.domain.Category;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v1.CategoryInfo;
import ru.yandex.market.api.domain.v1.MatchedCategoryV1;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.ir.classifier.ClassifierClient;
import ru.yandex.market.api.server.version.CategoryVersion;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
@WithMocks
public class MatchServiceTest extends BaseTest {

    private static final CategoryInfo NORMAL_CATEGORY = new CategoryInfo() {{
        setId(1);
        setName("Normal");
        setAdult(null);
    }};

    private static final CategoryInfo ADULT_CATEGORY = new CategoryInfo() {{
        setId(2);
        setName("Adult");
        setAdult(Boolean.TRUE);
    }};

    private static final List<Category> CATEGORIES = Lists.newArrayList(
        NORMAL_CATEGORY,
        ADULT_CATEGORY
    );

    private static final Int2DoubleMap CATEGORY_RATES = new Int2DoubleArrayMap() {{
        put(1, 1.0);
        put(2, 0.5);
    }};

    @Mock
    ClassifierClient classifierClient;

    @Mock
    CategoryService categoryService;

    @Mock
    RestrictionService restrictionService;

    @InjectMocks
    private MatchService matchService;

    @Test
    public void shouldFilterAdultCategories() throws Exception {
        when(classifierClient.classify(any(ModelMatchRequest.class), anyInt()))
            .thenReturn(Pipelines.startWith(Futures.newSucceededFuture(CATEGORY_RATES)));

        when(categoryService.getCategoryInfos(any(IntCollection.class), anyCollectionOf(Field.class), any(CategoryVersion.class)))
            .thenReturn(CATEGORIES);

        when(restrictionService.isRestricted(anyInt(), anyInt(), anyCollectionOf(Parameters.Section.class)))
            .thenReturn(true, false);

        List<MatchedCategoryV1> now = matchService.<MatchedCategoryV1>matchCategories(new ModelMatchRequest(),
                PageInfo.fromTotalPages(1, 30, 1),null).getNow();
        assertEquals(1, now.size());
    }

    @Test
    public void shouldShowAdultCategories() throws Exception {
        when(classifierClient.classify(any(ModelMatchRequest.class), anyInt()))
            .thenReturn(Pipelines.startWith(Futures.newSucceededFuture(CATEGORY_RATES)));

        when(categoryService.getCategoryInfos(any(IntCollection.class), anyCollectionOf(Field.class), any(CategoryVersion.class)))
            .thenReturn(CATEGORIES);

        when(restrictionService.isRestricted(anyInt(), anyInt(), anyCollectionOf(Parameters.Section.class)))
            .thenReturn(false, false);

        List<MatchedCategoryV1> now = matchService.<MatchedCategoryV1>matchCategories(new ModelMatchRequest(),
                PageInfo.fromTotalPages(1, 30, 1),null).getNow();
        assertEquals(2, now.size());
    }
}

package ru.yandex.market.api.category;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.yandex.market.api.common.client.MarketTypeResolver;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.cataloger.CatalogerService;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Created by fettsery on 01.08.18.
 */
@WithMocks
public class CategoryIdResolverTest extends UnitTestBase {
    @InjectMocks
    CategoryIdResolver categoryIdResolver;

    @Mock
    CategoryService categoryService;

    @Mock
    CatalogerService catalogerService;

    @Mock
    MarketTypeResolver marketTypeResolver;

    @Test
    public void shouldReturnOnlyNidIfThereIsNoHid() {

        //Нет категории в кэше
        when(categoryService.getHidByNid(eq(58898)))
            .thenReturn(0);

        //Нет такого hid
        when(catalogerService.getNavigationCategoryByHid(eq(58898), anyInt(), any(), any()))
            .thenReturn(Pipelines.startWithValue(null));

        CategoryId categoryId = Futures.waitAndGet(categoryIdResolver.resolve(CategoryId.of(58898, 0)));

        assertEquals(CategoryId.of(58898, 0), categoryId);
    }


}

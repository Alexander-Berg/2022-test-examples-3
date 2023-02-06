package ru.yandex.market.mbo.db.category_wiki;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWiki;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;

/**
 * @author galaev
 * @since 2019-01-28
 */
public class CachedCategoryWikiServiceTest {

    private CategoryWikiService service;
    private CachedCategoryWikiService cachedService;

    @Before
    public void setUp() {
        service = Mockito.mock(CategoryWikiService.class);
        Mockito.when(service.getCategoryWiki(anyLong())).thenReturn(new CategoryWiki().setCategoryId(1));
        Mockito.when(service.getCategoryWikiMap(anyCollection())).thenReturn(ImmutableList.of(
            new CategoryWiki().setCategoryId(1), new CategoryWiki().setCategoryId(2)
        ));

        cachedService = new CachedCategoryWikiService(service);
    }

    @Test
    public void testGetCategoryWiki() {
        CategoryWiki wiki = cachedService.getCategoryWiki(1);
        Assertions.assertThat(wiki.getCategoryId()).isEqualTo(1);
    }

    @Test
    public void testPreloadCache() {
        cachedService.preloadCache(ImmutableList.of(1L, 2L));

        CategoryWiki wiki = cachedService.getCategoryWiki(2);
        Assertions.assertThat(wiki.getCategoryId()).isEqualTo(2);

        Mockito.verify(service, times(1)).getCategoryWikiMap(anyCollection());
        Mockito.verifyNoMoreInteractions(service);
    }
}

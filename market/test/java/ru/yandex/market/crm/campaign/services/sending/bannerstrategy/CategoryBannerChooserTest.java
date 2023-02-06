package ru.yandex.market.crm.campaign.services.sending.bannerstrategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.core.domain.categories.CategoryImageLink;
import ru.yandex.market.crm.core.services.sending.bannerstrategy.BannerStrategyContext;
import ru.yandex.market.crm.core.services.sending.bannerstrategy.CategoryBannerChooser;
import ru.yandex.market.crm.core.suppliers.CategoriesImageLinksSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author apershukov
 */
public class CategoryBannerChooserTest {

    private static final String IMAGE_1 = "http://image-1.png";
    private static final String IMAGE_2 = "http://image-2.png";

    private CategoryBannerChooser chooser;

    private static CategoryImageLink image(String url, Set<String> tags) {
        return new CategoryImageLink(111, url, "", tags);
    }

    @Before
    public void setUp() throws Exception {
        Int2ObjectMap<List<CategoryImageLink>> data = new Int2ObjectArrayMap<>();
        data.put(111, Arrays.asList(
                image(IMAGE_1, ImmutableSet.of("S")),
                image(IMAGE_2, ImmutableSet.of("tag1", "M")),
                image("http://image-3.png", Collections.emptySet())
        ));

        CategoriesImageLinksSupplier cache = mock(CategoriesImageLinksSupplier.class);
        when(cache.get()).thenReturn(data);

        chooser = new CategoryBannerChooser(cache);
    }

    @Test
    public void testNullIfNoImageFound() {
        assertNull(choose(ImmutableSet.of("iddqd")));
    }

    @Test
    public void testFirstTagHasHighestPriority() {
        CategoryImageLink link = choose(ImmutableSet.of("tag1", "M"));
        assertNotNull(link);
        assertEquals(IMAGE_2, link.getImageUrl());
    }

    @Test
    public void testSelectUsedImagesOfNoNewLeft() {
        BannerStrategyContext ctx = new BannerStrategyContext();
        ctx.add(image(IMAGE_2, ImmutableSet.of("tag1", "M")));
        CategoryImageLink link = chooser.chooseImage(111, ImmutableSet.of("tag1", "M"), ctx);
        assertNotNull(link);
        assertEquals(IMAGE_2, link.getImageUrl());
    }

    @Test
    public void testNullIfOneTagNotFound() {
        CategoryImageLink link = choose(ImmutableSet.of("iddqd", "M"));
        assertNull(link);
    }

    @Test
    public void testUseDefaultTagIfNoTagsSpecified() {
        CategoryImageLink link = choose(null);
        assertNotNull(link);
        assertEquals(IMAGE_1, link.getImageUrl());
    }

    private CategoryImageLink choose(Set<String> possibleTags) {
        return chooser.chooseImage(111, possibleTags, new BannerStrategyContext());
    }
}

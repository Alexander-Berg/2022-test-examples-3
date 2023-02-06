package ru.yandex.market.api.vendor.load;

import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.market.api.common.url.ContextUrlNormalizer;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.api.vendor.Category;
import ru.yandex.market.api.vendor.Vendor;

import static org.junit.Assert.assertEquals;

/**
 * Created by anton0xf on 29.12.16.
 */
public class CatalogerVendorWithCategoriesParserTest extends UnitTestBase {

    @Test
    public void testParseCatalogerVendorCategories() {
        int vendorId = 1042232;
        Vendor expected = new Vendor(vendorId, "Horizon", "http://www.world.horizonfitness.com",
            "https://mdata.yandex.net/i?path=b1125192216_img_id9076591173493315833.png", false);
        expected.setCategories(ImmutableList.of(
                category(91512, "Товары для спорта и отдыха", vendorId,
                        category(91525, "Беговые дорожки", vendorId),
                        category(91524, "Велотренажеры", vendorId)),
            category(198119, "Электроника",
                vendorId, category(226665, "Бинокли и зрительные трубы", vendorId))));

        CatalogerVendorWithCategoriesParser parser = new CatalogerVendorWithCategoriesParser(
            Collections.emptyList(),
                new UrlParamsFactoryImpl(null, null, new ContextUrlNormalizer(UrlSchema.HTTPS), null)
        );
        Vendor res = parser.parse(ResourceHelpers.getResource("cataloger.vendor.categories.xml"));
        assertEquals(expected, res);
    }

    @NotNull
    private static Category category(int id, String name, int vendorId, Category... inner) {
        Category res = new Category(id, name, 0, 0.0);
        res.setFilterId(-11L);
        res.setFilterValueId((long)vendorId);
        Arrays.stream(inner).forEach(c -> res.addInnerCategory(c));
        return res;
    }

}

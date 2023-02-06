package ru.yandex.market.api.internal.cataloger;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.catalog.NavigationCategoryV1;
import ru.yandex.market.api.domain.catalog.NavigationIcon;
import ru.yandex.market.api.domain.catalog.NavigationMenu;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Arrays;

/**
 *
 */
@WithContext
public class NavigationMenuDeserializationTest extends UnitTestBase {

    @Test
    public void shouldParseNormalResult() throws Exception {
        NavigationMenuCatalogerResponse resp = new NavigationMenuResponseParser().parse(
            ResourceHelpers.getResource("3-level-menu.xml")
        );
        NavigationMenu menu = resp.getMenu();
        Assert.assertNotNull(menu);
        Assert.assertEquals(57964, menu.getId());
        Assert.assertEquals(15, menu.getCategories().size());
        NavigationCategoryV1 category = new NavigationCategoryV1(54440, 198119, "Электроника", null, null, null);
        category.setIcons(Arrays.asList(new NavigationIcon("url-electronic")));
        category.setChildren(Arrays.asList(
            new NavigationCategoryV1(54726, 91491, "Мобильные телефоны", null, null, null),
            new NavigationCategoryV1(58429, 6427100, "Планшеты", null, null, null),
            new NavigationCategoryV1(56034, 10498025, "Умные часы и браслеты", null, null, null),
            new NavigationCategoryV1(54719, 91497, "Аксессуары для телефонов", null, null,
                Arrays.asList(
                    new NavigationCategoryV1(56036, 91498, "Чехлы", null, null, null),
                    new NavigationCategoryV1(54714, 418706, "Bluetooth-гарнитуры", null, null, null),
                    new NavigationCategoryV1(56035, 8353924, "Внешние аккумуляторы", null, null, null),
                    new NavigationCategoryV1(58444, 91032, "Карты памяти", null, null, null),
                    new NavigationCategoryV1(56024, 91499, "Аккумуляторы", null, null, null)
                )),
            new NavigationCategoryV1(54730, 91464, "Радиотелефоны", null, null, null),
            new NavigationCategoryV1(54771, 91470, "Рации", null, null, null)
        ));
        Assert.assertEquals(category, menu.getCategories().get(0));
    }

    @Test
    public void shouldParseError() throws Exception {
        NavigationMenuCatalogerResponse resp = new NavigationMenuResponseParser().parse(
            ResourceHelpers.getResource("error.xml")
        );
        Assert.assertNull(resp.getMenu());
        Assert.assertTrue(resp.isError());
        Assert.assertEquals("bad domain or menu name", resp.getError());
    }
}

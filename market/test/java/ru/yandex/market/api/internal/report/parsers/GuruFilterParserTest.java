package ru.yandex.market.api.internal.report.parsers;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.category.*;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
public class GuruFilterParserTest extends UnitTestBase {


    @Test
    public void shouldParseShopFilter() throws Exception {
        Filter filter = new GuruFilterParser().parse(
            ResourceHelpers.getResource("guru-filter-shops-part.xml")
        );

        Assert.assertEquals("-6", filter.getId());
        Assert.assertEquals("Магазины", filter.getName());
        Assert.assertEquals("shop", filter.getShortname());
        Assert.assertEquals(FilterType.ENUMERATOR, filter.getType());
        Assert.assertEquals(FilterSubType.MULTI_CHOICE, filter.getSubType());

        List<? extends EnumValue> shops = ((EnumFilter) filter).getOptions();
        Assert.assertEquals(3, shops.size());

        EnumValueShop shop1 = (EnumValueShop) shops.get(0);
        Assert.assertEquals("108034", shop1.getValueId());
        Assert.assertEquals("BigAp.ru", shop1.getValueText());
        Assert.assertEquals("2", shop1.getCount());

        EnumValueShop shop2 = (EnumValueShop) shops.get(1);
        Assert.assertEquals("108546", shop2.getValueId());
        Assert.assertEquals("ТехноГид", shop2.getValueText());
        Assert.assertEquals("1", shop2.getCount());

        EnumValueShop shop3 = (EnumValueShop) shops.get(2);
        Assert.assertEquals("81414", shop3.getValueId());
        Assert.assertEquals("AppAvenue", shop3.getValueText());
        Assert.assertEquals("1", shop3.getCount());
    }
}

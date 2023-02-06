package ru.yandex.market.api.internal.basket.parsers;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.WishListItem;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Arrays;
import java.util.List;

/**
 * Created by vivg on 28.07.16.
 */
@WithContext
public class ItemsParserTest extends UnitTestBase {

    @Test
    public void usual() throws Exception {
        ItemsParser<WishListItem> parser = new ItemsParser();
        List<WishListItem> items = parser.parse(ResourceHelpers.getResource("items.json"));
        Assert.assertEquals(3, items.size());

        WishListItem item = items.get(0);
        Assert.assertEquals(233560763, item.getId());
        Assert.assertEquals(1198681542, item.getModelId());
        Assert.assertEquals(new Long(1448734337), item.getUnixTimestamp());
        Assert.assertEquals(Arrays.asList(233560420L), item.getLabelIds());

        item = items.get(1);
        Assert.assertEquals(233561194, item.getId());
        Assert.assertEquals(11031621, item.getModelId());
        Assert.assertEquals(new Long(1449337502), item.getUnixTimestamp());
        Assert.assertEquals(Arrays.asList(233560421L), item.getLabelIds());

        item = items.get(2);
        Assert.assertEquals(233575886, item.getId());
        Assert.assertEquals(13324087, item.getModelId());
        Assert.assertEquals(new Long(1460570912), item.getUnixTimestamp());
        Assert.assertEquals(Arrays.asList(233560422L), item.getLabelIds());
    }
}

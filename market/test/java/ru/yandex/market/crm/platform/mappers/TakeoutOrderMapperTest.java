package ru.yandex.market.crm.platform.mappers;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.mappers.takeout.orderlog.TakeoutBlueOrderMapper;
import ru.yandex.market.crm.platform.mappers.takeout.orderlog.TakeoutOrderMapper;
import ru.yandex.market.crm.platform.mappers.takeout.orderlog.TakeoutRedOrderMapper;
import ru.yandex.market.crm.platform.models.TakeoutOrder;
import ru.yandex.market.crm.util.ResourceHelpers;

//TODO entarrion расширить набор тестов - в рамках тикета MSTAT-8108
public class TakeoutOrderMapperTest {
    private byte[] resource;

    private static TakeoutOrder extractOrder(TakeoutOrderMapper mapper, byte[] resource) {
        return Iterables.getFirst(mapper.apply(resource), null);
    }

    @Test
    public void testParseBlue() {
        TakeoutOrder order = extractOrder(new TakeoutBlueOrderMapper(), resource);
        Assert.assertNotNull(order);
    }

    @Test
    public void testParseRed() {
        TakeoutOrder order = extractOrder(new TakeoutRedOrderMapper(), resource);
        Assert.assertNull(order);
    }

    @Before
    public void setUp() {
        resource = ResourceHelpers.getResource("order.json");
    }
}

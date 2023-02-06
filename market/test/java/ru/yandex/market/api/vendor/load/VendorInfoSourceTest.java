package ru.yandex.market.api.vendor.load;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.api.vendor.Vendor;


public class VendorInfoSourceTest extends Assert {

    private VendorInfoParser vendorInfoParser = new VendorInfoParser();

    @Test
    public void testParse() {
        final Long2ObjectMap<Vendor> actual = vendorInfoParser.parse(
            ResourceHelpers.getResource("global.vendors.xml"));

        final Long2ObjectOpenHashMap<Vendor> expected = new Long2ObjectOpenHashMap<Vendor>() {{
            put(152691, new Vendor(152691, "Acorp", "www.acorp.ru", "http://mdata.yandex.ru/i?path=a_1869969__2029.gif", false));
            put(152701, new Vendor(152701, "BBK", "www.bbk.ru", null, false));
            put(152708, new Vendor(152708, "Mabe", "www.mabe.ru", "http://mdata.yandex.ru/i?path=b0206143024_1.gif", false));
            put(152724, new Vendor(152724, "Kalpa", null, "http://mdata.yandex.ru/i?path=a_1870101__336.gif", false));
            put(448899, new Vendor(448899, "Season", null, null, false));
            put(15728405, new Vendor(15728405, "Яндекс Маркет", null, null, true));
        }};

        assertEquals(expected, actual);
    }
}

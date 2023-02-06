package ru.yandex.market.api.vendor.load;

import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import ru.yandex.market.api.common.url.CommonMarketUrls;
import ru.yandex.market.api.domain.v1.VendorField;
import ru.yandex.market.api.domain.v2.VendorV2;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.internal.report.parsers.json.CatalogerVendorsJsonParser;
import ru.yandex.market.api.server.version.VendorVersion;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.Converters;
import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.api.vendor.Vendor;
import ru.yandex.market.parser.json.AbstractJsonParser;

import static org.junit.Assert.assertEquals;

/**
 * Created by anton0xf on 29.12.16.
 */
@WithContext
@WithMocks
public class VendorV2JsonParserTest extends BaseTest {
    @Inject
    CommonMarketUrls commonMarketUrls;

    Converters converters;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.setUrlSchema(UrlSchema.HTTP);
    }

    @Test
    public void testParseCatalogerVendorCategories() {
        VendorV2 expected = new VendorV2(new Vendor(153061, "Samsung", "http://www.samsung.com/ru/home",
            "http://mdata.yandex.net/i?path=b1125192406_img_id730689178734219159.png", false));

        EnumSet<VendorField> fields = EnumSet.allOf(VendorField.class);
        AbstractJsonParser<List<VendorV2>> parser = new CatalogerVendorsJsonParser(
            fields,
            VendorVersion.V2,
            commonMarketUrls,
            converters
        );
        List<VendorV2> res = parser.parse(ResourceHelpers.getResource("cataloger.vendors.json"));
        assertEquals(1, res.size());
        assertEquals(expected.getId(), res.get(0).getId());
        assertEquals(expected.getCategories(), res.get(0).getCategories());
        assertEquals(expected.getName(), res.get(0).getName());
        assertEquals(expected.getPicture(), res.get(0).getPicture());
        assertEquals(expected.getSite(), res.get(0).getSite());
    }

}

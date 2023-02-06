package ru.yandex.market.mbo.db.vendor;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.db.params.ParameterProtoConverter;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.http.MboVendors;

import java.util.Collections;

/**
 * @author dmserebr
 * @date 13.04.18
 */
public class GlobalVendorConverterTest {

    @Test
    public void testEmptyVendorConversion() {
        GlobalVendor globalVendor = GlobalVendorBuilder.newBuilder()
            .setId(1L)
            .setName("name")
            .build();

        MboVendors.GlobalVendor protoVendor =
            GlobalVendorConverter.toProto(globalVendor, Collections.emptyList(),  Collections.emptyList());
        MboVendors.GlobalVendor expected = MboVendors.GlobalVendor.newBuilder()
            .setId(1L)
            .addName(ParameterProtoConverter.convert(WordUtil.defaultWord("name")))
            .setSite("")
            .setComment("")
            .setLogoPosition(MboVendors.Position.RIGHT)
            .setPublished(false)
            .setPictureUrl("")
            .build();
        Assert.assertEquals(expected, protoVendor);
    }

    @Test
    public void testNonEmptyVendorConversion() {
        GlobalVendor globalVendor = GlobalVendorBuilder.newBuilder()
            .setId(1L)
            .setName("name1")
            .addAlias("alias1")
            .addAlias("alias2")
            .setPublished(true)
            .setPictureUrl("http://url1.com")
            .setIsFakeVendor(true)
            .setIsRequireGtinBarcodes(true)
            .build();

        MboVendors.GlobalVendor protoVendor =
            GlobalVendorConverter.toProto(globalVendor, Collections.emptyList(),  Collections.emptyList());
        MboVendors.GlobalVendor expected = MboVendors.GlobalVendor.newBuilder()
            .setId(1L)
            .addName(ParameterProtoConverter.convert(WordUtil.defaultWord("name1")))
            .addAlias(ParameterProtoConverter.convert(WordUtil.defaultWord("alias1")))
            .addAlias(ParameterProtoConverter.convert(WordUtil.defaultWord("alias2")))
            .setPublished(true)
            .setPictureUrl("http://url1.com")
            .setSite("")
            .setComment("")
            .setLogoPosition(MboVendors.Position.RIGHT)
            .setIsFakeVendor(true)
            .setIsRequireGtinBarcodes(true)
            .build();
        Assert.assertEquals(expected, protoVendor);
    }
}

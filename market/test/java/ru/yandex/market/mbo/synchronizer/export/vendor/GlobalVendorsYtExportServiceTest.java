package ru.yandex.market.mbo.synchronizer.export.vendor;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.MboVendors;

/**
 * @author kravchenko-aa
 * @date 2019-08-15
 */
public class GlobalVendorsYtExportServiceTest {
    private static final long VENDOR_ID = 1;
    public static final String VENDOR_NAME = "BigCompany";

    @Test
    public void columnTest() {
        MboVendors.GlobalVendor vendor = MboVendors.GlobalVendor.newBuilder()
            .setPublished(true)
            .setId(VENDOR_ID)
            .setSite("")
            .setPictureUrl("")
            .setComment("")
            .setLogoPosition(MboVendors.Position.RIGHT)
            .addName(MboParameters.Word.newBuilder()
                .setLangId(Language.RUSSIAN.getId())
                .setName(VENDOR_NAME)
                .setMorphologicalProcessing(true)
            )
            .build();

        YTreeMapNode mapNode = GlobalVendorsYtExportService.mapVendor(vendor);

        Assert.assertEquals(vendor.getId(),
            mapNode.get(GlobalVendorsYtExportService.ID).get().intValue());
        Assert.assertEquals(vendor.getName(0).getName(),
            mapNode.get(GlobalVendorsYtExportService.NAME).get().stringValue());
        Assert.assertEquals(vendor.getPublished(),
            mapNode.get(GlobalVendorsYtExportService.IS_PUBLISHED).get().boolValue());
    }
}

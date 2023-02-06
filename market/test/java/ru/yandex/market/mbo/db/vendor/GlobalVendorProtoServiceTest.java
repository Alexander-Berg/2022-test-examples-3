package ru.yandex.market.mbo.db.vendor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.vendor.LogoPosition;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.http.MboVendors;

/**
 * @author ayratgdl
 * @date 19.04.18
 */
public class GlobalVendorProtoServiceTest {
    private static final long UID = 1;

    private GlobalVendorDBMock vendorDBMock;
    private GlobalVendorProtoService vendorProtoService;

    @Before
    public void setUp() throws Exception {
        vendorDBMock = new GlobalVendorDBMock();
        GlobalVendorLoaderService globalVendorLoaderService = new GlobalVendorLoaderService();
        GlobalVendorService vendorService = new GlobalVendorService();
        globalVendorLoaderService.setVendorDb(vendorDBMock);
        vendorService.setVendorDb(vendorDBMock);
        vendorService.setGlobalVendorLoaderService(globalVendorLoaderService);
        vendorProtoService = new GlobalVendorProtoService(vendorService);
    }

    @Test
    public void searchVendorsByIdWhenNoVendors() {
        MboVendors.SearchVendorsRequest request = MboVendors.SearchVendorsRequest.newBuilder()
            .addId(1)
            .build();

        MboVendors.SearchVendorsResponse actualResponse = vendorProtoService.searchVendors(request);

        MboVendors.SearchVendorsResponse expectedResponse = MboVendors.SearchVendorsResponse.newBuilder()
            .setTotalCount(0)
            .build();
        Assert.assertEquals(expectedResponse, actualResponse);
    }


    @Test
    public void searchVendorsByNameWhenNoVendors() {
        MboVendors.SearchVendorsRequest request = MboVendors.SearchVendorsRequest.newBuilder()
            .setNameSubstring("vendorName")
            .build();

        MboVendors.SearchVendorsResponse actualResponse = vendorProtoService.searchVendors(request);

        MboVendors.SearchVendorsResponse expectedResponse = MboVendors.SearchVendorsResponse.newBuilder()
            .setTotalCount(0)
            .build();
        Assert.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void searchVendorsById() {
        GlobalVendor vendor = new GlobalVendor();
        vendor.addName(new Word(0, Language.RUSSIAN.getId(), "Vendor 101", false));
        vendor.setPublished(true);
        vendor.setSite("example.org");
        vendor.setComment("comment");
        vendor.setPictureUrl("image.example.org/image.png");
        vendor.setLogoPosition(LogoPosition.RIGHT);
        long vendorId = vendorDBMock.createVendor(vendor, UID);

        MboVendors.SearchVendorsRequest request = MboVendors.SearchVendorsRequest.newBuilder()
            .addId(vendorId)
            .build();

        MboVendors.SearchVendorsResponse actualResponse = vendorProtoService.searchVendors(request);

        MboVendors.SearchVendorsResponse expectedResponse = MboVendors.SearchVendorsResponse.newBuilder()
            .setTotalCount(1)
            .addVendors(
                MboVendors.GlobalVendor.newBuilder()
                    .setId(vendorId)
                    .addName(MboParameters.Word.newBuilder()
                                 .setLangId(Language.RUSSIAN.getId())
                                 .setName("Vendor 101")
                                 .setMorphologicalProcessing(false)
                    )
                    .setSite("example.org")
                    .setPublished(true)
                    .setComment("comment")
                    .setPictureUrl("image.example.org/image.png")
                    .setLogoPosition(MboVendors.Position.RIGHT)
            )
            .build();
        Assert.assertEquals(expectedResponse, actualResponse);
    }
}

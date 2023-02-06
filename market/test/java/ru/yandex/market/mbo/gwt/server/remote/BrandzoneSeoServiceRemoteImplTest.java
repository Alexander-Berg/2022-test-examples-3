package ru.yandex.market.mbo.gwt.server.remote;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.vendor.GlobalVendorDBMock;
import ru.yandex.market.mbo.db.vendor.GlobalVendorLoaderService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorUtilDB;
import ru.yandex.market.mbo.gwt.client.models.brandzone_seo.RowFilter;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ayratgdl
 * @since 18.11.18
 */
public class BrandzoneSeoServiceRemoteImplTest {
    private static final User USER = new User("user", 0, 0);
    private static final int ROWS_COUNT = 3;

    private BrandzoneSeoServiceRemoteImpl brandzoneSeoRemote;

    private GlobalVendor vendor1;
    private GlobalVendor withSeoOrder2Vendor2;
    private GlobalVendor vendor3;
    private GlobalVendor withSeoOrder1Vendor4;

    @Before
    public void setUp() throws Exception {
        GlobalVendorLoaderService globalVendorLoaderService
                = new GlobalVendorLoaderService();
        GlobalVendorService vendorService = new GlobalVendorService();
        GlobalVendorDBMock vendorDBMock = new GlobalVendorDBMock();
        globalVendorLoaderService.setVendorDb(vendorDBMock);
        globalVendorLoaderService.setVendorDBUtil(Mockito.mock(GlobalVendorUtilDB.class));
        vendorService.setVendorDb(vendorDBMock);
        vendorService.setVendorDBUtil(Mockito.mock(GlobalVendorUtilDB.class));
        vendorService.setGlobalVendorLoaderService(globalVendorLoaderService);

        brandzoneSeoRemote = Mockito.spy(new BrandzoneSeoServiceRemoteImpl());
        brandzoneSeoRemote.setVendorService(vendorService);

        Mockito.doReturn(USER).when(brandzoneSeoRemote).getCurrentUser();

        vendor1 = buildTestVendor("vendor 1", null, null);
        withSeoOrder2Vendor2 = buildTestVendor("seo2 vendor 2", "seo title", null);
        vendor3 = buildTestVendor("vendor 2", null, null);
        withSeoOrder1Vendor4 = buildTestVendor("seo1 vendor 4", null, "seo description");

        vendorDBMock.createVendor(vendor1, USER.getUid());
        vendorDBMock.createVendor(withSeoOrder2Vendor2, USER.getUid());
        vendorDBMock.createVendor(vendor3, USER.getUid());
        vendorDBMock.createVendor(withSeoOrder1Vendor4, USER.getUid());
    }

    @Test
    public void loadVendorsWithSeoInfoWithoutFilterOrOffset() {
        List<GlobalVendor> actualVendors =
            brandzoneSeoRemote.loadVendorsWithSeoInfo(0, ROWS_COUNT, new RowFilter(null));
        List<GlobalVendor> expectedVendors = Arrays.asList(withSeoOrder1Vendor4, withSeoOrder2Vendor2);
        Assert.assertEquals(expectedVendors, actualVendors);
    }

    @Test
    public void loadVendorsWithSeoInfoWithOffset() {
        List<GlobalVendor> actualVendors =
            brandzoneSeoRemote.loadVendorsWithSeoInfo(1, ROWS_COUNT, new RowFilter(null));
        List<GlobalVendor> expectedVendors = Arrays.asList(withSeoOrder2Vendor2);
        Assert.assertEquals(expectedVendors, actualVendors);
    }

    @Test
    public void loadVendorsWithSeoInfoWithFilterByVendor() {
        RowFilter filter = new RowFilter(withSeoOrder1Vendor4.getId());
        List<GlobalVendor> actualVendors = brandzoneSeoRemote.loadVendorsWithSeoInfo(0, ROWS_COUNT, filter);
        List<GlobalVendor> expectedVendors = Arrays.asList(withSeoOrder1Vendor4);
        Assert.assertEquals(expectedVendors, actualVendors);
    }

    @Test
    public void getVendorsCountWithSeoInfoWithoutFilter() {
        int actualCount = brandzoneSeoRemote.getVendorsCountWithSeoInfo(new RowFilter(null));
        Assert.assertEquals(2, actualCount);
    }

    @Test
    public void getVendorsCountWithSeoInfoWithFilterByVendor() {
        RowFilter filter = new RowFilter(withSeoOrder1Vendor4.getId());
        int actualCount = brandzoneSeoRemote.getVendorsCountWithSeoInfo(filter);
        Assert.assertEquals(1, actualCount);
    }

    @Test
    public void removeSeoInfoFromVendor() {
        brandzoneSeoRemote.removeSeoInfoFromVendor(withSeoOrder1Vendor4.getId());
        List<GlobalVendor> remainingVendors =
            brandzoneSeoRemote.loadVendorsWithSeoInfo(0, ROWS_COUNT, new RowFilter(null));
        Assert.assertEquals(Arrays.asList(withSeoOrder2Vendor2), remainingVendors);
    }

    @Test
    public void saveSeoInfoForVendor() {
        brandzoneSeoRemote.saveSeoInfoForVendor(withSeoOrder1Vendor4.getId(), "seo title", "seo description");
        GlobalVendor actualUpdatedVendor =
            brandzoneSeoRemote.loadVendorsWithSeoInfo(0, 1, new RowFilter(withSeoOrder1Vendor4.getId())).get(0);

        GlobalVendor expectedUpdatedVendor = withSeoOrder1Vendor4.copy();
        expectedUpdatedVendor.setSeoTitle("seo title");
        expectedUpdatedVendor.setSeoDescription("seo description");

        Assert.assertEquals(expectedUpdatedVendor, actualUpdatedVendor);
    }

    private static GlobalVendor buildTestVendor(String name, String seoTitle, String seoDescription) {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setNames(Collections.singletonList(new Word(Language.RUSSIAN.getId(), name)));
        vendor.setSeoTitle(seoTitle);
        vendor.setSeoDescription(seoDescription);
        return vendor;
    }
}

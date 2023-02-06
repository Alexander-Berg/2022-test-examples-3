package ru.yandex.market.mbo.gwt.server.remote;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.vendor.GlobalVendorDBMock;
import ru.yandex.market.mbo.db.vendor.GlobalVendorLoaderService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorUtilDB;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author ayratgdl
 * @date 30.03.18
 */
public class GlobalVendorServiceImplTest {
    private static final Integer FOUNDATION_YEAR = 1986;

    private GlobalVendorServiceImpl vendorHandlerImpl;

    @Before
    public void setUp() {
        GlobalVendorLoaderService globalVendorLoaderService
                = new GlobalVendorLoaderService();
        GlobalVendorService vendorService = new GlobalVendorService();
        globalVendorLoaderService.setVendorDb(new GlobalVendorDBMock());
        globalVendorLoaderService.setVendorDBUtil(Mockito.mock(GlobalVendorUtilDB.class));
        vendorService.setVendorDb(new GlobalVendorDBMock());
        vendorService.setVendorDBUtil(Mockito.mock(GlobalVendorUtilDB.class));
        vendorService.setGlobalVendorLoaderService(globalVendorLoaderService);


        AccessControlManager accessManager = Mockito.mock(AccessControlManager.class);
        Mockito.when(accessManager.getCachedUser()).thenReturn(new User());

        vendorHandlerImpl = new GlobalVendorServiceImpl();
        vendorHandlerImpl.setVendorService(vendorService);
        ReflectionTestUtils.setField(vendorHandlerImpl, "accessControlManager", accessManager);
    }

    @Test
    public void getGlobalVendorsEmptyList() {
        List<GlobalVendor> vendors = vendorHandlerImpl.getGlobalVendors(Collections.emptyList());
        Assert.assertEquals(Collections.emptyList(), vendors);
    }

    @Test
    public void saveVendor() {
        GlobalVendor vendor = buildVendor();

        long vendorId = vendorHandlerImpl.createVendor(vendor);
        GlobalVendor createdVendor = vendorHandlerImpl.loadVendor(vendorId);
        Assert.assertEquals(vendor, createdVendor);
    }

    @Test
    public void saveVendorWithModernSite() {
        GlobalVendor vendor = buildVendor();
        vendor.setSite("http://something.games");

        long vendorId = vendorHandlerImpl.createVendor(vendor);
        GlobalVendor createdVendor = vendorHandlerImpl.loadVendor(vendorId);
        Assert.assertEquals(vendor, createdVendor);
    }

    @Test
    public void saveVendorWithWrongSite() {
        GlobalVendor vendor = buildVendor();
        vendor.setSite("http://something.sdfdsfdsfsdf");

        assertThatThrownBy(() -> vendorHandlerImpl.createVendor(vendor))
            .isInstanceOf(OperationException.class);
    }

    @Test
    public void updateFoundationYear() {
        GlobalVendor vendor = buildVendor();
        vendor.setFoundationYear(null);

        vendorHandlerImpl.createVendor(vendor);
        vendor.setFoundationYear(FOUNDATION_YEAR);
        vendorHandlerImpl.updateVendor(vendor);

        GlobalVendor updatedVendor = vendorHandlerImpl.loadVendor(vendor.getId());
        Assert.assertEquals(vendor, updatedVendor);
    }

    private static GlobalVendor buildVendor() {
        GlobalVendor vendor = new GlobalVendor();
        vendor.addName(Language.RUSSIAN.getId(), "BigCompany");
        vendor.setPublished(true);
        vendor.setSite("company.example.com");
        vendor.setFoundationYear(FOUNDATION_YEAR);
        return vendor;
    }
}

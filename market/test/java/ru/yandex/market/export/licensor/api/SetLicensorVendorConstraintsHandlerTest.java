package ru.yandex.market.export.licensor.api;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import ru.yandex.market.http.BadRequestException;
import ru.yandex.market.mbo.licensor.MboLicensors.SetVendorConstraintsRequest;
import ru.yandex.market.mbo.licensor.MboLicensors.VendorConstraint;
import ru.yandex.market.mbo.licensor.MboLicensors.VendorConstraintsResponse;
import ru.yandex.market.mbo.licensor2.LicensorServiceImpl;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCaseDAOMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorExtraDAOMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorScheme;
import ru.yandex.market.mbo.licensor2.scheme.LicensorSchemeService;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint.Source;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraintDAOMock;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfDeletedUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfExtrasUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfRestoreUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorVendorLinkUpdater;

/**
 * @author ayratgdl
 * @date 21.05.18
 */
public class SetLicensorVendorConstraintsHandlerTest {
    private static final long LICENSOR_1 = 101;
    private static final long VENDOR_1 = 201;
    private static final long VENDOR_2 = 202;
    private static final long CATEGORY_1 = 301;
    private static final long CATEGORY_2 = 302;
    private static final long USER_ID_1 = 401;
    private static final long VENDOR_OFFICE_USER_ID = 402;

    private SetLicensorVendorConstraintsHandler handler;
    private LicensorServiceImpl licensorService;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        LicensorSchemeService schemeService = new LicensorSchemeService();
        schemeService.setLicensorCaseDAO(new LicensorCaseDAOMock());
        schemeService.setExtraLfpDAO(new LicensorExtraDAOMock());
        schemeService.setLVConstraintDAO(new LicensorVendorConstraintDAOMock());

        licensorService = new LicensorServiceImpl();
        licensorService.setSchemeService(schemeService);
        licensorService.setRuleOfRestoreUpdater(Mockito.mock(LicensorRuleOfRestoreUpdater.class));
        licensorService.setRuleOfDeletedUpdater(Mockito.mock(LicensorRuleOfDeletedUpdater.class));
        licensorService.setRuleOfExtrasUpdater(Mockito.mock(LicensorRuleOfExtrasUpdater.class));
        licensorService.setVendorLinkUpdater(Mockito.mock(LicensorVendorLinkUpdater.class));

        handler = new SetLicensorVendorConstraintsHandler(licensorService, VENDOR_OFFICE_USER_ID);
    }

    @Test
    public void handlePostRequestWhereRequestNotHaveLicensorId() {
        exception.expect(BadRequestException.class);
        exception.expectMessage(Matchers.equalTo("Request doesn't contain 'licensorId'"));
        SetVendorConstraintsRequest request = SetVendorConstraintsRequest.newBuilder()
            .setUserId(USER_ID_1)
            .build();
        handler.handle(request);
    }

    @Test
    public void handlePostRequestWhereRequestNotHaveUserId() {
        exception.expect(BadRequestException.class);
        exception.expectMessage(Matchers.equalTo("Request doesn't contain 'userId'"));
        SetVendorConstraintsRequest request = SetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .build();
        handler.handle(request);
    }

    @Test
    public void handlePostRequestWhereRequestNotHaveVendorId() {
        exception.expect(BadRequestException.class);
        exception.expectMessage(Matchers.equalTo("Request contains VendorConstraint without 'vendorId'"));
        SetVendorConstraintsRequest request = SetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .setUserId(USER_ID_1)
            .addVendorConstraint(
                VendorConstraint.newBuilder().setCategoryId(CATEGORY_1)
            )
            .build();
        handler.handle(request);
    }

    @Test
    public void handlePostRequestWhereRequestNotHaveCategoryId() {
        exception.expect(BadRequestException.class);
        exception.expectMessage(Matchers.equalTo("Request contains VendorConstraint without 'categoryId'"));
        SetVendorConstraintsRequest request = SetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .setUserId(USER_ID_1)
            .addVendorConstraint(
                VendorConstraint.newBuilder().setVendorId(VENDOR_1)
            )
            .build();
        handler.handle(request);
    }

    @Test
    public void handleRequestWithSeveralVendorConstrains() {
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR_1, VENDOR_1, CATEGORY_1, USER_ID_1, Source.UNKNOWN_THROUGH_HTTP_API)
        );
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR_1, VENDOR_2, CATEGORY_2, USER_ID_1, Source.UNKNOWN_THROUGH_HTTP_API)
        );
        SetVendorConstraintsRequest request = SetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .setUserId(USER_ID_1)
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_2))
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_1))
            .build();
        VendorConstraintsResponse response = handler.handle(request);

        VendorConstraintsResponse expectedResponse = VendorConstraintsResponse.newBuilder()
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_1))
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_2))
            .build();
        Assert.assertEquals(expectedResponse, response);

        LicensorScheme expectedScheme = new LicensorScheme()
            .addVendorConstraint(new LicensorVendorConstraint(LICENSOR_1, VENDOR_1, CATEGORY_2,
                                                              USER_ID_1, Source.UNKNOWN_THROUGH_HTTP_API))
            .addVendorConstraint(new LicensorVendorConstraint(LICENSOR_1, VENDOR_1, CATEGORY_1,
                                                              USER_ID_1, Source.UNKNOWN_THROUGH_HTTP_API));
        Assert.assertEquals(expectedScheme, licensorService.getLicensorScheme());
    }

    @Test
    public void handleRequestWithSeveralVendorConstrainsWithRewriteConstrainsFromMboUI() {
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR_1, VENDOR_1, CATEGORY_1, USER_ID_1, Source.MBO_UI)
        );
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR_1, VENDOR_2, CATEGORY_2, USER_ID_1, Source.MBO_UI)
        );
        SetVendorConstraintsRequest request = SetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .setUserId(USER_ID_1)
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_2))
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_1))
            .build();
        VendorConstraintsResponse response = handler.handle(request);

        VendorConstraintsResponse expectedResponse = VendorConstraintsResponse.newBuilder()
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_1))
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_2))
            .build();
        Assert.assertEquals(expectedResponse, response);

        LicensorScheme expectedScheme = new LicensorScheme()
            .addVendorConstraint(new LicensorVendorConstraint(LICENSOR_1, VENDOR_1, CATEGORY_2,
                                                              USER_ID_1, Source.UNKNOWN_THROUGH_HTTP_API))
            .addVendorConstraint(new LicensorVendorConstraint(LICENSOR_1, VENDOR_1, CATEGORY_1,
                                                              USER_ID_1, Source.UNKNOWN_THROUGH_HTTP_API));
        Assert.assertEquals(expectedScheme, licensorService.getLicensorScheme());
    }

    @Test
    public void handleRequestFromVendorOffice() {
        SetVendorConstraintsRequest request = SetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .setUserId(VENDOR_OFFICE_USER_ID)
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_1))
            .build();
        VendorConstraintsResponse response = handler.handle(request);

        VendorConstraintsResponse expectedResponse = VendorConstraintsResponse.newBuilder()
            .addVendorConstraint(VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_1))
            .build();
        Assert.assertEquals(expectedResponse, response);

        LicensorScheme expectedScheme = new LicensorScheme()
            .addVendorConstraint(new LicensorVendorConstraint(LICENSOR_1, VENDOR_1, CATEGORY_1,
                                                              VENDOR_OFFICE_USER_ID, Source.VENDOR_OFFICE));
        Assert.assertEquals(expectedScheme, licensorService.getLicensorScheme());
    }
}

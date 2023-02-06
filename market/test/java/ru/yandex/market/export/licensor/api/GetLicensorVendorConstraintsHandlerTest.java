package ru.yandex.market.export.licensor.api;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import ru.yandex.market.http.BadRequestException;
import ru.yandex.market.mbo.licensor.MboLicensors.GetVendorConstraintsRequest;
import ru.yandex.market.mbo.licensor.MboLicensors.VendorConstraint;
import ru.yandex.market.mbo.licensor.MboLicensors.VendorConstraintsResponse;
import ru.yandex.market.mbo.licensor2.LicensorServiceImpl;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCaseDAOMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorExtraDAOMock;
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
public class GetLicensorVendorConstraintsHandlerTest {
    private static final long LICENSOR_1 = 101;
    private static final long VENDOR_1 = 201;
    private static final long VENDOR_2 = 202;
    private static final long CATEGORY_1 = 301;
    private static final long CATEGORY_2 = 302;
    private static final long USER_ID_1 = 401;
    private static final long VENDOR_OFFICE_USER_ID = 402;

    private GetLicensorVendorConstraintsHandler handler;
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

        handler = new GetLicensorVendorConstraintsHandler(licensorService, VENDOR_OFFICE_USER_ID);
    }

    @Test
    public void handleGetRequestWithoutParameterLicensorId() {
        exception.expect(BadRequestException.class);
        exception.expectMessage(Matchers.equalTo("Request doesn't contain 'licensorId'"));
        GetVendorConstraintsRequest request = GetVendorConstraintsRequest.newBuilder().setUserId(USER_ID_1).build();
        handler.handle(request);
    }

    @Test
    public void handleGetRequestWithoutParameterUserId() {
        exception.expect(BadRequestException.class);
        exception.expectMessage(Matchers.equalTo("Request doesn't contain 'userId'"));
        GetVendorConstraintsRequest request = GetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .build();
        handler.handle(request);
    }

    @Test
    public void handleGetRequestWhereLicensorIsNotExist() {
        GetVendorConstraintsRequest request = GetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .setUserId(USER_ID_1)
            .build();
        VendorConstraintsResponse response = handler.handle(request);
        VendorConstraintsResponse expectedResponse = VendorConstraintsResponse.getDefaultInstance();
        Assert.assertEquals(expectedResponse, response);
    }

    @Test
    public void handleGetRequestWhereLicensorConstrainsVendors() {
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR_1, VENDOR_1, CATEGORY_1, USER_ID_1, Source.UNKNOWN_THROUGH_HTTP_API)
        );
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR_1, VENDOR_2, CATEGORY_2, USER_ID_1, Source.UNKNOWN_THROUGH_HTTP_API)
        );
        GetVendorConstraintsRequest request = GetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .setUserId(USER_ID_1)
            .build();

        VendorConstraintsResponse response = handler.handle(request);

        VendorConstraintsResponse expectedResponse = VendorConstraintsResponse.newBuilder()
            .addVendorConstraint(
                VendorConstraint.newBuilder().setVendorId(VENDOR_1).setCategoryId(CATEGORY_1)
            )
            .addVendorConstraint(
                VendorConstraint.newBuilder().setVendorId(VENDOR_2).setCategoryId(CATEGORY_2)
            )
            .build();
        Assert.assertEquals(expectedResponse, response);
    }

    @Test(expected = Exception.class)
    public void handleGetRequestWhereThrowUnexpectedException() {
        handler = new GetLicensorVendorConstraintsHandler(null, null);
        GetVendorConstraintsRequest request = GetVendorConstraintsRequest.newBuilder()
            .setLicensorId(LICENSOR_1)
            .setUserId(USER_ID_1)
            .build();
        handler.handle(request);
    }
}

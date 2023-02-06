package ru.yandex.market.api.internal.market.vendor.parsers;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import ru.yandex.market.api.common.ApiException;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.common.error.ExceptionContainer;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.market.vendor.domain.ExternalVendorApiError;
import ru.yandex.market.api.internal.market.vendor.domain.VendorUserPermission;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class GetPermissionsByVendorParserTest extends UnitTestBase {

    @Test
    public void shouldParsePermissionsResponse() throws Exception {
        Result<Long2ObjectMap<EnumSet<VendorUserPermission>>, ExceptionContainer> result = parse("get-permissions-by-vendor.json");

        assertTrue(result.isOk());
        assertEquals(
            EnumSet.of(VendorUserPermission.MODEL_WRITE, VendorUserPermission.CHARACTERISTICS_READ),
            result.getValue().get(1)
        );
        assertEquals(
            EnumSet.of(VendorUserPermission.CHARACTERISTICS_READ),
            result.getValue().get(2)
        );
    }

    @Test
    public void shouldParseErrors() throws Exception {
        Result<Long2ObjectMap<EnumSet<VendorUserPermission>>, ExceptionContainer> result= parse("get-permissions-by-vendor-error.json");
        assertFalse(result.isOk());

        List<ApiException> exceptions = result.getError().getExceptions();
        assertEquals(1, exceptions.size());

        ExternalVendorApiError vendorApiError = (ExternalVendorApiError) exceptions.get(0);
        assertEquals(
            "market_vendor-api error: Code: MISSING_REQUEST_PARAMETER, message: Required Long parameter 'uid' " +
                "is not present, HTTP status: 400, error string: 0EC3DCGRAVICAPA01HT",
            vendorApiError.getMessage()
        );
        assertEquals("Required Long parameter 'uid' is not present", vendorApiError.getErrorMessage());
        assertEquals("MISSING_REQUEST_PARAMETER", vendorApiError.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, vendorApiError.getHttpStatus());
        assertEquals("0EC3DCGRAVICAPA01HT", vendorApiError.getErrorString());
    }

    private static Result<Long2ObjectMap<EnumSet<VendorUserPermission>>, ExceptionContainer> parse(String file) {
        return getParser().parse(ResourceHelpers.getResource(file));
    }

    private static GetPermissionsByVendorParser getParser() {
        return new GetPermissionsByVendorParser();
    }
}

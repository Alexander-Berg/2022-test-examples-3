package ru.yandex.market.api.internal.market.vendor.parsers;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import ru.yandex.market.api.internal.market.vendor.domain.ExternalVendorApiError;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class ErrorParserTest {

    @Test
    public void shouldParse() throws Exception {
        ExternalVendorApiError error = new ErrorParser().parse(ResourceHelpers.getResource("error.json"));

        assertEquals("MISSING_REQUEST_PARAMETER", error.getErrorCode());
        assertEquals("Required Long parameter 'uid' is not present", error.getErrorMessage());
        assertEquals(HttpStatus.BAD_REQUEST, error.getHttpStatus());
        assertEquals("0EC3DCGRAVICAPA01HT", error.getErrorString());
    }
}

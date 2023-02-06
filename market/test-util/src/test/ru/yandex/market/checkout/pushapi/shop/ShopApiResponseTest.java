package ru.yandex.market.checkout.pushapi.shop;

import org.apache.http.conn.ConnectTimeoutException;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;

public class ShopApiResponseTest {

    @Test
    public void testHttpError() throws Exception {
        final ShopApiResponse response = r(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertEquals(ErrorSubCode.HTTP, response.getErrorSubCode());
    }

    @Test
    public void testConnectionTimedOutError() throws Exception {
        final ShopApiResponse response = r(
            new ResourceAccessException(anyString(), new ConnectTimeoutException())
        );
        assertEquals(ErrorSubCode.CONNECTION_TIMED_OUT, response.getErrorSubCode());
    }

    @Test
    public void testConnectionRefusedError() throws Exception {
        final ShopApiResponse response = r(
            new ResourceAccessException(anyString(), new ConnectException())
        );
        assertEquals(ErrorSubCode.CONNECTION_REFUSED, response.getErrorSubCode());
    }

    @Test
    public void testReadTimedOutError() throws Exception {
        final ShopApiResponse response = r(
            new ResourceAccessException(anyString(), new SocketTimeoutException())
        );
        assertEquals(ErrorSubCode.READ_TIMED_OUT, response.getErrorSubCode());
    }

    @Test
    public void testInvalidDataIfUnknownResourceAccessExceptionCause() throws Exception {
        final ShopApiResponse response = r(new ResourceAccessException(anyString(), new IOException()));
        assertEquals(ErrorSubCode.INVALID_DATA, response.getErrorSubCode());
    }

    @Test
    public void testCantParseResponseError() throws Exception {
        final ShopApiResponse response = r(new HttpMessageConversionException(anyString()));
        assertEquals(ErrorSubCode.CANT_PARSE_RESPONSE, response.getErrorSubCode());
    }

    @Test
    public void testInvalidDataError() throws Exception {
        final ShopApiResponse response = r(new ValidationException(anyString()));
        assertEquals(ErrorSubCode.INVALID_DATA, response.getErrorSubCode());
    }

    @Test
    public void testUnkownErrorOnOtherExceptions() throws Exception {
        final ShopApiResponse response = r(new Exception());
        assertEquals(ErrorSubCode.UNKNOWN, response.getErrorSubCode());
    }

    private ShopApiResponse r(Exception e) {
        return ShopApiResponse.fromException(e);
    }
}

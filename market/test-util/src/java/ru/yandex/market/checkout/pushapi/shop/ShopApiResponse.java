package ru.yandex.market.checkout.pushapi.shop;

import org.apache.http.conn.ConnectTimeoutException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;

public class ShopApiResponse<T> {
    private T body;
    private String httpMethod;
    private String host;
    private String url;
    private String args;
    private Long uid;
    private String requestHeaders;
    private String requestBody;
    private String responseHeaders;
    private String responseBody;
    private long responseTime;
    private Exception exception;

    @Deprecated
    public ShopApiResponse(T body, String requestBody, String responseBody) {
        this.body = body;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
    }

    @Deprecated
    public ShopApiResponse(String requestBody, String responseBody, Exception exception) {
        this.requestBody = requestBody;
        this.responseBody = responseBody;
        this.exception = exception;
    }

    private ShopApiResponse() {
    }

    public static <T> ShopApiResponse<T> copyAndSetError(Exception exception, ShopApiResponse<T> response) {
        final ShopApiResponse<T> tResponse = fromException(exception);
        copyFields(response, tResponse);
        return tResponse;
    }

    public static <T> ShopApiResponse<T> copyAndSetBody(T body, ShopApiResponse<T> response) {
        final ShopApiResponse<T> tResponse = fromBody(body);
        copyFields(response, tResponse);
        return tResponse;
    }

    private static <T> void copyFields(ShopApiResponse<T> from, ShopApiResponse<T> to) {
        to.httpMethod = from.httpMethod;
        to.host = from.host;
        to.url = from.url;
        to.args = from.args;
        to.uid = from.uid;
        to.requestHeaders = from.requestHeaders;
        to.requestBody = from.requestBody;
        to.responseHeaders = from.responseHeaders;
        to.responseBody = from.responseBody;
        to.responseTime = from.responseTime;
    }

    public static <T> ShopApiResponse<T> fromException(Exception exception) {
        final ShopApiResponse<T> tResponse = new ShopApiResponse<>();
        tResponse.exception = exception;
        return tResponse;
    }

    public static <T> ShopApiResponse<T> fromBody(T body) {
        final ShopApiResponse<T> tResponse = new ShopApiResponse<>();
        tResponse.body = body;
        return tResponse;
    }

    public ShopApiResponse<T> populateBodies(HttpBodies httpBodies) {
        requestHeaders = new String(httpBodies.getRequestHeaders().toByteArray());
        requestBody = new String(httpBodies.getRequestBody().toByteArray());
        responseHeaders = new String(httpBodies.getResponseHeaders().toByteArray());
        responseBody = new String(httpBodies.getResponseBody().toByteArray());
        return this;
    }

    public boolean isError() {
        return exception != null;
    }

    public T getBody() {
        return body;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Exception getException() {
        return exception;
    }

    public String getUrl() {
        return url;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public ShopApiResponse<T> setUrl(String url) {
        this.url = url;
        return this;
    }

    public ShopApiResponse<T> setResponseTime(long responseTime) {
        this.responseTime = responseTime;
        return this;
    }

    public String getHost() {
        return host;
    }

    public ShopApiResponse<T> setHost(String host) {
        this.host = host;
        return this;
    }

    public String getArgs() {
        return args;
    }

    public ShopApiResponse<T> setArgs(String args) {
        this.args = args;
        return this;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public ShopApiResponse<T> setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public Long getUid() {
        return uid;
    }

    public ShopApiResponse<T> setUid(Long uid) {
        this.uid = uid;
        return this;
    }

    public ErrorSubCode getErrorSubCode() {
        if(exception instanceof HttpStatusCodeException) {
            return ErrorSubCode.HTTP;
        } else if(exception instanceof ShopErrorException) {
            return ((ShopErrorException) exception).getCode();
        } else if(exception instanceof ResourceAccessException) {
            final Class<? extends Throwable> cause = exception.getCause().getClass();
            if(cause == ConnectTimeoutException.class) {
                return ErrorSubCode.CONNECTION_TIMED_OUT;
            } else if(cause == ConnectException.class) {
                return ErrorSubCode.CONNECTION_REFUSED;
            } else if(cause == SocketTimeoutException.class) {
                return ErrorSubCode.READ_TIMED_OUT;
            } else if(cause == SSLException.class) {
                return ErrorSubCode.SSL_ERROR;
            } else if(cause == CertificateException.class) {
                return ErrorSubCode.SSL_ERROR;
            } else {
                return ErrorSubCode.UNKNOWN;
            }
        } else if(exception instanceof HttpMessageConversionException) {
            if(exception.getCause().getClass() == HttpMediaTypeNotSupportedException.class) {
                return ErrorSubCode.UNSUPPORTED_MEDIA_TYPE;
            } else {
                return ErrorSubCode.CANT_PARSE_RESPONSE;
            }
        } else if(exception instanceof ValidationException) {
            return ErrorSubCode.INVALID_DATA;
        } else if(exception instanceof IllegalArgumentException) {
            return ErrorSubCode.INVALID_DATA;
        } else if(exception instanceof InvalidRequestException) {
            return ErrorSubCode.INVALID_DATA;
        } else {
            return ErrorSubCode.UNKNOWN;
        }
    }
}

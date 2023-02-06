package ru.yandex.market.checkout.pushapi.shop;

import java.io.ByteArrayOutputStream;

public class HttpBodies {

    private ByteArrayOutputStream requestHeaders = new ByteArrayOutputStream();
    private ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
    private ByteArrayOutputStream responseHeaders = new ByteArrayOutputStream();
    private ByteArrayOutputStream responseBody = new ByteArrayOutputStream();

    public ByteArrayOutputStream getRequestHeaders() {
        return requestHeaders;
    }

    public ByteArrayOutputStream getRequestBody() {
        return requestBody;
    }

    public ByteArrayOutputStream getResponseHeaders() {
        return responseHeaders;
    }

    public ByteArrayOutputStream getResponseBody() {
        return responseBody;
    }
}

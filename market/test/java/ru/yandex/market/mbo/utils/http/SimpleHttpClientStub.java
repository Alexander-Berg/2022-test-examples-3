package ru.yandex.market.mbo.utils.http;

/**
 * @author ayratgdl
 * @since 12/12/2018
 */
public class SimpleHttpClientStub implements SimpleHttpClient {
    private String responseOnGet;

    @Override
    public String get(String url) {
        if (responseOnGet != null) {
            return responseOnGet;
        } else {
            throw new RuntimeException("Absent response");
        }
    }

    public SimpleHttpClientStub setResponseOnGet(String response) {
        responseOnGet = response;
        return this;
    }
}

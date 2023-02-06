package ru.yandex.market.core.util.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import ru.yandex.market.mbi.http.MarketHttpClient;

/**
 * @author sergey-fed
 */
public class UnitTestMarketHttpClient implements MarketHttpClient {

    private String responseText = "";

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    @Override
    public int getForStatusCode(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T get(String url, Function<InputStream, T> transformer) throws Exception {
        return transformer.apply(new ByteArrayInputStream(responseText.getBytes(Charset.defaultCharset())));
    }
}

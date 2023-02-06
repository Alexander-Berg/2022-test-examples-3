package ru.yandex.market.tsum.clients.huawei.http;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

public class FakeResponseBuilder {

    private static final String REASON_PHRASE = "";

    private int status;

    private String content;

    public static FakeResponseBuilder create() {
        return new FakeResponseBuilder();
    }

    public FakeResponseBuilder status(int status) {
        this.status = status;
        return this;
    }

    public FakeResponseBuilder emptyJsonContent() {
        this.content = "{}";
        return this;
    }

    public FakeResponseBuilder content(String content) {
        this.content = content;
        return this;
    }

    public HttpResponse build() {
        try {
            BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, status,
                REASON_PHRASE));
            response.setEntity(new StringEntity(content));
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

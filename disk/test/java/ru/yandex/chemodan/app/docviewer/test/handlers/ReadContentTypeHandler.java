package ru.yandex.chemodan.app.docviewer.test.handlers;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

/**
 * @author nshmakov
 */
public class ReadContentTypeHandler implements ResponseHandler<String> {

    @Override
    public String handleResponse(HttpResponse response) {
        return response.getFirstHeader("Content-Type").getValue();
    }
}

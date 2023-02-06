package ru.yandex.autotests.direct.httpclient.core;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.BufferedHttpEntity;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;

import java.io.IOException;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class DirectResponseHandler implements ResponseHandler<DirectResponse> {
    @Override
    public DirectResponse handleResponse(HttpResponse response) throws IOException {
        StatusLine statusLine = response.getStatusLine();
        if(statusLine.getStatusCode() > 400) {
            throw new BackEndClientException("Server returned error " +
                    statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
        }
        HttpEntity entity = new BufferedHttpEntity(response.getEntity());

        return new DirectResponse(statusLine, entity, response.getAllHeaders());
    }
}

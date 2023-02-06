package ru.yandex.autotests.direct.httpclient.steps.base;

import org.apache.commons.httpclient.HttpClientError;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.Asserts;
import ru.yandex.autotests.direct.httpclient.core.DirectRequestBuilder;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.steps.HttpLogSteps;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.autotests.httpclient.lite.core.steps.BackEndBaseSteps;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static ru.yandex.autotests.direct.httpclient.core.DirectRequestBuilder.setGetVarsInNameValuePairs;
import static ru.yandex.autotests.httpclient.lite.utils.HttpUtils.parseUrlParameters;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public abstract class DirectBackEndSteps extends BackEndBaseSteps {

    public HttpLogSteps onHttpLogSteps() {
        return getInstance(HttpLogSteps.class, config);
    }

    protected DirectRequestBuilder getRequestBuilder() {
        return (DirectRequestBuilder) config.getRequestBuilder();
    }

    @Step("Открываем редирект")
    public DirectResponse openRedirect(DirectResponse responseWithRedirect) {
        DirectRequestBuilder rb = new DirectRequestBuilder(config.getRequestBuilder().getConfig(),
                getRequestBuilder().isReturnVariableDump());
        Header location = responseWithRedirect.getHeader(HttpHeaders.LOCATION);
        Asserts.notNull(location, "Redirect location");
        rb.uri(location.getValue());
        List<NameValuePair> parameters = parseUrlParameters(location.getValue());
        if (getRequestBuilder().isReturnVariableDump()) {
            setGetVarsInNameValuePairs(parameters);
        }
        return execute(rb.get(parameters));
    }

    public DirectResponse execute(HttpUriRequest method) {
        try {
            DirectResponse response = (DirectResponse) config.getHttpClient().execute(method, config.getHandler());
            onHttpLogSteps().logContext(method, response);
            DirectResponse.setLastToken(response.getCSRFToken());
            return response;
        } catch (IOException e) {
            throw new BackEndClientException("Execution failed", e);
        }
    }

    public DirectResponse execute(String url) {
        HttpGet httpget;
        try {
            httpget = new HttpGet(new URIBuilder(url).build());
        } catch (URISyntaxException e) {
            throw new HttpClientError("Ошибка преобразования строки '" + url + "' в URI");
        }
        return execute(httpget);
    }

    public String getFilePath(final String fileName) {
        if (getResourceFromClasspath(fileName) != null) {
            return getPathForResource(fileName);
        }
        return getPathForSystemFile(fileName);
    }

    private String getPathForResource(final String fileName) {
        return getResourceFromClasspath(fileName).getPath();
    }

    public static URL getResourceFromClasspath(final String fileName) {
        return Thread.currentThread().getContextClassLoader().getResource(fileName);
    }

    private String getPathForSystemFile(final String fileName) {
        File file = new File(fileName);
        return file.getPath();
    }


}

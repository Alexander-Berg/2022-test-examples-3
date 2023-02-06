package ru.yandex.common.framework.http;

import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

import ru.yandex.common.util.http.HttpClientUtils;

import static ru.yandex.common.util.IOUtils.readInputStream;
import static ru.yandex.common.util.collections.CollectionFactory.newArrayList;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public abstract class AbstractHttpTestCase extends AbstractDependencyInjectionSpringContextTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerTest.class);
    private static final int TIMEOUT = 10000;

    private HttpClient httpClient;
    private HttpServerInitializer httpServerInitializer;

    public void setHttpServerInitializer(final HttpServerInitializer httpServerInitializer) {
        this.httpServerInitializer = httpServerInitializer;
    }

    protected final String[] getConfigLocations() {
        String[] localConfigs = getLocalConfigLocations();
        List<String> configs = newArrayList(new String[]{
                "classpath:http/test-http-server-config.xml"
        });
        configs.addAll(Arrays.asList(localConfigs));
        return configs.toArray(new String[1]);
    }

    protected abstract String[] getLocalConfigLocations();

    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        if (isLocal()) {
            httpServerInitializer.init();
        }
        httpClient = HttpClientUtils.createHttpClient(TIMEOUT, TIMEOUT);
    }

    @Override
    protected void onTearDown() throws Exception {
        super.onTearDown();
        if (isLocal()) {
            Thread t = new Thread(() -> {
                try {
                    httpServerInitializer.stop();
                } catch (Exception e) {
                    LOGGER.error("ignored", e); // ignored
                }
            });
            t.setDaemon(false);
            t.start();
        }
    }

    final String loadPage(final String actionName,
                          final String cookieString,
                          final NameValuePair... params) throws Exception {
        final HttpPost method = buildMethod(actionName, params);
        try {
            if (cookieString != null) {
                method.addHeader("Cookie", cookieString);
            }

            final HttpResponse response = httpClient.execute(method);

            final StatusLine statusLine = response.getStatusLine();
            final int statusCode = statusLine.getStatusCode();
            assertEquals(HttpStatus.SC_OK, statusCode);

            final HttpEntity entity = response.getEntity();
            return readInputStream(entity.getContent());
        } finally {
            method.releaseConnection();
        }
    }

    final String loadPage(final String actionName, final NameValuePair... params) throws Exception {
        return loadPage(actionName, null, params);
    }

    private String getHost() {
        return "localhost";
    }

    protected boolean isLocal() {
        return true;
    }

    private HttpPost buildMethod(final String actionName, final NameValuePair... params) throws Exception {
        final String query = "http://" + getHost() + ":" + httpServerInitializer.getPort() + "/" +
                actionName +
                "?x=x";

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("query = " + query);
        }
        final HttpPost method = new HttpPost(query);
        method.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf8");
        method.setEntity(new UrlEncodedFormEntity(Arrays.asList(params)));
        return method;
    }

}

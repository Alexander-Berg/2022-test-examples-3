package ru.yandex.chemodan.app.dataapi.web.test;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.test.DataApiTestSupport;
import ru.yandex.chemodan.app.dataapi.web.ActionsContextConfiguration;
import ru.yandex.chemodan.app.dataapi.web.ProxyConfiguration;
import ru.yandex.chemodan.cloud.auth.PlatformSecurityConfiguration;
import ru.yandex.chemodan.zk.configuration.ZkEmbeddedConfiguration;
import ru.yandex.commune.a3.ActionApp;
import ru.yandex.commune.a3.action.http.ActionInvocationServlet;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.net.uri.Uri2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

/**
 * @author Denis Bakharev
 */
@ContextConfiguration(classes = {
        ActionsContextConfiguration.class,
        ProxyConfiguration.class,
        PlatformSecurityConfiguration.class,
        ZkEmbeddedConfiguration.class
})
public abstract class ApiTestBase extends DataApiTestSupport {

    @Autowired
    private ActionApp actionApp;

    private ActionInvocationServlet servlet;
    protected DataApiUserId uid;

    @Before
    public void before() {
        uid = createUser();
        servlet = actionApp.createServlet(getNamespace());
    }

    protected DataApiUserId createUser() {
        return createRandomCleanUser();
    }

    protected MockHttpServletResponse sendRequestUsual(String action, String url, String content) {
        return sendRequest(action, "/api", url, content, Cf.map());
    }

    protected MockHttpServletResponse sendRequestRestLike(String action, String url, String content) {
        return sendRequest(action, "/v1/data", url, content, Cf.map());
    }

    protected MockHttpServletResponse sendRequestUsual(String action, String url, String content,
            MapF<String, String> headers)
    {
        return sendRequest(action, "/api", url, content, headers);
    }

    protected MockHttpServletResponse sendRequestRestLike(String action, String url, String content,
            MapF<String, String> headers)
    {
        return sendRequest(action, "/v1/data", url, content, headers);
    }

    protected MockHttpServletResponse sendRequest(String action, String urlPrefix, String url, String content,
            MapF<String, String> headers)
    {
        Uri2 uri = Uri2.parse("http://localhost" + url);
        String requestStringForMock = uri.onlyPathQueryFragment().toString();

        MockHttpServletRequest
                request = new MockHttpServletRequest(action, requestStringForMock);
        MapF<String, String> parameters = uri.getQueryArgs().toMap();
        request.setParameters(parameters);
        request.setContent(content.getBytes());
        request.setContentType("application/json");
        request.setRequestURI(urlPrefix + url);
        request.setPathInfo(uri.getPath());
        request.setServerName(uri.getHost().get());

        headers.forAllEntries((k, v) -> {
            request.addHeader(k, v);
            return true;
        });

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            servlet.service(request, response);
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }

        return response;
    }

    protected void assertResponseContains(MockHttpServletResponse response, String text) {
        assertResponseContainsOrNot(response, text, true);
    }

    protected void assertResponseNotContains(MockHttpServletResponse response, String text) {
        assertResponseContainsOrNot(response, text, false);
    }

    protected void assertResponseContainsOrNot(MockHttpServletResponse response, String text, boolean positive) {
        String responseContext;
        try {
            responseContext = response.getContentAsString();
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }
        Assert.isTrue(""
                + "string must " + (positive ? "" : "not ")
                + "contain substring; string=" + responseContext + "; substring=" + text,
                responseContext.contains(text.replaceAll(" |\\n", "")) == positive);
    }

    protected void assertContainsRegexp(MockHttpServletResponse response, String regexp) {
        Assert.isTrue(getContent(response).matches(".*" + regexp + ".*"));
    }

    protected String getContent(MockHttpServletResponse response) {
        try {
            return response.getContentAsString();
        } catch (Exception e) {
            throw ExceptionUtils.translate(e);
        }
    }

    protected void assertContains(MockHttpServletResponse response, String string)  {
        Assert.assertContains(getContent(response), string);
    }

    protected void assertContainsInvocationInfo(MockHttpServletResponse response) {
        String content = getContent(response);
        Assert.equals(1, StringUtils.countMatches(content, "\"invocationInfo\""));
        Assert.equals(1, StringUtils.countMatches(content, "\"hostname\""));
        Assert.equals(1, StringUtils.countMatches(content, "\"action\""));
        Assert.equals(1, StringUtils.countMatches(content, "\"app-name\""));
        Assert.equals(1, StringUtils.countMatches(content, "\"app-version\""));
        Assert.equals(1, StringUtils.countMatches(content, "\"exec-duration-millis\""));
    }

    protected abstract String getNamespace() ;
}

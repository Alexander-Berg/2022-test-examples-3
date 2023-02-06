package ru.yandex.chemodan.util.test;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.commune.a3.ActionApp;
import ru.yandex.commune.a3.action.http.ActionInvocationServlet;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.net.uri.Uri2;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

/**
 * @author tolmalev
 */
public class ApiTestBase {
    @Autowired
    private ActionApp actionApp;

    private ActionInvocationServlet servlet;

    @Before
    public void before() {
        servlet = actionApp.createServlet(getNamespace());
    }

    protected String getNamespace() {
        return "";
    }

    protected MockHttpServletResponse sendRequest(String method, String url) {
        return sendRequest(method, url, "", Cf.map());
    }

    protected MockHttpServletResponse sendRequest(String method, String url, String content,
            MapF<String, String> headers)
    {
        Uri2 uri = Uri2.parse("http://localhost" + url);
        String requestStringForMock = uri.onlyPathQueryFragment().toString();

        MockHttpServletRequest
                request = new MockHttpServletRequest(method, requestStringForMock);
        MapF<String, String> parameters = uri.getQueryArgs().toMap();
        request.setParameters(parameters);
        request.setContent(content.getBytes());
        request.setContentType("application/json");
        request.setRequestURI(url);
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
}

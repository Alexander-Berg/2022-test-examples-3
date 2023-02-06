package ru.yandex.common.framework.core.servantletchecker;

import java.util.HashMap;

import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.common.framework.core.DefaultServantInfo;
import ru.yandex.common.framework.core.ErrorRedirectChecker;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.Servantlet;
import ru.yandex.common.framework.core.StubServRequest;
import ru.yandex.common.framework.core.servantletchecker.response.TestableResponse;
import ru.yandex.common.framework.http.HttpServResponse;
import ru.yandex.common.framework.xml.StringXmlBuilder;

/**
 * @author agorbunov @ Oct 25, 2010
 */
public class ServantletClient {

    public TestableResponse getResponse(Servantlet<ServRequest, ServResponse> servantlet,
                                        HashMap<String, String> parameters) {
        ServRequest request = createRequest(parameters);
        ServResponse response = createResponse();
        servantlet.process(request, response);
        return new TestableResponse(response);
    }

    private ServRequest createRequest(HashMap<String, String> parameters) {
        ServRequest request = createEmptyRequest();
        return applyParametersToRequest(parameters, request);
    }

    private ServRequest createEmptyRequest() {
        String name = "empty-request";
        long userId = 0L;
        String redirParamName = "redir";
        return new StubServRequest(userId, redirParamName, name);
    }

    private ServRequest applyParametersToRequest(HashMap<String, String> parameters, ServRequest request) {
        for (String key : parameters.keySet()) {
            request.setParam(key, parameters.get(key));
        }
        return request;
    }

    private ServResponse createResponse() {
        return new HttpServResponse(new MockHttpServletResponse(), createXmlBuilder(), new ErrorRedirectChecker());
    }

    private StringXmlBuilder createXmlBuilder() {
        StringXmlBuilder xmlBuilder = new StringXmlBuilder();
        xmlBuilder.setServantInfo(createServantInfo());
        return xmlBuilder;
    }

    private DefaultServantInfo createServantInfo() {
        DefaultServantInfo servantInfo = new DefaultServantInfo();
        servantInfo.setName("aqua-main");
        return servantInfo;
    }
}

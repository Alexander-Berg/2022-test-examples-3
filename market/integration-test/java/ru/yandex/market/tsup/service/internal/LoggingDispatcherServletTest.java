package ru.yandex.market.tsup.service.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.tpl.common.data_provider.meta.FrontHttpRequestMeta;
import ru.yandex.market.tsup.AbstractContextualTest;

public class LoggingDispatcherServletTest extends AbstractContextualTest {
    @Autowired
    private LoggingDispatcherServlet servlet;

    @BeforeEach
    void init() throws ServletException {
        servlet.init(mockMvc.getDispatcherServlet().getServletConfig());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/user_acivity_log/after_request_dispatch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpServletRequest request = getRequest();

        servlet.service(request, response);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/user_acivity_log/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void excludedPaths() throws Exception {
        MockHttpServletRequest request = getRequest();
        MockHttpServletResponse response;

        response = new MockHttpServletResponse();
        request.setRequestURI("/health/path");
        servlet.doDispatch(request, response);

        response = new MockHttpServletResponse();
        request.setRequestURI("/ping");
        servlet.doDispatch(request, response);

        response = new MockHttpServletResponse();
        request.setRequestURI("/monitoring");
        servlet.doDispatch(request, response);

        response = new MockHttpServletResponse();
        request.setRequestURI("/close");
        servlet.doDispatch(request, response);
    }

    private MockHttpServletRequest getRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setPathInfo("trips");
        request.setRequestURI("trips");
        request.setParameter("startDateFrom", "2021-01-01");
        request.setParameter("isActive", "true");
        request.addHeader(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "aidenne");
        request.addHeader("X-Forwarded-For", "78.178.8.206");
        request.addHeader("X-Real-IP", "90.188.42.86");
        request.setMethod("GET");

        return request;
    }

}

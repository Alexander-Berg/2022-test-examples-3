package ru.yandex.market.logistics.management.serlvet;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.util.ContentCachingRequestWrapper;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.UserActivityLog;
import ru.yandex.market.logistics.management.repository.UserActivityLogRepository;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.LoggableDispatcherServlet;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_RELATION;

@CleanDatabase
class LoggableDispatcherServletTest extends AbstractContextualTest {

    @Autowired
    private MockServletContext servletContext;

    @Autowired
    private LoggableDispatcherServlet servlet;

    @Autowired
    private UserActivityLogRepository logRepository;

    @BeforeEach
    void setup() throws Exception {
        MockServletConfig servletConfig = new MockServletConfig(servletContext, "loggableServlet");
        servlet.init(servletConfig);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION})
    void testLogging() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpServletRequest request = createRequest();

        servlet.service(request, response);

        doAssert(response, request);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION})
    void testMasking() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        var request = createRequest();
        ObjectNode nodeWithToken = new ObjectNode(JsonNodeFactory.instance)
            .put("token", "haxx0rpr00ft0k3n")
            .put("login", "unmaskedLogin");
        ObjectNode nodeWithMaskedToken = new ObjectNode(JsonNodeFactory.instance)
            .put("token", "hax**")
            .put("login", "unmaskedLogin");

        request.setContent(objectMapper.writeValueAsBytes(nodeWithToken));
        request.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        request.setCharacterEncoding("utf-8");
        request.setMethod("POST");
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);

        servlet.service(wrapper, response);
        List<UserActivityLog> records = logRepository.findAll();
        softly.assertThat(records)
            .extracting(UserActivityLog::getRequestBody)
            .as("sensitive data is hidden")
            .containsExactly(nodeWithMaskedToken);
    }

    private void doAssert(MockHttpServletResponse response, HttpServletRequest request) {
        List<UserActivityLog> records = logRepository.findAll();

        softly.assertThat(records)
            .as("there's got to be exactly one log record")
            .hasSize(1);

        softly.assertThat(records)
            .extracting(UserActivityLog::getLogin)
            .as("login has to be equal")
            .contains("lmsUser");

        softly.assertThat(records)
            .extracting(UserActivityLog::getRemoteAddress)
            .as("remote-addr has to be equal")
            .contains("0:0:0:0:0:0:0:1");

        softly.assertThat(records)
            .extracting(UserActivityLog::getResponseStatus)
            .as("response status has to be equal")
            .contains(response.getStatus());

        softly.assertThat(records)
            .extracting(UserActivityLog::getProject)
            .as("project has to be equal")
            .contains("lms");

        softly.assertThat(records)
            .extracting(UserActivityLog::getRequestMethod)
            .as("method has to be equal")
            .contains(request.getMethod());

        softly.assertThat(records)
            .extracting(UserActivityLog::getRequestParams)
            .extracting(JsonNode::toString)
            .as("request params has to be equal")
            .contains("{\"page\":\"[0]\",\"size\":\"[10]\"}");

        softly.assertThat(records)
            .extracting(UserActivityLog::getRequestBody)
            .extracting(JsonNode::toString)
            .as("body has to be empty")
            .contains("{}");

        softly.assertThat(records)
            .extracting(UserActivityLog::getRequestUri)
            .as("request uri has to be equal")
            .contains("/partner-relation");
    }

    private MockHttpServletRequest createRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPathInfo("/admin/lms/partner-relation");
        request.setRequestURI("/admin/lms/partner-relation");
        request.setRemoteAddr("0:0:0:0:0:0:0:1");
        request.setParameter("page", "0");
        request.setParameter("size", "10");
        request.setMethod("GET");

        return request;
    }
}

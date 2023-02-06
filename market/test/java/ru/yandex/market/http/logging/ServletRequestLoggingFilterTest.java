package ru.yandex.market.http.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@DisplayName("Тесты на логирование")
public class ServletRequestLoggingFilterTest extends AbstractTest {

    private static final Pattern UUID_AND_TS_LOG_PATTERN =
        Pattern.compile(
            ".*uuid: [0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.[0-9]{13}.*",
            Pattern.DOTALL
        );

    private final List<String> logs = new ArrayList<>();

    private ServletRequestLoggingFilter servletRequestLoggingFilter;

    private MockFilterChain filterChain;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    public void initMock() {
        servletRequestLoggingFilter = new ServletRequestLoggingFilter();
        filterChain = new MockFilterChain();
        initAppender();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    public void cleanup() {
        logs.clear();
        detachAppender();
    }

    @Test
    @DisplayName("Логгер вызывается")
    public void logMessages() throws Exception {
        servletRequestLoggingFilter.doFilterInternal(request, response, filterChain);

        softly.assertThat(logs)
            .anyMatch(s -> s.contains("#http_server_request"));
        softly.assertThat(logs)
            .anyMatch(s -> s.contains("#http_server_response"));
    }

    @Test
    @DisplayName("Идентификатор из заголовка требуется и передан")
    public void staticIdRequestedAndFound() throws Exception {
        HttpServletRequest request = MockMvcRequestBuilders.post("https://localhost/test")
            .header("X-Market-Req-Id", "xxxx-request-id")
            .buildRequest(new MockServletContext());
        LoggingSettings settings = new LoggingSettings(true, false);
        servletRequestLoggingFilter.setLoggingSettings(settings);
        servletRequestLoggingFilter.doFilterInternal(request, response, filterChain);

        softly.assertThat(logs)
            .isNotEmpty()
            .allMatch(s -> s.contains("xxxx-request-id"));
    }

    @Test
    @DisplayName("Идентификатор из заголовка требуется и не передан")
    public void staticIdRequestedAndNotFound() throws Exception {
        HttpServletRequest request = MockMvcRequestBuilders.post("https://localhost/test")
            .content("{\"body\":\"request\"}")
            .contentType(MediaType.APPLICATION_JSON)
            .buildRequest(new MockServletContext());
        LoggingSettings settings = new LoggingSettings(true, false);
        servletRequestLoggingFilter.setLoggingSettings(settings);
        servletRequestLoggingFilter.doFilterInternal(request, response, filterChain);

        softly.assertThat(logs)
            .isNotEmpty()
            .allMatch(UUID_AND_TS_LOG_PATTERN.asPredicate());
    }

    private void initAppender() {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration configuration = context.getConfiguration();
        AbstractStringLayout.Serializer consoleEventSerializer =
            ((PatternLayout) configuration.getAppender("CONSOLE").getLayout()).getEventSerializer();

        Appender spyAppender = Mockito.spy(NullAppender.createAppender("NULL"));
        spyAppender.start();
        configuration.getLoggerConfig(ServletRequestLoggingFilter.class.getCanonicalName())
            .addAppender(spyAppender, Level.INFO, null);
        context.updateLoggers();

        Mockito.doAnswer(invocation -> {
            String formattedMessage = consoleEventSerializer.toSerializable(invocation.getArgument(0));
            logs.add(formattedMessage);
            return null; // Still does nothing
        })
            .when(spyAppender).append(Mockito.any(LogEvent.class));
    }

    private void detachAppender() {
        LoggerContext context = LoggerContext.getContext(false);
        LoggerConfig loggerConfig = context.getConfiguration()
            .getLoggerConfig(ServletRequestLoggingFilter.class.getCanonicalName());
        loggerConfig.getAppenders().get("NULL").stop();
        loggerConfig.removeAppender("NULL");
        context.updateLoggers();
    }
}

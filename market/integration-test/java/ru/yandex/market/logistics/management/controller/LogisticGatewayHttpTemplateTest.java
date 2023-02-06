package ru.yandex.market.logistics.management.controller;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.gateway.client.utils.TvmHttpTemplate;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.lgw.LogisticGatewayHttpTemplate;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class LogisticGatewayHttpTemplateTest extends AbstractContextualTest {

    private static final String SERVICE_TICKET = "service_ticket";
    private static final String USER_TICKET = "user_ticket";

    @Value("${lgw.api.host}")
    private String uri;

    @Autowired
    private LogisticGatewayHttpTemplate httpTemplate;

    @Autowired
    private RestTemplate lgwApiRestTemplate;

    @Autowired
    private TvmTicketProvider ticketProvider;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setup() {
        when(ticketProvider.provideServiceTicket()).thenReturn(SERVICE_TICKET);
        when(ticketProvider.provideUserTicket()).thenReturn(USER_TICKET);
        mockServer = MockRestServiceServer.createServer(lgwApiRestTemplate);
    }

    @Test
    void testHeadersExistsExecutePost() {
        initMockServerExpectations(HttpMethod.POST);
        httpTemplate.executePost(mock(Object.class));
    }

    @Test
    void testHeadersExistsExecutePostWithResponseClass() {
        initMockServerExpectations(HttpMethod.POST);
        httpTemplate.executePost(mock(Object.class), Object.class);
    }

    @Test
    void testHeadersExistsExecuteGet() {
        initMockServerExpectations(HttpMethod.GET);
        httpTemplate.executeGet(Object.class, Collections.emptyMap());
    }

    @Test
    void testHeadersExistsExecute() {
        initMockServerExpectations(HttpMethod.GET);
        httpTemplate.execute(HttpMethod.GET, Object.class, Collections.emptyMap());
    }

    @Test
    void testHeadersExistsExecuteWithRequest() {
        initMockServerExpectations(HttpMethod.GET);
        httpTemplate.execute(mock(Object.class), HttpMethod.GET, Object.class, Collections.emptyMap());
    }

    @AfterEach
    void afterEachTest() {
        mockServer.verify();
    }

    private void initMockServerExpectations(HttpMethod httpMethod) {
        mockServer
            .expect(requestTo(uri))
            .andExpect(method(httpMethod))
            .andExpect(header(TvmHttpTemplate.SERVICE_TICKET_HEADER, SERVICE_TICKET))
            .andExpect(header(TvmHttpTemplate.USER_TICKET_HEADER, USER_TICKET))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{}"));
    }
}

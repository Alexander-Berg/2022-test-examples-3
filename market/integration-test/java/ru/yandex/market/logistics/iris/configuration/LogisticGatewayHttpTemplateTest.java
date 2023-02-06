package ru.yandex.market.logistics.iris.configuration;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.gateway.client.utils.TvmHttpTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.iris.configuration.TvmMockConfiguration.SERVICE_TICKET;
import static ru.yandex.market.logistics.iris.configuration.TvmMockConfiguration.USER_TICKET;

@Import({LGWExchangeConfiguration.class})
public class LogisticGatewayHttpTemplateTest extends AbstractContextualTest {

    @Value("${lgw.api.host}")
    private String uri;

    @Autowired
    private TvmHttpTemplate httpTemplate;

    @Autowired
    private RestTemplate lgwRestTemplate;

    private MockRestServiceServer mockServer;

    private String request = "{}";

    @Before
    public void setUp() {
        mockServer = createMockRestServiceServer(lgwRestTemplate);
        mockServer
            .expect(requestTo(uri))
            .andExpect(header(TvmHttpTemplate.SERVICE_TICKET_HEADER, SERVICE_TICKET))
            .andExpect(header(TvmHttpTemplate.USER_TICKET_HEADER, USER_TICKET))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(request));
    }

    @After
    public void after() {
        mockServer.verify();
    }

    @Test
    public void testHeadersExistsExecutePost() {
        httpTemplate.executePost(request);
    }

    @Test
    public void testHeadersExistsExecutePostWithResponseClass() {
        httpTemplate.executePost(request, Object.class);
    }

    @Test
    public void testHeadersExistsExecuteGet() {
        httpTemplate.executeGet(Object.class, Collections.emptyMap());
    }
}

package ru.yandex.market.delivery.tracker;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerResponse;
import ru.yandex.market.delivery.tracker.configuration.LgwRestTemplateMockConfiguration;
import ru.yandex.market.delivery.tracker.configuration.LgwSqsMockConfiguration;
import ru.yandex.market.delivery.tracker.configuration.TvmMockConfiguration;
import ru.yandex.market.delivery.tracker.configuration.lgw.LgwClientsConfiguration;
import ru.yandex.market.delivery.tracker.configuration.lgw.LgwTvmHttpTemplateConfiguration;
import ru.yandex.market.logistic.gateway.client.utils.TvmHttpTemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.delivery.tracker.ParsingUtils.extractFileContent;

@ExtendWith(SpringExtension.class)
@Import({
    LgwClientsConfiguration.class,
    LgwTvmHttpTemplateConfiguration.class,
    TvmMockConfiguration.class,
    LgwRestTemplateMockConfiguration.class,
    LgwSqsMockConfiguration.class
})
@PropertySource("classpath:application-integration-test.properties")
@SuppressWarnings({"WeakerAccess", "SpringJavaAutowiredMembersInspection"})
public abstract class AbstractLgwParsingTest {

    @Autowired
    protected LgwClientsConfiguration configuration;

    @Autowired
    protected RestTemplate lgwRestTemplate;

    @Value("${lgw.api.host}")
    protected String lgwApiHost;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setup() throws IllegalAccessError {
        mockServer = MockRestServiceServer.createServer(lgwRestTemplate);
        ParsingUtils.decorateWithBuffering(lgwRestTemplate);
    }

    protected void setupMockServerExpectation(String url, String fileName) throws IOException {
        mockServer.expect(requestTo(lgwApiHost + url))
            .andExpect(content().json(extractFileContent("request/" + fileName)))
            .andExpect(header(TvmHttpTemplate.SERVICE_TICKET_HEADER, TvmMockConfiguration.SERVICE_TICKET))
            .andRespond(
                withSuccess()
                    .body(extractFileContent("response/" + fileName))
                    .contentType(MediaType.APPLICATION_JSON)
            );
    }

    protected <T> void assertResponseWithExpectedPayload(TrackerResponse<T> response, T expectedResponsePayload) {
        assertThat("Response was null", response, is(notNullValue()));
        assertThat("Payload was different", response.getPayload(), is(expectedResponsePayload));
    }
}

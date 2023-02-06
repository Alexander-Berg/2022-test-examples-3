package ru.yandex.market.pers.feedback.config;

import java.util.List;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.HttpStatus;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;
import ru.yandex.passport.tvmauth.TvmClient;

public class CheckouterClientConfigTest extends AbstractPersFeedbackTest {
    @Autowired
    private CheckouterAPI checkouterAPI;
    @Autowired
    private Stubbing checkouterMock;
    @Autowired
    private TvmClient tvmClient;

    @Test
    public void shouldSendTvmHeader() {
        checkouterMock.stubFor(
                WireMock.get("/ping").willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withBody("0;OK")
                        .withStatus(200)
                )
        );
        checkouterAPI.ping();

        List<ServeEvent> events = checkouterMock.getAllServeEvents();
        ServeEvent event = events.get(0);
        HttpHeaders headers = event.getRequest().getHeaders();

        HttpHeader header = headers.getHeader(CheckoutHttpParameters.SERVICE_TICKET_HEADER);
        MatcherAssert.assertThat(header, Matchers.notNullValue());
        MatcherAssert.assertThat(header.values(), Matchers.not(Matchers.empty()));
        MatcherAssert.assertThat(header.isSingleValued(), Matchers.is(true));
        MatcherAssert.assertThat(header.values().get(0), Matchers.is("aServiceTicket"));
    }
}
